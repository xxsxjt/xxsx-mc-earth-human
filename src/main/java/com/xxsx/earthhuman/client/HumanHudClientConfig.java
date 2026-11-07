package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.EarthHuman;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class HumanHudClientConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("earth_human-client.properties");
    private static boolean loaded;
    private static boolean showVitalsHud = true;
    private static boolean showBodyHud = true;
    private static boolean showNumericValues = true;
    private static HudCorner bodyHudCorner = HudCorner.TOP_LEFT;
    private static HudTheme hudTheme = HudTheme.CLINICAL;
    private static double bodyHudScale = 1.0D;
    private static double bodyHudOpacity = 0.5D;

    private HumanHudClientConfig() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            properties.load(in);
            showVitalsHud = Boolean.parseBoolean(properties.getProperty("showVitalsHud", "true"));
            showBodyHud = Boolean.parseBoolean(properties.getProperty("showBodyHud", "true"));
            showNumericValues = Boolean.parseBoolean(properties.getProperty("showNumericValues", "true"));
            bodyHudCorner = HudCorner.byId(properties.getProperty("bodyHudCorner", HudCorner.TOP_LEFT.id));
            hudTheme = HudTheme.byId(properties.getProperty("hudTheme", HudTheme.CLINICAL.id));
            bodyHudScale = clamp(parseDouble(properties.getProperty("bodyHudScale"), 1.0D), 0.6D, 2.0D);
            bodyHudOpacity = clamp(parseDouble(properties.getProperty("bodyHudOpacity"), 0.5D), 0.1D, 1.0D);
        } catch (IOException e) {
            EarthHuman.LOGGER.warn("[Earth Human] Failed to load client HUD config, using defaults", e);
        }
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("showVitalsHud", Boolean.toString(showVitalsHud));
        properties.setProperty("showBodyHud", Boolean.toString(showBodyHud));
        properties.setProperty("showNumericValues", Boolean.toString(showNumericValues));
        properties.setProperty("bodyHudCorner", bodyHudCorner.id);
        properties.setProperty("hudTheme", hudTheme.id);
        properties.setProperty("bodyHudScale", Double.toString(bodyHudScale));
        properties.setProperty("bodyHudOpacity", Double.toString(bodyHudOpacity));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(out, "Earth Human client HUD settings");
            }
        } catch (IOException e) {
            EarthHuman.LOGGER.warn("[Earth Human] Failed to save client HUD config", e);
        }
    }

    public static boolean showBodyHud() {
        load();
        return showBodyHud;
    }

    public static boolean showVitalsHud() {
        load();
        return showVitalsHud;
    }

    public static boolean showNumericValues() {
        load();
        return showNumericValues;
    }

    public static HudTheme hudTheme() {
        load();
        return hudTheme;
    }

    public static HudCorner bodyHudCorner() {
        load();
        return bodyHudCorner;
    }

    public static double bodyHudScale() {
        load();
        return bodyHudScale;
    }

    public static double bodyHudOpacity() {
        load();
        return bodyHudOpacity;
    }

    public static void setShowBodyHud(boolean value) {
        showBodyHud = value;
        save();
    }

    public static void setShowVitalsHud(boolean value) {
        showVitalsHud = value;
        save();
    }

    public static void setShowNumericValues(boolean value) {
        showNumericValues = value;
        save();
    }

    public static void setHudTheme(HudTheme value) {
        hudTheme = value == null ? HudTheme.CLINICAL : value;
        save();
    }

    public static void setBodyHudCorner(HudCorner value) {
        bodyHudCorner = value == null ? HudCorner.BOTTOM_LEFT : value;
        save();
    }

    public static void setBodyHudScale(double value) {
        bodyHudScale = clamp(value, 0.6D, 2.0D);
        save();
    }

    public static void setBodyHudOpacity(double value) {
        bodyHudOpacity = clamp(value, 0.1D, 1.0D);
        save();
    }

    public static void cycleTheme() {
        HudTheme[] values = HudTheme.values();
        hudTheme = values[(hudTheme.ordinal() + 1) % values.length];
        save();
    }

    public static void cycleCorner() {
        HudCorner[] values = HudCorner.values();
        bodyHudCorner = values[(bodyHudCorner.ordinal() + 1) % values.length];
        save();
    }

    public static void changeScale(double delta) {
        bodyHudScale = clamp(bodyHudScale + delta, 0.6D, 2.0D);
        save();
    }

    public static void changeOpacity(double delta) {
        bodyHudOpacity = clamp(bodyHudOpacity + delta, 0.1D, 1.0D);
        save();
    }

    public enum HudCorner {
        BOTTOM_LEFT("bottom_left"),
        BOTTOM_RIGHT("bottom_right"),
        TOP_LEFT("top_left"),
        TOP_RIGHT("top_right");

        public final String id;

        HudCorner(String id) {
            this.id = id;
        }

        public String translationKey() {
            return "screen.earth_human.settings.corner." + id;
        }

        static HudCorner byId(String value) {
            String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
            for (HudCorner corner : values()) {
                if (corner.id.equals(normalized)) {
                    return corner;
                }
            }
            return TOP_LEFT;
        }
    }

    public enum HudTheme {
        CLINICAL("clinical", 0x55FFFF, 0xFFFFFF, 0xFFAAFF),
        FIELD("field", 0x7ED957, 0xFFD166, 0xF3A6FF),
        HIGH_CONTRAST("high_contrast", 0x00E5FF, 0xFFFFFF, 0xFF7AE5);

        public final String id;
        public final int hydrationColor;
        public final int temperatureColor;
        public final int fatigueColor;

        HudTheme(String id, int hydrationColor, int temperatureColor, int fatigueColor) {
            this.id = id;
            this.hydrationColor = hydrationColor;
            this.temperatureColor = temperatureColor;
            this.fatigueColor = fatigueColor;
        }

        public String translationKey() {
            return "screen.earth_human.settings.theme." + id;
        }

        static HudTheme byId(String value) {
            String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
            for (HudTheme theme : values()) {
                if (theme.id.equals(normalized)) {
                    return theme;
                }
            }
            return CLINICAL;
        }
    }

    private static double parseDouble(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
