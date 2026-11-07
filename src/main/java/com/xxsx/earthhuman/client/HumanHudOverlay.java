package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.CareAction;
import com.xxsx.earthhuman.ExerciseAction;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import com.xxsx.earthhuman.HumanCareStatePayload;
import com.xxsx.earthhuman.HumanExerciseSessionPayload;
import com.xxsx.earthhuman.HumanStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Clean, conditional HUD overlay for Earth Human.
 * Displays top-left status indicators ONLY when temperature is abnormal, fatigue is high,
 * hydration is low (thirsty), any body part is injured, or an exercise/care session is active.
 */
public final class HumanHudOverlay {
    private static final int ROW_HEIGHT = 14;

    private HumanHudOverlay() {
    }

    public static boolean isTemperatureAbnormal(double bodyTemperature) {
        return bodyTemperature < 35.8D || bodyTemperature > 37.8D;
    }

    public static boolean isFatigueHigh(double fatigue) {
        return fatigue >= 45.0D;
    }

    public static boolean isThirsty(double hydration) {
        return hydration <= 70.0D;
    }

    public static boolean shouldDisplayStatus(
            double hydration,
            double bodyTemperature,
            double fatigue,
            double head,
            double torso,
            double limbs,
            double legInjury,
            double rightHandInjury,
            boolean exerciseActive,
            boolean careActive) {
        return exerciseActive
                || careActive
                || head < 0.99D
                || torso < 0.99D
                || limbs < 0.99D
                || legInjury > 0.01D
                || rightHandInjury > 0.01D
                || isTemperatureAbnormal(bodyTemperature)
                || isFatigueHigh(fatigue)
                || isThirsty(hydration);
    }

    public static boolean isBodyInjured(HumanBodySyncPayload body) {
        if (body == null) {
            return false;
        }
        return body.head() < 0.99D
                || body.torso() < 0.99D
                || body.limbs() < 0.99D
                || body.legInjury() > 0.01D
                || body.rightHandInjury() > 0.01D;
    }

    public static boolean shouldDisplayHud(
            HumanStatus status,
            HumanBodySyncPayload body,
            HumanExerciseSessionPayload exercise,
            HumanCareStatePayload care) {
        if (exercise != null && exercise.active()) {
            return true;
        }
        if (care != null && care.active()) {
            return true;
        }
        if (isBodyInjured(body)) {
            return true;
        }
        if (status == null) {
            return false;
        }
        return isTemperatureAbnormal(status.bodyTemperature)
                || isFatigueHigh(status.fatigue)
                || isThirsty(status.hydration);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null || minecraft.options.hideGui || !minecraft.gameMode.canHurtPlayer()) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        HumanStatus status = EarthHumanClient.syncedStatus();
        HumanBodySyncPayload body = EarthHumanClient.syncedBody();
        HumanExerciseSessionPayload exercise = EarthHumanClient.syncedExercise();
        HumanCareStatePayload care = EarthHumanClient.syncedCareState();

        if (!shouldDisplayHud(status, body, exercise, care)) {
            return;
        }

        Font font = minecraft.font;
        renderTopLeftOverlay(graphics, font, status, body, exercise, care);

        if (!HumanHudClientConfig.showVitalsHud()) {
            return;
        }
        if (!isTemperatureAbnormal(status.bodyTemperature)
                && !isFatigueHigh(status.fatigue)
                && !isThirsty(status.hydration)) {
            return;
        }

        int x = graphics.guiWidth() / 2 + 10;
        int y = graphics.guiHeight() - minecraft.gui.rightHeight;

        int firstWidth = 42;
        int secondWidth = 48;
        int thirdWidth = 47;
        int totalWidth = firstWidth + secondWidth + thirdWidth + 4;
        graphics.fill(x - 2, y - 1, x + totalWidth + 2, y + 12, 0xA8141A1C);
        graphics.outline(x - 2, y - 1, totalWidth + 2, 13, 0xAA52615D);
        drawCell(graphics, font, x, y + 1, firstWidth, status.hydration / 100.0D, hydrationColor(status.hydration),
                hudLabel("hud.earth_human.hydration.short", status.hydration, false));
        drawCell(graphics, font, x + firstWidth + 2, y + 1, secondWidth,
                (status.bodyTemperature - 32.0D) / 10.5D, temperatureColor(status.bodyTemperature),
                hudLabel("hud.earth_human.temperature.short", status.bodyTemperature, true));
        drawCell(graphics, font, x + firstWidth + secondWidth + 4, y + 1, thirdWidth,
                status.fatigue / 100.0D, fatigueColor(status.fatigue),
                hudLabel("hud.earth_human.fatigue.short", status.fatigue, false));

