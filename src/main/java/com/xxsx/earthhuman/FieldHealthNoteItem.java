package com.xxsx.earthhuman;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class FieldHealthNoteItem extends GuidedItem {
    public FieldHealthNoteItem(Item.Properties properties, String tooltipKey) {
        super(properties, tooltipKey);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            HumanNetwork.openStatus(serverPlayer, 0);
        }
        return InteractionResult.SUCCESS;
    }
}
