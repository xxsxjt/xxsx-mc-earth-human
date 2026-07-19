package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

import java.util.List;

/** Keeps the normal body HUD visible and clickable while the survival inventory is open. */
final class InventoryBodySummary {
    private InventoryBodySummary() {
    }

    @SuppressWarnings("removal")
    static void render(ContainerScreenEvent.Render.Foreground event, HumanBodySyncPayload body) {
        if (!(event.getContainerScreen() instanceof InventoryScreen screen)
                || !HumanHudClientConfig.showBodyHud()) {
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        BodyHudRenderer.Placement placement = placement();
        graphics.pose().pushMatrix();
        graphics.pose().translate(-screen.getGuiLeft(), -screen.getGuiTop());
        BodyHudRenderer.draw(graphics, body, placement.x(), placement.y(),
                HumanHudClientConfig.bodyHudScale(), HumanHudClientConfig.bodyHudOpacity());
        if (placement.contains(event.getMouseX(), event.getMouseY())) {
            graphics.outline(placement.x() - 3, placement.y() - 3,
                    placement.width() + 6, placement.height() + 6, 0xFF7BB69F);
        }
        graphics.pose().popMatrix();

        if (placement.contains(event.getMouseX(), event.getMouseY())) {
            Font font = Minecraft.getInstance().font;
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
