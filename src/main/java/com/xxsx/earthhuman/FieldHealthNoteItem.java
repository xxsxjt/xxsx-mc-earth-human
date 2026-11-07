package com.xxsx.earthhuman;

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

public final class FieldHealthNoteItem extends GuidedItem {
    private static final int READ_TICKS = 10;

    public FieldHealthNoteItem(Item.Properties properties, String tooltipKey) {
        super(properties, tooltipKey);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.swing(hand, true);
        ItemUtils.startUsingInstantly(level, player, hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            HumanNetwork.openGuide(serverPlayer);
        }
        return stack;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return READ_TICKS;
    }
}
