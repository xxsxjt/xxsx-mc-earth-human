package com.xxsx.earthhuman;

/** Pure geometry used by damage routing and client-side diagnostics. */
public final class BodyHitResolver {
    private BodyHitResolver() {
    }

    /**
     * Returns the hit offset along the player's anatomical right direction.
     * Minecraft yaw 0 faces +Z, so the player's right vector is (-X, -Z).
     */
    public static double localRightOffset(double relativeX, double relativeZ, float yRot) {
        double yaw = Math.toRadians(yRot);
        return relativeX * -Math.cos(yaw) + relativeZ * -Math.sin(yaw);
    }

    public static BodyPart sidePart(double localRightOffset, BodyPart left, BodyPart right) {
        return localRightOffset < 0.0D ? left : right;
    }

    public static BodyPart fromImpactPoint(double verticalRatio, double localRightOffset) {
        if (verticalRatio >= 0.78D) {
            return BodyPart.HEAD;
        }
        if (verticalRatio < 0.42D || Math.abs(localRightOffset) > 0.28D) {
            return BodyPart.LIMBS;
        }
        return BodyPart.TORSO;
    }
}
