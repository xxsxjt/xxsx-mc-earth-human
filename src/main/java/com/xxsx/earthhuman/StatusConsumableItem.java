package com.xxsx.earthhuman;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && !wouldHaveEffect(serverPlayer, stack)) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.earth_human.care.no_effect"));
            return InteractionResult.FAIL;
        }
        if (level.isClientSide() && isMedicalItem(stack.getItem()) && !clientNeedsMedicalItem(player, stack.getItem())) {
            player.sendOverlayMessage(Component.translatable("message.earth_human.care.no_effect"));
            return InteractionResult.FAIL;
        }
        player.swing(hand, true);
        ItemUtils.startUsingInstantly(level, player, hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            if (!wouldHaveEffect(player, stack)) {
                player.sendOverlayMessage(Component.translatable("message.earth_human.care.no_effect"));
                return stack;
            }
            applyStatusEffects(player, stack);
        }
        if (entity instanceof Player player) {
            player.swing(player.getUsedItemHand(), true);
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
        emitConsumableFeedback(player, stack.getItem());
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
        if (isMedicalItem(stack.getItem())) {
            return canApplyBodyTreatment(player, stack.getItem())
                    || player.getHealth() < player.getMaxHealth() - 0.05F;
        }
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

    private void emitConsumableFeedback(ServerPlayer player, Item item) {
        if (item == EarthHumanItems.ORTHOPEDIC_SPLINT.get()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.90F, 0.95F);
        } else if (item == EarthHumanItems.SIMPLE_BANDAGE.get() || item == EarthHumanItems.STERILE_BANDAGE.get()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.85F, 1.15F);
        } else if (item == EarthHumanItems.BASIC_FIRST_AID_KIT.get()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 0.90F, 1.10F);
        } else if (animation == ItemUseAnimation.DRINK) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.85F, 1.0F);
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WOOL_STEP, SoundSource.PLAYERS, 0.80F, 1.05F);
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.2D, player.getZ(),
                    isMedicalItem(item) ? 4 : 2, 0.30D, 0.25D, 0.30D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    5, 0.35D, 0.30D, 0.35D, 0.02D);
        }
    }

    private static boolean isMedicalItem(Item item) {
        return item == EarthHumanItems.SIMPLE_BANDAGE.get()
                || item == EarthHumanItems.STERILE_BANDAGE.get()
                || item == EarthHumanItems.ORTHOPEDIC_SPLINT.get()
                || item == EarthHumanItems.BASIC_FIRST_AID_KIT.get();
    }

    private static boolean clientNeedsMedicalItem(Player player, Item item) {
        if (player.getHealth() < player.getMaxHealth() - 0.05F) {
            return true;
        }
        HumanBodySyncPayload body = com.xxsx.earthhuman.client.EarthHumanClient.currentBodyPayload();
        if (item == EarthHumanItems.ORTHOPEDIC_SPLINT.get()) {
            return body.legInjury() > 0.15D || body.rightHandInjury() > 0.15D || body.limbs() < 0.99D;
        }
        boolean includeHead = item == EarthHumanItems.BASIC_FIRST_AID_KIT.get();
        if (includeHead && body.head() < 0.99D) {
            return true;
        }
        return body.torso() < 0.99D
                || body.limbs() < 0.99D
                || body.legInjury() > 0.01D
                || body.rightHandInjury() > 0.01D;
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
        double fractureRelief;
        if (item == EarthHumanItems.SIMPLE_BANDAGE.get()) {
            heal = 1.5D;
            includeHead = false;
            fractureRelief = 0.25D;
        } else if (item == EarthHumanItems.STERILE_BANDAGE.get()) {
            heal = 2.5D;
            includeHead = false;
            fractureRelief = 0.38D;
        } else if (item == EarthHumanItems.ORTHOPEDIC_SPLINT.get()) {
            heal = 3.0D;
            includeHead = false;
            fractureRelief = 0.68D;
        } else if (item == EarthHumanItems.BASIC_FIRST_AID_KIT.get()) {
            heal = 5.0D;
            includeHead = true;
            fractureRelief = 0.78D;
        } else {
            return null;
        }

        HumanBodyStatus body = HumanBodyStatus.get(player);
        if (body.hasFracture() || item == EarthHumanItems.ORTHOPEDIC_SPLINT.get()) {
            if (body.treatFracture(player, fractureRelief, heal * EarthHumanConfig.healingMultiplier())) {
                body.save(player);
                HumanNetwork.syncBody(player, body);
                return Component.translatable(
                        "message.earth_human.body.injury.treated",
                        String.format(java.util.Locale.ROOT, "%.0f", (1.0D - Math.max(body.legInjury(), body.rightHandInjury())) * 100.0D));
            }
        }
        BodyPart part = body.worstPart(includeHead);
        if (body.hasSpecificInjury() && body.health(part) >= body.maxHealth(part) - 0.01D) {
            boolean recovered = body.recoverSpecificInjuries(Math.max(fractureRelief, heal / 4.0D));
            if (recovered) {
                body.applyFunctionalEffects(player);
                body.save(player);
                HumanNetwork.syncBody(player, body);
                return Component.translatable(
                        "message.earth_human.body.injury.treated",
                        String.format(java.util.Locale.ROOT, "%.0f", (1.0D - Math.max(body.legInjury(), body.rightHandInjury())) * 100.0D));
            }
        }
        if (body.health(part) >= body.maxHealth(part) - 0.01D) {
            return null;
        }
        if (!body.healPart(player, part, heal * EarthHumanConfig.healingMultiplier())) {
            return null;
        }
        body.applyFunctionalEffects(player);
        body.save(player);
        HumanNetwork.syncBody(player, body);
        return Component.translatable(
                "message.earth_human.body.treated",
                Component.translatable(part.translationKey()),
                String.format(java.util.Locale.ROOT, "%.1f", body.health(part)));
    }

    private static boolean canApplyBodyTreatment(ServerPlayer player, Item item) {
        boolean includeHead;
        if (item == EarthHumanItems.SIMPLE_BANDAGE.get()
                || item == EarthHumanItems.STERILE_BANDAGE.get()
                || item == EarthHumanItems.ORTHOPEDIC_SPLINT.get()) {
            includeHead = false;
        } else if (item == EarthHumanItems.BASIC_FIRST_AID_KIT.get()) {
            includeHead = true;
        } else {
            return false;
        }
        HumanBodyStatus body = HumanBodyStatus.get(player);
        BodyPart part = body.worstPart(includeHead);
        return body.health(part) < body.maxHealth(part) - 0.01D || body.hasSpecificInjury();
    }

    public record EffectSpec(Holder<MobEffect> effect, int durationTicks, int amplifier) {
        public MobEffectInstance create() {
            return new MobEffectInstance(effect, durationTicks, amplifier, true, false, true);
        }
    }
}
