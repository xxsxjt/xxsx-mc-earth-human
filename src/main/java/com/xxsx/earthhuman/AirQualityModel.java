package com.xxsx.earthhuman;

/** Pure model for the slow air-exposure meter updated once per server second. */
public final class AirQualityModel {
    private AirQualityModel() {
    }

    public static double next(double current, boolean smoke, boolean dust, boolean underwater,
                              boolean nether, double filterStrength,
                              double lossMultiplier, double recoveryMultiplier) {
        double filter = HumanStatus.clamp(filterStrength, 0.0D, 0.95D);
        double protectedSources = (smoke ? 0.85D : 0.0D) + (dust ? 0.55D : 0.0D);
        // Underwater breathing is governed independently by vanilla air bubbles; this meter only tracks lung dust/smoke/ash.
        double environmentalSources = (nether ? 0.04D : 0.0D);
        double exposure = protectedSources * (1.0D - filter) + environmentalSources;
        if (exposure > 0.0D) {
            return HumanStatus.clamp(current - exposure * Math.max(0.0D, lossMultiplier), 0.0D, 100.0D);
        }
        return HumanStatus.clamp(current + 0.18D * Math.max(0.0D, recoveryMultiplier), 0.0D, 100.0D);
    }

    public static String stateId(double quality) {
        if (quality < 15.0D) {
            return "danger";
        }
        if (quality < 35.0D) {
            return "poor";
        }
        if (quality < 70.0D) {
            return "reduced";
        }
        return "good";
    }

    public record Exposure(boolean smoke, boolean dust, boolean underwater, boolean nether) {
    }
}
