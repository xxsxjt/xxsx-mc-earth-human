package com.xxsx.earthhuman;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class EarthHumanBodyHealthBridgeTest {
    @AfterEach
    void resetBridge() {
        EarthHumanBodyHealthBridge.deactivate();
    }

    @Test
    void coreRemainsInactiveUntilAddonActivation() {
        EarthHumanBodyHealthBridge.deactivate();
        assertFalse(EarthHumanBodyHealthBridge.active());
        assertEquals(EarthHumanRules.HealthBridgeMode.OVERLAY_ONLY, EarthHumanBodyHealthBridge.mode());
    }

    @Test
    void addonCanActivateAndDisableTheBridge() {
        EarthHumanBodyHealthBridge.activate(EarthHumanRules.HealthBridgeMode.HEAD_BAR);
        assertTrue(EarthHumanBodyHealthBridge.active());
        assertEquals(EarthHumanRules.HealthBridgeMode.HEAD_BAR, EarthHumanBodyHealthBridge.mode());

        EarthHumanBodyHealthBridge.activate(EarthHumanRules.HealthBridgeMode.OVERLAY_ONLY);
        assertFalse(EarthHumanBodyHealthBridge.active());
        assertEquals(EarthHumanRules.HealthBridgeMode.OVERLAY_ONLY, EarthHumanBodyHealthBridge.mode());
    }
}
