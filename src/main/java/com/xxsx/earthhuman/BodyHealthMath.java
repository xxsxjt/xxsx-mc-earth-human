package com.xxsx.earthhuman;

public final class BodyHealthMath {
    private BodyHealthMath() {
    }

    public static double migratePreservingRatio(double oldValue, double oldMaximum, double newMaximum) {
        double ratio = clamp(oldValue / Math.max(0.1D, oldMaximum), 0.0D, 1.0D);
        return Math.max(0.0D, newMaximum) * ratio;
    }

    public static double rescalePreservingRatio(double value, double oldMaximum, double newMaximum) {
        return migratePreservingRatio(value, oldMaximum, newMaximum);
    }

    public static double compatiblePartDamage(double finalVanillaDamage, double partMaximum,
                                              double vanillaMaximum, double multiplier) {
        double vanillaRatio = Math.max(0.0D, finalVanillaDamage) / Math.max(1.0D, vanillaMaximum);
        return Math.max(0.0D, partMaximum) * vanillaRatio * Math.max(0.0D, multiplier);
    }

    public static HealingStep applyHealing(double health, double maximum, double available) {
        double clampedHealth = clamp(health, 0.0D, Math.max(0.0D, maximum));
        double remaining = Math.max(0.0D, available);
        double used = Math.min(Math.max(0.0D, maximum - clampedHealth), remaining);
        return new HealingStep(clampedHealth + used, remaining - used);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record HealingStep(double health, double remaining) {
    }
}
