package com.xxsx.earthhuman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirQualityModelTest {
    @Test
    void dustFallsFasterWithoutAFilter() {
        double unfiltered = AirQualityModel.next(100.0D, false, true, false, false,
                0.0D, 1.0D, 1.0D);
        double filtered = AirQualityModel.next(100.0D, false, true, false, false,
                0.82D, 1.0D, 1.0D);
        assertTrue(unfiltered < filtered);
        assertEquals(99.45D, unfiltered, 1.0E-9D);
    }

    @Test
    void cleanAirRecoversWithoutOvershooting() {
        assertEquals(80.18D, AirQualityModel.next(80.0D, false, false, false, false,
                0.0D, 1.0D, 1.0D), 1.0E-9D);
        assertEquals(100.0D, AirQualityModel.next(100.0D, false, false, false, false,
                0.0D, 1.0D, 1.0D), 1.0E-9D);
    }
}
