package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BodyHudRenderer {
    private static final int BASE_WIDTH = 38;
    private static final int BASE_HEIGHT = 60;

    private BodyHudRenderer() {
    }

    public static int width(double scale) {
        return Math.max(1, scaled(BASE_WIDTH, scale));
    }

    public static int height(double scale) {
        return Math.max(1, scaled(BASE_HEIGHT, scale));
    }

    public static Placement placement(int guiWidth, int guiHeight) {
        double scale = HumanHudClientConfig.bodyHudScale();
        int width = width(scale);
        int height = height(scale);
        int margin = 6;
        int x = switch (HumanHudClientConfig.bodyHudCorner()) {
            case BOTTOM_LEFT, TOP_LEFT -> margin;
            case BOTTOM_RIGHT, TOP_RIGHT -> guiWidth - width - margin;
        };
        int y = switch (HumanHudClientConfig.bodyHudCorner()) {
            case TOP_LEFT, TOP_RIGHT -> margin;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> guiHeight - height - margin;
        };
        return new Placement(x, y, width, height);
    }

    public static void draw(GuiGraphicsExtractor g, HumanBodySyncPayload body, int x, int y, double scale, double opacity) {
        int w = width(scale);
        int h = height(scale);
        int background = withAlpha(0x101617, (int) Math.round(108.0D * opacity));
        int edge = withAlpha(0x8FA39D, (int) Math.round(115.0D * opacity));
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, background);
        g.outline(x - 2, y - 2, w + 4, h + 4, edge);
        drawCornerMarks(g, x, y, scale, opacity);

        drawSegment(g, x, y, scale, opacity, body.value(BodyPart.HEAD), 13, 2, 12, 11);
        drawSegment(g, x, y, scale, opacity, body.value(BodyPart.TORSO), 10, 15, 18, 24);
        // Front-facing silhouette: the player's right is on the viewer's left.
        double limbIntegrity = body.value(BodyPart.LIMBS);
        double rightHandCondition = injuryCondition(body.rightHandInjury());
        double legCondition = injuryCondition(body.legInjury());
        drawSegment(g, x, y, scale, opacity, rightHandCondition, 4, 16, 6, 28);
        drawSegment(g, x, y, scale, opacity, limbIntegrity, 28, 16, 6, 28);
        drawSegment(g, x, y, scale, opacity, legCondition, 11, 41, 7, 18);
        drawSegment(g, x, y, scale, opacity, legCondition, 20, 41, 7, 18);
    }

    private static void drawSegment(GuiGraphicsExtractor g, int originX, int originY, double scale, double opacity,
                                    double ratio, int x, int y, int width, int height) {
        int sx = originX + scaled(x, scale);
        int sy = originY + scaled(y, scale);
        int sw = Math.max(1, scaled(width, scale));
        int sh = Math.max(1, scaled(height, scale));
        int outline = withAlpha(0x07100E, alpha(opacity));
        int fill = partColor(ratio, opacity);
        g.fill(sx - 1, sy - 1, sx + sw + 1, sy + sh + 1, outline);
        g.fill(sx, sy, sx + sw, sy + sh, fill);
        if (sh >= 4 && sw >= 3 && ratio > 0.001D) {
            g.fill(sx + 1, sy + 1, sx + sw - 1, sy + 2, highlight(fill, opacity));
        }
    }

    private static void drawCornerMarks(GuiGraphicsExtractor g, int x, int y, double scale, double opacity) {
        int color = withAlpha(0x7BB69F, (int) Math.round(150.0D * opacity));
        int len = Math.max(2, scaled(4, scale));
        int w = width(scale);
        int h = height(scale);
        g.fill(x, y, x + len, y + 1, color);
        g.fill(x, y, x + 1, y + len, color);
        g.fill(x + w - len, y, x + w, y + 1, color);
        g.fill(x + w - 1, y, x + w, y + len, color);
        g.fill(x, y + h - 1, x + len, y + h, color);
        g.fill(x, y + h - len, x + 1, y + h, color);
        g.fill(x + w - len, y + h - 1, x + w, y + h, color);
        g.fill(x + w - 1, y + h - len, x + w, y + h, color);
    }

    private static int partColor(double ratio, double opacity) {
        double value = Math.max(0.0D, Math.min(1.0D, ratio));
        if (value <= 0.001D) {
            return withAlpha(0x050707, alpha(opacity));
        }
        int red = (int) Math.round(214.0D - value * 132.0D);
        int green = (int) Math.round(60.0D + value * 132.0D);
        int blue = (int) Math.round(54.0D + value * 58.0D);
        return withAlpha((red << 16) | (green << 8) | blue, alpha(opacity));
    }

    private static double injuryCondition(double severity) {
        return Math.max(0.0D, Math.min(1.0D, 1.0D - severity));
    }

    private static int highlight(int color, double opacity) {
        int rgb = color & 0x00FFFFFF;
        int red = Math.min(255, ((rgb >> 16) & 0xFF) + 28);
        int green = Math.min(255, ((rgb >> 8) & 0xFF) + 28);
        int blue = Math.min(255, (rgb & 0xFF) + 28);
        return withAlpha((red << 16) | (green << 8) | blue, (int) Math.round(alpha(opacity) * 0.72D));
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    private static int alpha(double opacity) {
        return (int) Math.round(Math.max(0.1D, Math.min(1.0D, opacity)) * 255.0D);
    }

    private static int scaled(int value, double scale) {
        return (int) Math.round(value * Math.max(0.1D, scale));
    }

    public record Placement(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x - 2 && mouseX < x + width + 2
                    && mouseY >= y - 2 && mouseY < y + height + 2;
        }
    }
}
