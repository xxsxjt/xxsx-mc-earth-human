package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.EarthHuman;
import com.xxsx.earthhuman.EarthHumanTooltipProvider;
import com.xxsx.earthhuman.CareAction;
import com.xxsx.earthhuman.HumanCareActionPayload;
import com.xxsx.earthhuman.HumanCareCancelPayload;
import com.xxsx.earthhuman.HumanCareStatePayload;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import com.xxsx.earthhuman.HumanActivityType;
import com.xxsx.earthhuman.HumanOpenSettingsPayload;
import com.xxsx.earthhuman.HumanOpenStatusPayload;
import com.xxsx.earthhuman.HumanOpenGuidePayload;
import com.xxsx.earthhuman.HumanStatus;
import com.xxsx.earthhuman.HumanStatusSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class EarthHumanClient {
    private static final int ROW_HEIGHT = 14;
    private static HumanStatus syncedStatus = new HumanStatus(100.0D, 37.0D, 0.0D, 0);
    private static HumanBodySyncPayload syncedBody = new HumanBodySyncPayload(
            1.0D, 1.0D, 1.0D, 0.0D, 0.0D);
    private static HumanCareStatePayload syncedCareState = HumanCareStatePayload.IDLE;
    private static int careAnimationTicker;

    private EarthHumanClient() {
    }

    public static void register(IEventBus modBus, ModContainer modContainer) {
        HumanHudClientConfig.load();
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new EarthHumanSettingsScreen(parent));
        modBus.addListener(EarthHumanClient::registerGuiLayers);
        modBus.addListener(EarthHumanClient::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(EarthHumanClient::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(EarthHumanClient::onContainerForeground);
        NeoForge.EVENT_BUS.addListener(EarthHumanClient::onScreenMousePressed);
        NeoForge.EVENT_BUS.addListener(EarthHumanClient::onClientTick);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, EarthHuman.id("body_status"), EarthHumanClient::renderBodyStatus);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(HumanStatusSyncPayload.TYPE, EarthHumanClient::handleStatusSync);
        event.register(HumanBodySyncPayload.TYPE, EarthHumanClient::handleBodySync);
        event.register(HumanOpenSettingsPayload.TYPE, EarthHumanClient::handleOpenSettings);
        event.register(HumanOpenStatusPayload.TYPE, EarthHumanClient::handleOpenStatus);
        event.register(HumanOpenGuidePayload.TYPE, EarthHumanClient::handleOpenGuide);
        event.register(HumanCareStatePayload.TYPE, EarthHumanClient::handleCareState);
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof EarthHumanTooltipProvider provider) {
            provider.appendEarthHumanTooltip(event.getToolTip()::add);
        }
    }

    private static void onContainerForeground(ScreenEvent.Render.Foreground event) {
        InventoryBodySummary.render(event, syncedBody);
    }

    private static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !InventoryBodySummary.hitTest(event.getScreen(), event.getMouseX(), event.getMouseY())) {
            return;
        }
        event.setCanceled(true);
        openStatusScreen(EarthHumanStatusScreen.TAB_BODY);
    }

    private static void handleStatusSync(HumanStatusSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> syncedStatus = new HumanStatus(
                payload.hydration(), payload.bodyTemperature(), payload.fatigue(), 0,
                payload.exertionLoad(), HumanActivityType.byNetworkId(payload.currentActivityId()), 0,
                payload.airQuality()));
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

    private static void handleOpenGuide(HumanOpenGuidePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new EarthHumanGuideScreen(null)));
    }

    private static void handleCareState(HumanCareStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> syncedCareState = payload);
    }

    /**
     * A care action is owned by the server and has no item use behind it, so the client supplies
     * the visible motion: one arm swing every ten ticks while the action runs.
     */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !syncedCareState.active()) {
            careAnimationTicker = 0;
            return;
        }
        if (player.isUsingItem()) {
            return;
        }
        if (++careAnimationTicker % 10 == 0) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static void renderBodyStatus(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null || minecraft.options.hideGui || !minecraft.gameMode.canHurtPlayer()) {
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
        int y = graphics.guiHeight() - minecraft.gui.rightHeight;

        int firstWidth = 42;
        int secondWidth = 48;
        int thirdWidth = 47;
        int fourthWidth = 45;
        boolean compact = graphics.guiWidth() - x < firstWidth + secondWidth + thirdWidth + fourthWidth + 14;
        int firstRowWidth = firstWidth + secondWidth + thirdWidth + 4;
        int totalWidth = compact ? firstRowWidth : firstRowWidth + fourthWidth + 2;
        graphics.fill(x - 2, y - 1, x + totalWidth + 2, y + (compact ? 26 : 12), 0xA8141A1C);
        graphics.outline(x - 2, y - 1, totalWidth + 2, compact ? 27 : 13, 0xAA52615D);
        drawCell(graphics, font, x, y + 1, firstWidth, status.hydration / 100.0D, hydrationColor(status.hydration),
                hudLabel("hud.earth_human.hydration.short", status.hydration, false));
        drawCell(graphics, font, x + firstWidth + 2, y + 1, secondWidth,
                (status.bodyTemperature - 32.0D) / 10.5D, temperatureColor(status.bodyTemperature),
                hudLabel("hud.earth_human.temperature.short", status.bodyTemperature, true));
        drawCell(graphics, font, x + firstWidth + secondWidth + 4, y + 1, thirdWidth,
                status.fatigue / 100.0D, fatigueColor(status.fatigue),
                hudLabel("hud.earth_human.fatigue.short", status.fatigue, false));
        int airX = compact ? x : x + firstWidth + secondWidth + thirdWidth + 6;
        int airY = compact ? y + 14 : y + 1;
        drawCell(graphics, font, airX, airY, fourthWidth,
                status.airQuality / 100.0D, airQualityColor(status.airQuality),
                hudLabel("hud.earth_human.air_quality.short", status.airQuality, false));

        minecraft.gui.rightHeight += compact ? ROW_HEIGHT * 2 : ROW_HEIGHT;
    }

    public static void openSettingsScreen() {
        openStatusScreen(3);
    }

    public static void openStatusScreen(int initialTab) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new EarthHumanStatusScreen(null, initialTab));
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
        BodyHudRenderer.Placement placement = BodyHudRenderer.placement(graphics.guiWidth(), graphics.guiHeight());
        BodyHudRenderer.draw(graphics, syncedBody, placement.x(), placement.y(), scale,
                HumanHudClientConfig.bodyHudOpacity());
    }

    private static void drawCell(GuiGraphicsExtractor graphics, Font font, int x, int y, int width,
                                 double ratio, int color, String label) {
        graphics.fill(x, y - 2, x + width, y + 11, 0xB9161C1D);
        graphics.outline(x, y - 2, width, 13, 0x884D5A57);
        graphics.text(font, label, x + 2, y - 1, 0xFFF0F2EC, false);
        graphics.fill(x + 2, y + 7, x + width - 2, y + 10, 0xFF27302F);
        double clamped = Math.max(0.0D, Math.min(1.0D, ratio));
        int fillWidth = Math.max(1, (int) Math.round((width - 4) * clamped));
        graphics.fill(x + 2, y + 7, x + 2 + fillWidth, y + 10, color | 0xFF000000);
        graphics.fill(x + 2 + fillWidth, y + 7, x + 3 + fillWidth, y + 10, 0xFFF0F2EC);
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

    private static int airQualityColor(double value) {
        if (value < 15.0D) return 0xFF5555;
        if (value < 35.0D) return 0xFFAA00;
        return HumanHudClientConfig.hudTheme().hydrationColor;
    }

    private static String hudLabel(String key, double value, boolean decimal) {
        String label = Component.translatable(key).getString();
        if (!HumanHudClientConfig.showNumericValues()) {
            return label;
        }
        String rendered = decimal
                ? String.format(java.util.Locale.ROOT, "%.1f", value)
                : Long.toString(Math.round(value));
        return label + " " + rendered;
    }
}