        minecraft.gui.rightHeight += ROW_HEIGHT;
    }

    private static void renderTopLeftOverlay(
            GuiGraphicsExtractor graphics,
            Font font,
            HumanStatus status,
            HumanBodySyncPayload body,
            HumanExerciseSessionPayload exercise,
            HumanCareStatePayload care) {
        if (!HumanHudClientConfig.showBodyHud()) {
            return;
        }

        boolean drawSilhouette = isBodyInjured(body)
                || (exercise != null && exercise.active())
                || (care != null && care.active());

        BodyHudRenderer.Placement placement = BodyHudRenderer.placement(graphics.guiWidth(), graphics.guiHeight());
        if (drawSilhouette) {
            BodyHudRenderer.draw(graphics, body, placement.x(), placement.y(),
                    HumanHudClientConfig.bodyHudScale(), HumanHudClientConfig.bodyHudOpacity());
        }

        List<HudBadge> badges = collectActiveBadges(status, body, exercise, care);
        if (badges.isEmpty()) {
            return;
        }

        int startX = 6;
        int startY = 6;
        if (drawSilhouette && HumanHudClientConfig.bodyHudCorner() == HumanHudClientConfig.HudCorner.TOP_LEFT) {
            startX = placement.x() + placement.width() + 6;
            startY = placement.y();
        }

        int badgeWidth = 108;
        int badgeHeight = 14;
        for (int i = 0; i < badges.size(); i++) {
            HudBadge badge = badges.get(i);
            int by = startY + i * (badgeHeight + 3);
            drawCell(graphics, font, startX, by, badgeWidth, badge.ratio(), badge.color(), badge.label());
        }
    }

    private static List<HudBadge> collectActiveBadges(
            HumanStatus status,
            HumanBodySyncPayload body,
            HumanExerciseSessionPayload exercise,
            HumanCareStatePayload care) {
        List<HudBadge> badges = new ArrayList<>();
        if (exercise != null && exercise.active()) {
            ExerciseAction action = ExerciseAction.byId(exercise.actionId());
            String name = action != null
                    ? Component.translatable(action.translationKey()).getString()
                    : Component.translatable("hud.earth_human.exercise.active").getString();
            double progress = 1.0D - (exercise.remainingTicks() / (double) Math.max(1, exercise.totalTicks()));
            String label = String.format(Locale.ROOT, "%s %.1fs", name, exercise.remainingTicks() / 20.0D);
            badges.add(new HudBadge(label, progress, 0xFFD4883A));
        }
        if (care != null && care.active()) {
            CareAction action = CareAction.byId(care.actionId());
            String name = action != null
                    ? Component.translatable(action.titleKey()).getString()
                    : Component.translatable("screen.earth_human.status.care.running").getString();
            double progress = 1.0D - (care.remainingTicks() / (double) Math.max(1, care.totalTicks()));
            String label = String.format(Locale.ROOT, "%s %.1fs", name, care.remainingTicks() / 20.0D);
            badges.add(new HudBadge(label, progress, 0xFF7BB69F));
        }
        if (body != null) {
            if (body.rightHandInjury() > 0.01D) {
                double cond = Math.max(0.0D, 1.0D - body.rightHandInjury());
                String label = Component.translatable("bodyinjury.earth_human.right_hand").getString()
                        + " " + Math.round(cond * 100.0D) + "%";
                badges.add(new HudBadge(label, cond, body.rightHandInjury() >= 0.70D ? 0xFFFF5555 : 0xFFFFAA00));
            }
            if (body.legInjury() > 0.01D) {
                double cond = Math.max(0.0D, 1.0D - body.legInjury());
                String label = Component.translatable("bodyinjury.earth_human.leg").getString()
                        + " " + Math.round(cond * 100.0D) + "%";
                badges.add(new HudBadge(label, cond, body.legInjury() >= 0.70D ? 0xFFFF5555 : 0xFFFFAA00));
            }
            for (BodyPart part : BodyPart.values()) {
                double ratio = body.value(part);
                if (ratio < 0.99D) {
                    String label = Component.translatable(part.translationKey()).getString()
                            + " " + Math.round(ratio * 100.0D) + "%";
                    badges.add(new HudBadge(label, ratio, ratio < 0.35D ? 0xFFFF5555 : 0xFFFFAA00));
                }
            }
        }
        if (status != null) {
            if (isTemperatureAbnormal(status.bodyTemperature)) {
                badges.add(new HudBadge(
                        hudLabel("hud.earth_human.temperature.short", status.bodyTemperature, true) + "°C",
                        (status.bodyTemperature - 32.0D) / 10.5D,
                        temperatureColor(status.bodyTemperature)));
            }
            if (isThirsty(status.hydration)) {
                badges.add(new HudBadge(
                        hudLabel("hud.earth_human.hydration.short", status.hydration, false) + "%",
                        status.hydration / 100.0D,
                        hydrationColor(status.hydration)));
            }
            if (isFatigueHigh(status.fatigue)) {
                badges.add(new HudBadge(
                        hudLabel("hud.earth_human.fatigue.short", status.fatigue, false) + "%",
                        status.fatigue / 100.0D,
                        fatigueColor(status.fatigue)));
            }
        }
        return badges;
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
        if (value < 35.8D) return 0x55AAFF;
        if (value > 37.8D) return 0xFFAA00;
        return HumanHudClientConfig.hudTheme().temperatureColor;
    }

    private static int fatigueColor(double value) {
        if (value > 80.0D) return 0xFF5555;
        if (value >= 45.0D) return 0xFFAA00;
        return HumanHudClientConfig.hudTheme().fatigueColor;
    }

    private static String hudLabel(String key, double value, boolean decimal) {
        String label = Component.translatable(key).getString();
        if (!HumanHudClientConfig.showNumericValues()) {
            return label;
        }
        String rendered = decimal
                ? String.format(Locale.ROOT, "%.1f", value)
                : Long.toString(Math.round(value));
        return label + " " + rendered;
    }

    private record HudBadge(String label, double ratio, int color) {
    }
}
