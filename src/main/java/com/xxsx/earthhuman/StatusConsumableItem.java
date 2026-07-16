package com.xxsx.earthhuman;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class StatusConsumableItem extends Item implements EarthHumanTooltipProvider {
    private final String tooltipKey;
    private final ItemUseAnimation animation;
    private final double hydrationDelta;
    private final double temperatureDelta;
    private final double fatigueDelta;
    private final Supplier<? extends Item> remainder;
    private final EffectSpec[] effects;

    public StatusConsumableItem(
            Properties properties,
            String tooltipKey,
            ItemUseAnimation animation,
            double hydrationDelta,
            double temperatureDelta,
            double fatigueDelta,
            Supplier<? extends Item> remainder,
            EffectSpec... effects) {
        super(properties);
        this.tooltipKey = tooltipKey;
        this.animation = animation;
        this.hydrationDelta = hydrationDelta;
        this.temperatureDelta = temperatureDelta;
        this.fatigueDelta = fatigueDelta;
        this.remainder = remainder;
        this.effects = effects;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            applyStatusEffects(player, stack);
        }

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (remainder != null && entity instanceof Player player && !player.getAbilities().instabuild) {
            ItemStack result = new ItemStack(remainder.get());
            if (stack.isEmpty()) {
                return result;
            }
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
        return stack;
    }

    public void applyStatusEffects(ServerPlayer player, ItemStack stack) {
        HumanStatus status = HumanStatus.get(player);
        status.hydration = HumanStatus.clamp(status.hydration + hydrationDelta, 0.0D, 100.0D);
        status.bodyTemperature = HumanStatus.clamp(status.bodyTemperature + temperatureDelta, 32.0D, 42.5D);
        status.fatigue = HumanStatus.clamp(status.fatigue + fatigueDelta, 0.0D, 100.0D);
        for (EffectSpec effect : effects) {
            player.addEffect(effect.create());
        }
        Component bodyTreatment = applyBodyTreatment(player, stack.getItem());
        status.save(player);
        HumanNetwork.sync(player, status);
        player.sendSystemMessage(Component.translatable(
                "message.earth_human.item_used",
                stack.getHoverName(),
                Math.round(status.hydration),
                String.format(java.util.Locale.ROOT, "%.1f", status.bodyTemperature),
                Math.round(status.fatigue)));
        if (bodyTreatment != null) {
            player.sendSystemMessage(bodyTreatment);
        }
    }

    public boolean wouldHaveEffect(ServerPlayer player, ItemStack stack) {
        HumanStatus status = HumanStatus.get(player);
        if ((hydrationDelta > 0.0D && status.hydration < 100.0D)
                || (hydrationDelta < 0.0D && status.hydration > 0.0D)
                || (temperatureDelta > 0.0D && status.bodyTemperature < 42.5D)
                || (temperatureDelta < 0.0D && status.bodyTemperature > 32.0D)
                || (fatigueDelta > 0.0D && status.fatigue < 100.0D)
                || (fatigueDelta < 0.0D && status.fatigue > 0.0D)
                || canApplyBodyTreatment(player, stack.getItem())) {
            return true;
        }
        for (EffectSpec effect : effects) {
            if (effect.effect().equals(MobEffects.REGENERATION) && player.getHealth() >= player.getMaxHealth()) {
                continue;
            }
            MobEffectInstance current = player.getEffect(effect.effect());
            if (current == null
                    || current.getAmplifier() < effect.amplifier()
                    || current.getDuration() < effect.durationTicks()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack createRemainderStack() {
        return remainder == null ? ItemStack.EMPTY : new ItemStack(remainder.get());
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return animation;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return animation == ItemUseAnimation.DRINK ? 32 : 24;
    }

    @Override
    public void appendEarthHumanTooltip(Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(
                "tooltip.earth_human.status_delta",
                signed(hydrationDelta),
                signedTemperature(temperatureDelta),
                signed(fatigueDelta)).withStyle(ChatFormatting.AQUA));
    }

    private static String signed(double value) {
        return value > 0.0D
                ? "+" + Math.round(value)
                : String.valueOf(Math.round(value));
    }

    private static String signedTemperature(double value) {
        if (Math.abs(value) < 0.05D) {
            return "0.0";
        }
        String prefix = value > 0.0D ? "+" : "";
        return prefix + String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static Component applyBodyTreatment(ServerPlayer player, Item item) {
        double heal;
        boolean includeHead;
        if (item == EarthHumanItems.SIMPLE_BANDAGE.get()) {
            heal = 1.5D;
            includeHead = false;
        } else if (item == EarthHumanItems.STERILE_BANDAGE.get()) {
            heal = 2.5D;
            includeHead = false;
        } else if (item == EarthHumanItems.BASIC_FIRST_AID_KIT.get()) {
            heal = 5.0D;
            includeHead = true;
        } else {
            return null;
        }

        HumanBodyStatus body = HumanBodyStatus.get(player);
        BodyPart part = body.worstPart(includeHead);
        if (body.health(part) >= body.maxHealth(part) - 0.01D) {
            return null;
        }
        if (!body.healPart(player, part, heal * EarthHumanConfig.healingMultiplier())) {
            return null;
        }
        body.save(player);
        HumanNetwork.syncBody(player, body);
        return Component.translatable(
                "message.earth_human.body.treated",
                Component.translatable(part.translationKey()),
                String.format(java.util.Locale.ROOT, "%.1f", body.health(part)));
    }

    private static boolean canApplyBodyTreatment(ServerPlayer player, Item item) {
        boolean includeHead;
        if (item == EarthHumanItems.SIMPLE_BANDAGE.get() || item == EarthHumanItems.STERILE_BANDAGE.get()) {
            includeHead = false;
        } else if (item == EarthHumanItems.BASIC_FIRST_AID_KIT.get()) {
            includeHead = true;
        } else {
            return false;
        }
        HumanBodyStatus body = HumanBodyStatus.get(player);
        BodyPart part = body.worstPart(includeHead);
        return body.health(part) < body.maxHealth(part) - 0.01D;
    }

    public record EffectSpec(Holder<MobEffect> effect, int durationTicks, int amplifier) {
        public MobEffectInstance create() {
            return new MobEffectInstance(effect, durationTicks, amplifier, true, false, true);
        }
    }
}
