package com.xxsx.earthhuman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BodyHealthMathTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void migrationPreservesRatioWhenModdedMaxHealthIsHigher() {
        double migrated = BodyHealthMath.migratePreservingRatio(5.0D, 10.0D, 20.0D);

        assertEquals(10.0D, migrated, EPSILON);
    }

    @Test
    void modelThreeMigrationUsesItsPreviouslyScaledCapacity() {
        double migrated = BodyHealthMath.migratePreservingRatio(5.0D, 10.0D, 10.0D);

        assertEquals(5.0D, migrated, EPSILON);
    }

    @Test
    void capacityRescalePreservesExistingInjuryRatio() {
        double rescaled = BodyHealthMath.rescalePreservingRatio(1.0D, 2.0D, 5.0D);

        assertEquals(2.5D, rescaled, EPSILON);
    }

    @Test
    void compatibleDamageUsesFinalVanillaHealthFraction() {
        double routed = BodyHealthMath.compatiblePartDamage(5.0D, 8.0D, 20.0D, 1.0D);

        assertEquals(2.0D, routed, EPSILON);
    }

    @Test
    void compatibleDamageHonorsConfiguredMultiplier() {
        double routed = BodyHealthMath.compatiblePartDamage(5.0D, 8.0D, 20.0D, 0.5D);

        assertEquals(1.0D, routed, EPSILON);
    }

    @Test
    void healingReturnsOverflowForTheNextBodyPart() {
        BodyHealthMath.HealingStep step = BodyHealthMath.applyHealing(3.0D, 5.0D, 4.0D);

        assertEquals(5.0D, step.health(), EPSILON);
        assertEquals(2.0D, step.remaining(), EPSILON);
    }
}
