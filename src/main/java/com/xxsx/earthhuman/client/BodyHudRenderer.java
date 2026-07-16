package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BodyHudRenderer {
    private static final int BASE_WIDTH = 34;
    private static final int BASE_HEIGHT = 56;

    private BodyHudRenderer() {
    }

    public static int width(double scale) {
        return Math.max(1, scaled(BASE_WIDTH, scale));
    }

    public static int height(double scale) {
        return Math.max(1, scaled(BASE_HEIGHT, scale));
    }

    public static void draw(GuiGraphicsExtractor g, HumanBodySyncPayload body, int x, int y, double scale, double opacity) {
        int background = withAlpha(0x000000, (int) Math.round(70.0D * opacity));
        g.fill(x - 2, y - 2, x + width(scale) + 2, y + height(scale) + 2, background);
        drawPart(g, x, y, scale, opacity, body.value(BodyPart.HEAD), 12, 0, 10, 10);
        drawPart(g, x, y, scale, opacity, body.value(BodyPart.TORSO), 10, 12, 14, 21);
        drawPart(g, x, y, scale, opacity, body.value(BodyPart.LEFT_ARM), 3, 13, 6, 22);
        drawPart(g, x, y, scale, opacity, body.value(BodyPart.RIGHT_ARM), 25, 13, 6, 22);
        drawPart(g, x, y, scale, opacity, body.value(BodyPart.LEFT_LEG), 10, 35, 6, 21);
        drawPart(g, x, y, scale, opacity, body.value(BodyPart.RIGHT_LEG), 18, 35, 6, 21);
    }

    private static void drawPart(GuiGraphicsExtractor g, int originX, int originY, double scale, double opacity,
                                 double ratio, int x, int y, int width, int height) {
        int sx = originX + scaled(x, scale);
        int sy = originY + scaled(y, scale);
        int sw = Math.max(1, scaled(width, scale));
        int sh = Math.max(1, scaled(height, scale));
        g.fill(sx - 1, sy - 1, sx + sw + 1, sy + sh + 1, withAlpha(0x000000, alpha(opacity)));
        g.fill(sx, sy, sx + sw, sy + sh, partColor(ratio, opacity));
    }

    private static int partColor(double ratio, double opacity) {
        double value = Math.max(0.0D, Math.min(1.0D, ratio));
        if (value <= 0.001D) {
            return withAlpha(0x000000, alpha(opacity));
        }
        int red = (int) Math.round((1.0D - value) * 255.0D);
        int green = (int) Math.round(value * 220.0D);
        return withAlpha((red << 16) | (green << 8), alpha(opacity));
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
}
