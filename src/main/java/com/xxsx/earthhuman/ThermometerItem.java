package com.xxsx.earthhuman;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.Locale;

public final class ThermometerItem extends GuidedItem {
    public ThermometerItem(Item.Properties properties, String tooltipKey) {
        super(properties, tooltipKey);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            HumanStatus status = HumanStatus.get(serverPlayer);
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "message.earth_human.thermometer.reading",
                    String.format(Locale.ROOT, "%.1f", status.bodyTemperature),
                    Component.translatable(temperatureStateKey(status.bodyTemperature))));
        }
        return InteractionResult.SUCCESS;
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
