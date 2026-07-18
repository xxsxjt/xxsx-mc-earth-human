package com.xxsx.earthhuman;

/** Pure target-temperature model. World access stays in the event adapter. */
public final class ThermalModel {
    private ThermalModel() {
    }

    public static double target(Inputs input, double hydration) {
        double target = 37.0D;
        target += HumanStatus.clamp((input.biomeTemperature() - 0.8D) * 2.4D, -1.8D, 1.8D);
        if (input.nether()) {
            target += 1.10D;
        }
        if (input.wet()) {
            target -= 0.55D;
        }
        if (input.night()) {
            target -= 0.12D;
        }
        target += HumanStatus.clamp((64.0D - input.altitude()) * 0.003D, -0.60D, 0.60D);
        if (input.heatSource()) {
            target += 0.55D;
        }
        if (input.coldSource()) {
            target -= 0.30D;
        }
        if (hydration < 15.0D && target > 37.0D) {
            target += 0.35D;
        }
        return HumanStatus.clamp(target, 32.0D, 42.5D);
    }

    public record Inputs(double biomeTemperature, boolean nether, boolean wet, boolean night,
                         double altitude, boolean heatSource, boolean coldSource) {
    }
}
