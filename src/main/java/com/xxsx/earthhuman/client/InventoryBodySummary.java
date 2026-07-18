package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.EarthHuman;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

import java.util.List;

/** Compact, clickable body summary attached to the vanilla survival inventory. */
final class InventoryBodySummary {
    private static final int PANEL_HEIGHT = 72;
    private static final int NORMAL_WIDTH = 82;
    private static final int COMPACT_WIDTH = 64;
    private static final int NARROW_WIDTH = 56;
    private static final int MARGIN = 4;

    private InventoryBodySummary() {
    }

    static void render(ContainerScreenEvent.Render.Foreground event, HumanBodySyncPayload body) {
        if (!(event.getContainerScreen() instanceof InventoryScreen screen)) {
            return;
        }

        Layout layout = layout(screen);
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        graphics.fill(layout.x - 1, layout.y - 1, layout.x + layout.width + 1,
                layout.y + layout.height + 1, 0xD0101718);
        graphics.outline(layout.x - 1, layout.y - 1, layout.width + 2, layout.height + 2, 0xFF78968C);
        graphics.text(font, Component.translatable("screen.earth_human.inventory.summary"),
                layout.x + 4, layout.y + 3, 0xFFF0F2EC, false);

        int rowHeight = Math.max(7, (layout.height - 17) / BodyPart.values().length);
        for (int index = 0; index < BodyPart.values().length; index++) {
            BodyPart part = BodyPart.values()[index];
            int rowY = layout.y + 14 + index * rowHeight;
            double ratio = clamp(body.value(part));
            int labelWidth = layout.width <= NARROW_WIDTH ? 14 : 20;
            graphics.text(font, label(font, part, labelWidth),
                    layout.x + 4, rowY, ratioColor(ratio), false);
            int barX = layout.x + 4 + labelWidth;
            int barWidth = Math.max(12, layout.width - labelWidth - 9);
            graphics.fill(barX, rowY + 2, barX + barWidth, rowY + 4, 0xFF26302E);
            int fill = Math.max(1, (int) Math.round(barWidth * ratio));
            graphics.fill(barX, rowY + 2, barX + fill, rowY + 4, ratioColor(ratio));
        }

        if (contains(layout, event.getMouseX(), event.getMouseY())) {
            BodyPart hovered = hoveredPart(layout, event.getMouseX(), event.getMouseY());
            if (hovered == null) {
                graphics.setTooltipForNextFrame(font,
                        Component.translatable("screen.earth_human.inventory.click_hint"),
                        event.getMouseX(), event.getMouseY());
            } else {
                double ratio = clamp(body.value(hovered));
                graphics.setComponentTooltipForNextFrame(font, List.of(
                                Component.translatable(hovered.translationKey()),
                                Component.translatable("screen.earth_human.inventory.part_value",
                                        Math.round(ratio * 100.0D)),
                                Component.translatable("screen.earth_human.inventory.click_hint")),
                        event.getMouseX(), event.getMouseY());
            }
        }
    }

    static boolean hitTest(Screen screen, double mouseX, double mouseY) {
        if (!(screen instanceof InventoryScreen inventoryScreen)) {
            return false;
        }
        return contains(layout(inventoryScreen), mouseX, mouseY);
    }

    @SuppressWarnings("removal")
    private static Layout layout(AbstractContainerScreen<?> screen) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int right = left + screen.getXSize();
        int bottom = top + screen.getYSize();
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        int rightSpace = screenWidth - right - MARGIN;
        if (rightSpace >= NORMAL_WIDTH) {
            return new Layout(right + MARGIN, top + 3, NORMAL_WIDTH, PANEL_HEIGHT);
        }
        if (rightSpace >= COMPACT_WIDTH) {
            return new Layout(right + MARGIN, top + 3, COMPACT_WIDTH, PANEL_HEIGHT);
        }

        int leftSpace = left - MARGIN;
        if (leftSpace >= NORMAL_WIDTH) {
            return new Layout(left - MARGIN - NORMAL_WIDTH, top + 3, NORMAL_WIDTH, PANEL_HEIGHT);
        }
        if (leftSpace >= COMPACT_WIDTH) {
            return new Layout(left - MARGIN - COMPACT_WIDTH, top + 3, COMPACT_WIDTH, PANEL_HEIGHT);
        }

        if (top - MARGIN >= PANEL_HEIGHT) {
            return new Layout(Math.max(MARGIN, left), top - MARGIN - PANEL_HEIGHT, COMPACT_WIDTH, PANEL_HEIGHT);
        }
        if (screenHeight - bottom - MARGIN >= PANEL_HEIGHT) {
            return new Layout(Math.max(MARGIN, left), bottom + MARGIN, COMPACT_WIDTH, PANEL_HEIGHT);
        }

        return new Layout(Math.max(MARGIN, screenWidth - NARROW_WIDTH - MARGIN), MARGIN,
                NARROW_WIDTH, PANEL_HEIGHT);
    }

    private static boolean contains(Layout layout, double mouseX, double mouseY) {
        return mouseX >= layout.x && mouseX < layout.x + layout.width
                && mouseY >= layout.y && mouseY < layout.y + layout.height;
    }

    private static BodyPart hoveredPart(Layout layout, double mouseX, double mouseY) {
        int rowHeight = Math.max(7, (layout.height - 17) / BodyPart.values().length);
        int row = (int) ((mouseY - layout.y - 14) / rowHeight);
        return row >= 0 && row < BodyPart.values().length ? BodyPart.values()[row] : null;
    }

    private static Component label(Font font, BodyPart part, int width) {
        Component translated = Component.translatable(part.translationKey());
        if (font.width(translated) <= width) {
            return translated;
        }
        return Component.translatable("screen.earth_human.inventory.label.short." + part.id);
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 0.0D;
    }

    private static int ratioColor(double ratio) {
        if (ratio <= 0.001D) {
            return 0xFF252827;
        }
        if (ratio < 0.30D) {
            return 0xFFD85A56;
        }
        if (ratio < 0.65D) {
            return 0xFFD5A44F;
        }
        return 0xFF7BB69F;
    }

    private record Layout(int x, int y, int width, int height) {
    }
}
