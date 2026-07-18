package com.xxsx.earthhuman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalModelTest {
    @Test
    void wetNightAtAltitudeIsColderThanAStableTemperateDay() {
        double normal = ThermalModel.target(new ThermalModel.Inputs(0.8D, false,
                false, false, 64.0D, false, false), 100.0D);
        double cold = ThermalModel.target(new ThermalModel.Inputs(0.8D, false,
                true, true, 180.0D, false, false), 100.0D);
        assertTrue(cold < normal);
    }

    @Test
    void hotBiomeAndHeatSourceRaiseTargetTemperature() {
        double target = ThermalModel.target(new ThermalModel.Inputs(1.8D, true,
                false, false, 64.0D, true, false), 10.0D);
        assertTrue(target > 39.0D);
    }
}
