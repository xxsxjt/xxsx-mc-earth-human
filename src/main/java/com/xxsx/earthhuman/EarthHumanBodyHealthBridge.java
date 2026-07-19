package com.xxsx.earthhuman;

import com.xxsx.earthhuman.api.BodyHealthBridgeApi;

/**
 * Runtime boundary between the standalone human-condition core and the optional
 * body-health add-on. The core never changes vanilla health unless the add-on
 * explicitly activates this bridge.
 */
@Deprecated
public final class EarthHumanBodyHealthBridge {
    private static final String LEGACY_OWNER = "earth_human_legacy_bridge";

    private EarthHumanBodyHealthBridge() {
    }

    public static void activate(EarthHumanRules.HealthBridgeMode requestedMode) {
        EarthHumanRules.HealthBridgeMode requested = requestedMode == null
                ? EarthHumanRules.HealthBridgeMode.HEAD_BAR
                : requestedMode;
        BodyHealthBridgeApi.configure(LEGACY_OWNER, BodyHealthBridgeApi.Mode.byId(requested.id));
    }

    public static void deactivate() {
        BodyHealthBridgeApi.deactivate(LEGACY_OWNER);
    }

    public static boolean active() {
        return BodyHealthBridgeApi.active();
    }

    public static EarthHumanRules.HealthBridgeMode mode() {
        return EarthHumanRules.HealthBridgeMode.byId(BodyHealthBridgeApi.mode().id());
    }
}
