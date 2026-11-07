package com.xxsx.earthhuman;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Hand-held mask that can be equipped into the head slot without an armor dependency. */
public final class PpeMaskItem extends GuidedItem {
    private static final int EQUIP_TICKS = 12;

    private final double filterStrength;
    private final String protectionKey;

    public PpeMaskItem(Item.Properties properties, String tooltipKey, double filterStrength, String protectionKey) {
        super(properties, tooltipKey);
        this.filterStrength = HumanStatus.clamp(filterStrength, 0.0D, 0.95D);
        this.protectionKey = protectionKey;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.earth_human.mask.head_occupied"));
            }
            return InteractionResult.FAIL;
        }
        player.swing(hand, true);
        ItemUtils.startUsingInstantly(level, player, hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            player.setItemSlot(EquipmentSlot.HEAD, stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.sendOverlayMessage(Component.translatable("message.earth_human.mask.equipped"));
        }
        return stack;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return EQUIP_TICKS;
    }

    @Override
    public void appendEarthHumanTooltip(Consumer<Component> tooltip) {
        super.appendEarthHumanTooltip(tooltip);
        tooltip.accept(Component.translatable(protectionKey,
                Math.round(filterStrength * 100.0D)).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.earth_human.mask.equip_hint").withStyle(ChatFormatting.GRAY));
    }

    public static double filterStrength(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof PpeMaskItem mask) {
            return mask.filterStrength;
        }
        if (head.is(HumanCareTags.DUST_MASKS)) {
            return 0.82D;
        }
        if (head.is(HumanCareTags.MASKS)) {
            return 0.45D;
        }
        return 0.0D;
    }

    public static boolean isWearingMask(Player player) {
        return filterStrength(player) > 0.0D;
    }
}
