package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.EarthHuman;
import com.xxsx.earthhuman.HumanStatus;
import com.xxsx.earthhuman.HumanStatusSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public final class EarthHumanClient {
    private static final int ROW_HEIGHT = 10;
    private static HumanStatus syncedStatus = new HumanStatus(100.0D, 37.0D, 0.0D, 0);

    private EarthHumanClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EarthHumanClient::registerGuiLayers);
        modBus.addListener(EarthHumanClient::registerPayloadHandlers);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, EarthHuman.id("body_status"), EarthHumanClient::renderBodyStatus);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(HumanStatusSyncPayload.TYPE, EarthHumanClient::handleStatusSync);
    }

    private static void handleStatusSync(HumanStatusSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> syncedStatus = new HumanStatus(
                payload.hydration(), payload.bodyTemperature(), payload.fatigue(), 0));
    }

    private static void renderBodyStatus(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null || minecraft.gui.hud.isHidden() || !minecraft.gameMode.canHurtPlayer()) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        HumanStatus status = syncedStatus;
        Font font = minecraft.font;
        int x = graphics.guiWidth() / 2 + 10;
        int y = graphics.guiHeight() - minecraft.gui.hud.rightHeight;

        graphics.fill(x - 1, y, x + 82, y + 9, 0x78000000);
        drawCell(graphics, font, x, y + 1, 25, hydrationColor(status.hydration),
                Component.translatable("hud.earth_human.hydration.short").getString() + Math.round(status.hydration));
        drawCell(graphics, font, x + 27, y + 1, 25, temperatureColor(status.bodyTemperature),
                Component.translatable("hud.earth_human.temperature.short").getString() + Math.round(status.bodyTemperature));
        drawCell(graphics, font, x + 54, y + 1, 27, fatigueColor(status.fatigue),
                Component.translatable("hud.earth_human.fatigue.short").getString() + Math.round(status.fatigue));

        minecraft.gui.hud.rightHeight += ROW_HEIGHT;
    }

    private static void drawCell(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int color, String label) {
        graphics.fill(x, y + 7, x + width, y + 8, color | 0xFF000000);
        graphics.text(font, label, x + 1, y - 1, color | 0xFF000000, true);
    }

    private static int hydrationColor(double value) {
        if (value < 20.0D) return 0xFF5555;
        if (value < 45.0D) return 0xFFAA00;
        return 0x55FFFF;
    }

    private static int temperatureColor(double value) {
        if (value < 35.5D) return 0x55AAFF;
        if (value > 38.2D) return 0xFFAA00;
        return 0xFFFFFF;
    }

    private static int fatigueColor(double value) {
        if (value > 80.0D) return 0xFF5555;
        if (value > 55.0D) return 0xFFAA00;
        return 0xFFAAFF;
    }
}
