package com.xxsx.earthhuman;

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

import java.util.Locale;

/** Hold it up and read it, instead of printing the value the instant it is clicked. */
public final class ThermometerItem extends GuidedItem {
    private static final int READ_TICKS = 20;

    public ThermometerItem(Item.Properties properties, String tooltipKey) {
        super(properties, tooltipKey);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            HumanStatus status = HumanStatus.get(serverPlayer);
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "message.earth_human.thermometer.reading",
                    String.format(Locale.ROOT, "%.1f", status.bodyTemperature),
                    Component.translatable(temperatureStateKey(status.bodyTemperature))));
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

    private static String temperatureStateKey(double temperature) {
        if (temperature < 35.5D) {
            return "message.earth_human.state.temperature.cold";
        }
        if (temperature > 38.2D) {
            return "message.earth_human.state.temperature.hot";
        }
        return "message.earth_human.state.ok";
    }
}
