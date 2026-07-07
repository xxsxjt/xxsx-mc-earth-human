package com.xxsx.earthhuman;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class StatusConsumableItem extends Item {
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
            HumanStatus status = HumanStatus.get(player);
            status.hydration = HumanStatus.clamp(status.hydration + hydrationDelta, 0.0D, 100.0D);
            status.bodyTemperature = HumanStatus.clamp(status.bodyTemperature + temperatureDelta, 32.0D, 42.5D);
            status.fatigue = HumanStatus.clamp(status.fatigue + fatigueDelta, 0.0D, 100.0D);
            for (EffectSpec effect : effects) {
                player.addEffect(effect.create());
            }
            status.save(player);
            HumanNetwork.sync(player, status);
            player.sendSystemMessage(Component.translatable(
                    "message.earth_human.item_used",
                    stack.getHoverName(),
                    Math.round(status.hydration),
                    String.format(java.util.Locale.ROOT, "%.1f", status.bodyTemperature),
                    Math.round(status.fatigue)));
        }

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (remainder != null && entity instanceof Player player) {
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

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return animation;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return animation == ItemUseAnimation.DRINK ? 32 : 24;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
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

    public record EffectSpec(Holder<MobEffect> effect, int durationTicks, int amplifier) {
        public MobEffectInstance create() {
            return new MobEffectInstance(effect, durationTicks, amplifier, true, false, true);
        }
    }
}
