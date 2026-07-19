package com.xxsx.earthhuman.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyHealthBridgeApiTest {
    private static final String OWNER_A = "test_body_addon_a";
    private static final String OWNER_B = "test_body_addon_b";

    @AfterEach
    void reset() {
        BodyHealthBridgeApi.deactivate(OWNER_A);
        BodyHealthBridgeApi.deactivate(OWNER_B);
    }

    @Test
    void startsInactiveAndRejectsInvalidOwners() {
        assertEquals(1, BodyHealthBridgeApi.apiVersion());
        assertEquals(BodyHealthBridgeApi.ConfigureResult.REJECTED_INVALID_REQUEST,
                BodyHealthBridgeApi.configure("bad owner", BodyHealthBridgeApi.Mode.HEAD_BAR));
        assertFalse(BodyHealthBridgeApi.active());
    }

    @Test
    void oneOwnerCanReloadItsModeButAnotherCannotOverrideIt() {
        assertEquals(BodyHealthBridgeApi.ConfigureResult.APPLIED,
                BodyHealthBridgeApi.configure(OWNER_A, BodyHealthBridgeApi.Mode.HEAD_BAR));
        assertTrue(BodyHealthBridgeApi.active());
        assertEquals(OWNER_A, BodyHealthBridgeApi.state().ownerId());

        assertEquals(BodyHealthBridgeApi.ConfigureResult.REJECTED_OWNED_BY_ANOTHER,
                BodyHealthBridgeApi.configure(OWNER_B, BodyHealthBridgeApi.Mode.VANILLA_COMPATIBLE));
        assertEquals(BodyHealthBridgeApi.Mode.HEAD_BAR, BodyHealthBridgeApi.mode());

        assertEquals(BodyHealthBridgeApi.ConfigureResult.APPLIED,
                BodyHealthBridgeApi.configure(OWNER_A, BodyHealthBridgeApi.Mode.OVERLAY_ONLY));
        assertFalse(BodyHealthBridgeApi.active());
    }
}
