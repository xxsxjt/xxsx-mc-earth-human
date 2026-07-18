package com.xxsx.earthhuman;

import java.util.Locale;

public enum BodyPart {
    HEAD("head", 5.0D),
    TORSO("torso", 7.0D),
    LIMBS("limbs", 8.0D);

    /**
     * Source-compatible aliases for integrations written against the early six-region beta.
     * They intentionally resolve to the single coarse limbs region.
     */
    @Deprecated public static final BodyPart LEFT_ARM = LIMBS;
    @Deprecated public static final BodyPart RIGHT_ARM = LIMBS;
    @Deprecated public static final BodyPart LEFT_LEG = LIMBS;
    @Deprecated public static final BodyPart RIGHT_LEG = LIMBS;

    public final String id;
    public final double maxHealth;

    BodyPart(String id, double maxHealth) {
        this.id = id;
        this.maxHealth = maxHealth;
    }

    public String translationKey() {
        return "bodypart.earth_human." + id;
    }

    public boolean isLimb() {
        return this == LIMBS;
    }

    public boolean isLeg() {
        return this == LIMBS;
    }

    public boolean isArm() {
        return this == LIMBS;
    }

    public static BodyPart byId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("limb") || normalized.equals("limbs")
                || normalized.equals("arms") || normalized.equals("legs")
                || normalized.equals("left_arm") || normalized.equals("right_arm")
                || normalized.equals("left_leg") || normalized.equals("right_leg")) {
            return LIMBS;
        }
        for (BodyPart part : values()) {
            if (part.id.equals(normalized) || part.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return part;
            }
        }
        return null;
    }
}
