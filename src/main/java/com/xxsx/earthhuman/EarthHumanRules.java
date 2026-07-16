package com.xxsx.earthhuman;

import java.util.Locale;

public final class EarthHumanRules {
    private EarthHumanRules() {
    }

    public enum GameplayPreset {
        CASUAL("casual", 0.55D, 0.65D, 1.25D, 0.65D, 0.75D, 0.55D, 1.25D, 1.15D),
        STANDARD("standard", 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D),
        REALISTIC("realistic", 1.30D, 1.25D, 0.90D, 1.20D, 1.15D, 1.20D, 0.90D, 1.0D),
        CUSTOM("custom", 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D);

        public final String id;
        final double hydrationLoss;
        final double fatigueGain;
        final double recovery;
        final double temperatureRate;
        final double bodyDamage;
        final double bodySpread;
        final double healing;
        final double trainingGain;

        GameplayPreset(String id, double hydrationLoss, double fatigueGain, double recovery,
                       double temperatureRate, double bodyDamage, double bodySpread,
                       double healing, double trainingGain) {
            this.id = id;
            this.hydrationLoss = hydrationLoss;
            this.fatigueGain = fatigueGain;
            this.recovery = recovery;
            this.temperatureRate = temperatureRate;
            this.bodyDamage = bodyDamage;
            this.bodySpread = bodySpread;
            this.healing = healing;
            this.trainingGain = trainingGain;
        }

        public static GameplayPreset byId(String value) {
            String normalized = normalize(value);
            for (GameplayPreset preset : values()) {
                if (preset.id.equals(normalized)) {
                    return preset;
                }
            }
            return STANDARD;
        }

        public static boolean isValid(String value) {
            String normalized = normalize(value);
            for (GameplayPreset preset : values()) {
                if (preset.id.equals(normalized)) {
                    return true;
                }
            }
            return false;
        }
    }

    public enum HealthBridgeMode {
        VANILLA_COMPATIBLE("vanilla_compatible"),
        HEAD_BAR("head_bar"),
        OVERLAY_ONLY("overlay_only");

        public final String id;

        HealthBridgeMode(String id) {
            this.id = id;
        }

        public static HealthBridgeMode byId(String value) {
            String normalized = normalize(value);
            for (HealthBridgeMode mode : values()) {
                if (mode.id.equals(normalized)) {
                    return mode;
                }
            }
            return VANILLA_COMPATIBLE;
        }

        public static boolean isValid(String value) {
            String normalized = normalize(value);
            for (HealthBridgeMode mode : values()) {
                if (mode.id.equals(normalized)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
