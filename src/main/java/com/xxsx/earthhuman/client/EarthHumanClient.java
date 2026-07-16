package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.EarthHuman;
import com.xxsx.earthhuman.EarthHumanTooltipProvider;
import com.xxsx.earthhuman.CareAction;
import com.xxsx.earthhuman.HumanCareActionPayload;
import com.xxsx.earthhuman.HumanCareCancelPayload;
import com.xxsx.earthhuman.HumanCareStatePayload;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import com.xxsx.earthhuman.HumanOpenSettingsPayload;
import com.xxsx.earthhuman.HumanOpenStatusPayload;
import com.xxsx.earthhuman.HumanStatus;
import com.xxsx.earthhuman.HumanStatusSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class EarthHumanClient {
    private static final int ROW_HEIGHT = 10;
    private static HumanStatus syncedStatus = new HumanStatus(100.0D, 37.0D, 0.0D, 0);
    private static HumanBodySyncPayload syncedBody = new HumanBodySyncPayload(
            1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D);
    private static HumanCareStatePayload syncedCareState = HumanCareStatePayload.IDLE;

    private EarthHumanClient() {
    }

    public static void register(IEventBus modBus, ModContainer modContainer) {
        HumanHudClientConfig.load();
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new EarthHumanSettingsScreen(parent));
        modBus.addListener(EarthHumanClient::registerGuiLayers);
        modBus.addListener(EarthHumanClient::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(EarthHumanClient::onItemTooltip);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, EarthHuman.id("body_status"), EarthHumanClient::renderBodyStatus);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(HumanStatusSyncPayload.TYPE, EarthHumanClient::handleStatusSync);
        event.register(HumanBodySyncPayload.TYPE, EarthHumanClient::handleBodySync);
        event.register(HumanOpenSettingsPayload.TYPE, EarthHumanClient::handleOpenSettings);
        event.register(HumanOpenStatusPayload.TYPE, EarthHumanClient::handleOpenStatus);
        event.register(HumanCareStatePayload.TYPE, EarthHumanClient::handleCareState);
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof EarthHumanTooltipProvider provider) {
            provider.appendEarthHumanTooltip(event.getToolTip()::add);
        }
    }

    private static void handleStatusSync(HumanStatusSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> syncedStatus = new HumanStatus(
                payload.hydration(), payload.bodyTemperature(), payload.fatigue(), 0));
    }

    private static void handleBodySync(HumanBodySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> syncedBody = payload);
    }

    private static void handleOpenSettings(HumanOpenSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openStatusScreen(3));
    }

    private static void handleOpenStatus(HumanOpenStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openStatusScreen(payload.initialTab()));
    }

    private static void handleCareState(HumanCareStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> syncedCareState = payload);
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
        renderHumanHud(graphics);
        if (!HumanHudClientConfig.showVitalsHud()) {
            return;
        }

        int x = graphics.guiWidth() / 2 + 10;
        int y = graphics.guiHeight() - minecraft.gui.hud.rightHeight;

        graphics.fill(x - 1, y, x + 82, y + 9, 0x78000000);
        drawCell(graphics, font, x, y + 1, 25, hydrationColor(status.hydration),
                hudLabel("hud.earth_human.hydration.short", status.hydration));
        drawCell(graphics, font, x + 27, y + 1, 25, temperatureColor(status.bodyTemperature),
                hudLabel("hud.earth_human.temperature.short", status.bodyTemperature));
        drawCell(graphics, font, x + 54, y + 1, 27, fatigueColor(status.fatigue),
                hudLabel("hud.earth_human.fatigue.short", status.fatigue));

        minecraft.gui.hud.rightHeight += ROW_HEIGHT;
    }

    public static void openSettingsScreen() {
        openStatusScreen(3);
    }

    public static void openStatusScreen(int initialTab) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setScreen(new EarthHumanStatusScreen(null, initialTab));
    }

    static HumanStatus syncedStatus() {
        return syncedStatus;
    }

    static HumanBodySyncPayload syncedBody() {
        return syncedBody;
    }

    static HumanCareStatePayload syncedCareState() {
        return syncedCareState;
    }

    static void requestCareAction(CareAction action) {
        ClientPacketDistributor.sendToServer(new HumanCareActionPayload(action.id));
    }

    static void cancelCareAction() {
        ClientPacketDistributor.sendToServer(HumanCareCancelPayload.INSTANCE);
    }

    private static void renderHumanHud(GuiGraphicsExtractor graphics) {
        if (!HumanHudClientConfig.showBodyHud()) {
            return;
        }
        double scale = HumanHudClientConfig.bodyHudScale();
        int w = BodyHudRenderer.width(scale);
        int h = BodyHudRenderer.height(scale);
        int margin = 6;
        int x = switch (HumanHudClientConfig.bodyHudCorner()) {
            case BOTTOM_LEFT, TOP_LEFT -> margin;
            case BOTTOM_RIGHT, TOP_RIGHT -> graphics.guiWidth() - w - margin;
        };
        int y = switch (HumanHudClientConfig.bodyHudCorner()) {
            case TOP_LEFT, TOP_RIGHT -> margin;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> graphics.guiHeight() - h - margin;
        };
        BodyHudRenderer.draw(graphics, syncedBody, x, y, scale, HumanHudClientConfig.bodyHudOpacity());
    }

    private static void drawCell(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int color, String label) {
        graphics.fill(x, y + 7, x + width, y + 8, color | 0xFF000000);
        graphics.text(font, label, x + 1, y - 1, color | 0xFF000000, true);
    }

    private static int hydrationColor(double value) {
        if (value < 20.0D) return 0xFF5555;
        if (value < 45.0D) return 0xFFAA00;
        return HumanHudClientConfig.hudTheme().hydrationColor;
    }

    private static int temperatureColor(double value) {
        if (value < 35.5D) return 0x55AAFF;
        if (value > 38.2D) return 0xFFAA00;
        return HumanHudClientConfig.hudTheme().temperatureColor;
    }

    private static int fatigueColor(double value) {
        if (value > 80.0D) return 0xFF5555;
        if (value > 55.0D) return 0xFFAA00;
        return HumanHudClientConfig.hudTheme().fatigueColor;
    }

    private static String hudLabel(String key, double value) {
        String label = Component.translatable(key).getString();
        return HumanHudClientConfig.showNumericValues() ? label + Math.round(value) : label;
    }
}
