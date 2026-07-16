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

import java.util.function.Consumer;

public class ExerciseEquipmentItem extends Item implements EarthHumanTooltipProvider {
    private final String tooltipKey;
    private final ExerciseAction action;

    public ExerciseEquipmentItem(Properties properties, String tooltipKey, ExerciseAction action) {
        super(properties);
        this.tooltipKey = tooltipKey;
        this.action = action;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            HumanExercise.apply(player, action, true);
        }
        return stack;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 34;
    }

    @Override
    public void appendEarthHumanTooltip(Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.earth_human.exercise_action", action.displayName()).withStyle(ChatFormatting.AQUA));
    }
}
