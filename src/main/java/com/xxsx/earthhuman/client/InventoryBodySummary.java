package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

/** Keeps the normal body HUD visible and clickable while the survival inventory is open. */
final class InventoryBodySummary {
    private InventoryBodySummary() {
    }

    static void render(ScreenEvent.Render.Foreground event, HumanBodySyncPayload body) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }

        // Posted after the container's leftPos/topPos translation is popped: draw in absolute GUI coordinates.
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        // Unified [✦ 修行] Hub button at the top-right of the survival inventory
        int btnX = cultivationButtonX(event.getScreen());
        int btnY = cultivationButtonY(event.getScreen());
        int btnW = 54;
        int btnH = 16;
        boolean hoverHub = event.getMouseX() >= btnX && event.getMouseX() < btnX + btnW
                && event.getMouseY() >= btnY && event.getMouseY() < btnY + btnH;
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, hoverHub ? 0xE82A383B : 0xD8172023);
        graphics.outline(btnX, btnY, btnW, btnH, hoverHub ? 0xFFF2C96B : 0xFFB89242);
        Component hubLabel = Component.translatable("screen.earth_human.inventory.cultivation_button");
        graphics.centeredText(font, hubLabel, btnX + btnW / 2, btnY + 4, hoverHub ? 0xFFFFE599 : 0xFFF2D27A);
        if (hoverHub) {
            double factor = EarthHumanClient.syncedTraining().strengthFactor();
            graphics.setComponentTooltipForNextFrame(font, List.of(
                            Component.translatable("screen.earth_human.inventory.cultivation_tooltip.title"),
                            Component.translatable("screen.earth_human.inventory.cultivation_tooltip.somatics",
                                    String.format(java.util.Locale.ROOT, "%.2fx", factor)),
                            Component.translatable("screen.earth_human.inventory.cultivation_tooltip.hint")),
                    event.getMouseX(), event.getMouseY());
        }

        if (!HumanHudClientConfig.showBodyHud()) {
            return;
        }

        BodyHudRenderer.Placement placement = placement();
        BodyHudRenderer.draw(graphics, body, placement.x(), placement.y(),
                HumanHudClientConfig.bodyHudScale(), HumanHudClientConfig.bodyHudOpacity());
        if (placement.contains(event.getMouseX(), event.getMouseY())) {
            graphics.outline(placement.x() - 3, placement.y() - 3,
                    placement.width() + 6, placement.height() + 6, 0xFF7BB69F);
        }

        if (placement.contains(event.getMouseX(), event.getMouseY())) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                            Component.translatable("screen.earth_human.inventory.summary"),
                            partLine(body, BodyPart.HEAD),
                            partLine(body, BodyPart.TORSO),
                            partLine(body, BodyPart.LIMBS),
                            injuryLine("leg", body.legInjury()),
                            injuryLine("right_hand", body.rightHandInjury()),
                            Component.translatable("screen.earth_human.inventory.click_hint")),
                    event.getMouseX(), event.getMouseY());
        }
    }

    static boolean hitTest(Screen screen, double mouseX, double mouseY) {
        return screen instanceof InventoryScreen
                && HumanHudClientConfig.showBodyHud()
                && placement().contains(mouseX, mouseY);
    }

    static boolean hitTestCultivationHub(Screen screen, double mouseX, double mouseY) {
        if (!(screen instanceof InventoryScreen)) {
            return false;
        }
        int bx = cultivationButtonX(screen);
        int by = cultivationButtonY(screen);
        return mouseX >= bx && mouseX < bx + 54 && mouseY >= by && mouseY < by + 16;
    }

    private static int cultivationButtonX(Screen screen) {
        return (screen.width + 176) / 2 - 56;
    }

    private static int cultivationButtonY(Screen screen) {
        return Math.max(4, (screen.height - 166) / 2 - 19);
    }

    private static BodyHudRenderer.Placement placement() {
        var window = Minecraft.getInstance().getWindow();
        return BodyHudRenderer.placement(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    private static Component partLine(HumanBodySyncPayload body, BodyPart part) {
        double ratio = Math.max(0.0D, Math.min(1.0D, body.value(part)));
        return Component.translatable("screen.earth_human.inventory.part_line",
                Component.translatable(part.translationKey()), Math.round(ratio * 100.0D));
    }

    private static Component injuryLine(String id, double severity) {
        return Component.translatable("screen.earth_human.inventory.injury_line",
                Component.translatable("bodyinjury.earth_human." + id),
                Math.round(Math.max(0.0D, Math.min(1.0D, 1.0D - severity)) * 100.0D));
    }
}
