package com.xxsx.earthhuman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarthHumanConfigTest {
    @Test
    void bridgeModeFallsBackToCompatibilityMode() {
        assertEquals(EarthHumanRules.HealthBridgeMode.VANILLA_COMPATIBLE,
                EarthHumanRules.HealthBridgeMode.byId("unknown"));
    }

    @Test
    void bridgeModeParsingIsCaseInsensitive() {
        assertEquals(EarthHumanRules.HealthBridgeMode.HEAD_BAR,
                EarthHumanRules.HealthBridgeMode.byId(" HEAD_BAR "));
    }

    @Test
    void presetValidationRejectsUnknownValues() {
        assertTrue(EarthHumanRules.GameplayPreset.isValid("casual"));
        assertFalse(EarthHumanRules.GameplayPreset.isValid("hardcore"));
    }
}
