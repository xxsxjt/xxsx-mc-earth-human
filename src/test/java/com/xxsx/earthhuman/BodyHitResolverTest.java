package com.xxsx.earthhuman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BodyHitResolverTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void playerRightIsStableAcrossTheFourCardinalYawAngles() {
        assertEquals(1.0D, BodyHitResolver.localRightOffset(-1.0D, 0.0D, 0.0F), EPSILON);
        assertEquals(1.0D, BodyHitResolver.localRightOffset(0.0D, -1.0D, 90.0F), EPSILON);
        assertEquals(1.0D, BodyHitResolver.localRightOffset(1.0D, 0.0D, 180.0F), EPSILON);
        assertEquals(1.0D, BodyHitResolver.localRightOffset(0.0D, 1.0D, -90.0F), EPSILON);
    }

    @Test
    void impactHeightMapsToTheThreeStoredBodyRegions() {
        assertEquals(BodyPart.HEAD, BodyHitResolver.fromImpactPoint(0.9D, 0.0D));
        assertEquals(BodyPart.TORSO, BodyHitResolver.fromImpactPoint(0.60D, 0.0D));
        assertEquals(BodyPart.LIMBS, BodyHitResolver.fromImpactPoint(0.60D, 0.40D));
        assertEquals(BodyPart.LIMBS, BodyHitResolver.fromImpactPoint(0.60D, -0.40D));
        assertEquals(BodyPart.LIMBS, BodyHitResolver.fromImpactPoint(0.20D, 0.40D));
        assertEquals(BodyPart.LIMBS, BodyHitResolver.fromImpactPoint(0.20D, -0.40D));
    }
}
