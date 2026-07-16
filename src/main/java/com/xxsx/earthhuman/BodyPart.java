package com.xxsx.earthhuman;

import java.util.Locale;

public enum BodyPart {
    HEAD("head", 5.0D),
    TORSO("torso", 7.0D),
    LEFT_ARM("left_arm", 2.0D),
    RIGHT_ARM("right_arm", 2.0D),
    LEFT_LEG("left_leg", 2.0D),
    RIGHT_LEG("right_leg", 2.0D);

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
        return this == LEFT_ARM || this == RIGHT_ARM || this == LEFT_LEG || this == RIGHT_LEG;
    }

    public boolean isLeg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }

    public boolean isArm() {
        return this == LEFT_ARM || this == RIGHT_ARM;
    }

    public static BodyPart byId(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (BodyPart part : values()) {
            if (part.id.equals(normalized) || part.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return part;
            }
        }
        return null;
    }
}
