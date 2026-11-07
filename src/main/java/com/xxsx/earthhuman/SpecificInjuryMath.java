package com.xxsx.earthhuman;

/** Pure tuning rules for deterministic, non-combat injuries in the core mod. */
public final class SpecificInjuryMath {
    public static final double FRACTURE_THRESHOLD = 0.70D;
    private static final double FALL_SEVERITY_PER_DAMAGE = 0.08D;
    private static final double CACTUS_SEVERITY_PER_DAMAGE = 0.10D;
    private static final double BARE_HAND_MINING_SEVERITY_PER_TICK = 0.002D;

    private SpecificInjuryMath() {
    }

    public static boolean isFractured(double severity) {
        return clamp(severity) >= FRACTURE_THRESHOLD;
    }

    public static double addSeverity(double current, double amount) {
        return clamp(current + Math.max(0.0D, amount));
    }

    public static double recoverSeverity(double current, double amount) {
        return clamp(current - Math.max(0.0D, amount));
    }

    /**
     * Minor injuries (< FRACTURE_THRESHOLD) heal naturally and through food/rest.
     * Fractures (>= FRACTURE_THRESHOLD) require a splint, first-aid kit, or bandage immobilization
     * to drop below the fracture threshold before natural healing can finish the recovery.
     */
    public static double recoverNaturalSeverity(double current, double amount) {
        double clamped = clamp(current);
        if (clamped >= FRACTURE_THRESHOLD) {
            return clamped;
        }
        return clamp(clamped - Math.max(0.0D, amount));
    }

    public static double fallSeverityDelta(double finalDamage, double multiplier) {
        return Math.max(0.0D, finalDamage) * FALL_SEVERITY_PER_DAMAGE * Math.max(0.0D, multiplier);
    }

    public static double cactusSeverityDelta(double finalDamage, double multiplier) {
        return Math.max(0.0D, finalDamage) * CACTUS_SEVERITY_PER_DAMAGE * Math.max(0.0D, multiplier);
    }

    public static double bareHandMiningSeverityDelta(double multiplier) {
        return BARE_HAND_MINING_SEVERITY_PER_TICK * Math.max(0.0D, multiplier);
    }

    public static float rightHandMiningSpeedMultiplier(double severity) {
        double clamped = clamp(severity);
        if (clamped >= 0.75D) return 0.35F;
        if (clamped >= 0.30D) return 0.60F;
        if (clamped >= 0.10D) return 0.82F;
        return 1.0F;
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
