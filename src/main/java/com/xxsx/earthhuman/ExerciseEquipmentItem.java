package com.xxsx.earthhuman;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Consumer;

public class ExerciseEquipmentItem extends Item implements EarthHumanTooltipProvider {
    private final String tooltipKey;
    private final ExerciseAction action;
    private final ItemUseAnimation animation;
    private final int durationTicks;

    public ExerciseEquipmentItem(Properties properties, String tooltipKey, ExerciseAction action) {
        super(properties);
        this.tooltipKey = tooltipKey;
        this.action = action;
        this.animation = animationFor(action);
        this.durationTicks = durationFor(action);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.playSound(startSound(), 0.55F, startPitch());
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            if (HumanExercise.apply(player, action, true)) {
                player.playSound(finishSound(), 0.65F, finishPitch());
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
        return durationTicks;
    }

    @Override
    public void appendEarthHumanTooltip(Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.earth_human.exercise_action", action.displayName()).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.earth_human.exercise_duration",
                String.format(java.util.Locale.ROOT, "%.1f", durationTicks / 20.0D)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static ItemUseAnimation animationFor(ExerciseAction action) {
        return switch (action) {
            case RESISTANCE_BAND_ROW -> ItemUseAnimation.BOW;
            case DUMBBELL_CURL -> ItemUseAnimation.SPEAR;
            case STRETCH -> ItemUseAnimation.BRUSH;
            default -> ItemUseAnimation.NONE;
        };
    }

    private static int durationFor(ExerciseAction action) {
        return switch (action) {
            case RESISTANCE_BAND_ROW -> 48;
            case DUMBBELL_CURL -> 42;
            case STRETCH -> 62;
            default -> 40;
        };
    }

    private SoundEvent startSound() {
        return switch (action) {
            case RESISTANCE_BAND_ROW -> SoundEvents.ARMOR_EQUIP_LEATHER.value();
            case DUMBBELL_CURL -> SoundEvents.CHAIN_PLACE;
            case STRETCH -> SoundEvents.WOOL_PLACE;
            default -> SoundEvents.PLAYER_ATTACK_STRONG;
        };
    }

    private SoundEvent finishSound() {
        return switch (action) {
            case RESISTANCE_BAND_ROW -> SoundEvents.ARMOR_EQUIP_LEATHER.value();
            case DUMBBELL_CURL -> SoundEvents.IRON_GOLEM_REPAIR;
            case STRETCH -> SoundEvents.WOOL_STEP;
            default -> SoundEvents.PLAYER_ATTACK_STRONG;
        };
    }

    private float startPitch() {
        return action == ExerciseAction.DUMBBELL_CURL ? 0.85F : 1.05F;
    }

    private float finishPitch() {
        return action == ExerciseAction.STRETCH ? 1.2F : 1.0F;
    }
}
