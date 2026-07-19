package com.xxsx.earthhuman;

/** Pure policy for the deterministic injuries that remain in the standalone core. */
public final class CoreInjuryPolicy {
    private CoreInjuryPolicy() {
    }

    public static Outcome evaluate(Cause cause, double finalDamage, double multiplier) {
        if (cause == null) {
            return Outcome.NONE;
        }
        double damage = Math.max(0.0D, finalDamage);
        double scale = Math.max(0.0D, multiplier);
        return switch (cause) {
            case FALL -> new Outcome(
                    SpecificInjuryMath.fallSeverityDelta(damage, scale),
                    0.0D,
                    damage * 0.18D * scale);
            case BARE_HAND_CACTUS -> new Outcome(
                    0.0D,
                    SpecificInjuryMath.cactusSeverityDelta(damage, scale),
                    damage * 0.04D * scale);
            case BARE_HAND_HARD_MINING -> new Outcome(
                    0.0D,
                    SpecificInjuryMath.bareHandMiningSeverityDelta(scale),
                    0.0D);
            case NONE -> Outcome.NONE;
        };
    }

    public enum Cause {
        NONE,
        FALL,
        BARE_HAND_CACTUS,
        BARE_HAND_HARD_MINING
    }

    public record Outcome(double legSeverity, double rightHandSeverity, double limbIntegrityDamage) {
        private static final Outcome NONE = new Outcome(0.0D, 0.0D, 0.0D);

        public boolean changed() {
            return legSeverity > 0.0001D
                    || rightHandSeverity > 0.0001D
                    || limbIntegrityDamage > 0.0001D;
        }
    }
}
