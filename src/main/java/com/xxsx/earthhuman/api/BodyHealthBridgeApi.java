package com.xxsx.earthhuman.api;

import java.util.Locale;

/** Stable ownership contract for optional mods that activate split body health. */
public final class BodyHealthBridgeApi {
    public static final int API_VERSION = 1;

    private static volatile State state = new State("", Mode.OVERLAY_ONLY);

    private BodyHealthBridgeApi() {
    }

    public static int apiVersion() {
        return API_VERSION;
    }

    public static synchronized ConfigureResult configure(String ownerId, Mode requestedMode) {
        String owner = normalizeOwner(ownerId);
        if (owner.isEmpty() || requestedMode == null) {
            return ConfigureResult.REJECTED_INVALID_REQUEST;
        }

        State current = state;
        if (current.active() && !current.ownerId().equals(owner)) {
            return ConfigureResult.REJECTED_OWNED_BY_ANOTHER;
        }
        state = requestedMode == Mode.OVERLAY_ONLY
                ? new State("", Mode.OVERLAY_ONLY)
                : new State(owner, requestedMode);
        return ConfigureResult.APPLIED;
    }

    public static synchronized boolean deactivate(String ownerId) {
        String owner = normalizeOwner(ownerId);
        if (owner.isEmpty()) {
            return false;
        }
        State current = state;
        if (!current.active()) {
            return true;
        }
        if (!current.ownerId().equals(owner)) {
            return false;
        }
        state = new State("", Mode.OVERLAY_ONLY);
        return true;
    }

    public static State state() {
        return state;
    }

    public static boolean active() {
        return state.active();
    }

    public static Mode mode() {
        return state.mode();
    }

    private static String normalizeOwner(String ownerId) {
        if (ownerId == null) {
            return "";
        }
        String normalized = ownerId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "";
        }
        for (int i = 0; i < normalized.length(); i++) {
            char value = normalized.charAt(i);
            if ((value < 'a' || value > 'z')
                    && (value < '0' || value > '9')
                    && value != '_' && value != '-' && value != '.') {
                return "";
            }
        }
        return normalized;
    }

    public enum ConfigureResult {
        APPLIED,
        REJECTED_INVALID_REQUEST,
        REJECTED_OWNED_BY_ANOTHER
    }

    public enum Mode {
        VANILLA_COMPATIBLE("vanilla_compatible"),
        HEAD_BAR("head_bar"),
        OVERLAY_ONLY("overlay_only");

        private final String id;

        Mode(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Mode byId(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            for (Mode mode : values()) {
                if (mode.id.equals(normalized)) {
                    return mode;
                }
            }
            return OVERLAY_ONLY;
        }

        public static boolean isValid(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            for (Mode mode : values()) {
                if (mode.id.equals(normalized)) {
                    return true;
                }
            }
            return false;
        }
    }

    public record State(String ownerId, Mode mode) {
        public boolean active() {
            return mode != Mode.OVERLAY_ONLY;
        }
    }
}
