package com.xxsx.earthhuman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanActivitySampleTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void referenceWalkingSecondMatchesPreviousBaseline() {
        HumanActivitySample sample = new HumanActivitySample(430, 0, 0, 0, 0, 0, 0);

        assertEquals(HumanActivityType.WALKING, sample.dominantType());
        assertEquals(0.18D, sample.exertionGain(), EPSILON);
        assertEquals(0.13D, sample.fatigueGain(0.0D), EPSILON);
        assertEquals(0.010D, sample.hydrationExtraLoss(), EPSILON);
    }

    @Test
    void referenceSprintUsesHigherLoadAndFatigue() {
        HumanActivitySample sample = new HumanActivitySample(0, 560, 0, 0, 0, 0, 0);

        assertEquals(HumanActivityType.SPRINTING, sample.dominantType());
        assertEquals(0.46D, sample.exertionGain(), EPSILON);
        assertEquals(0.76D, sample.fatigueGain(50.0D), EPSILON);
    }

    @Test
    void ridingDoesNotPretendToBeLegExercise() {
        HumanActivitySample sample = new HumanActivitySample(0, 0, 0, 0, 800, 0, 0);

        assertEquals(HumanActivityType.RIDING, sample.dominantType());
        assertTrue(sample.hasMovement());
        assertFalse(sample.hasPhysicalExertion());
        assertEquals(0.01D, sample.exertionGain(), EPSILON);
        assertEquals(0.01D, sample.fatigueGain(100.0D), EPSILON);
    }

    @Test
    void swimmingAndClimbingRemainDistinct() {
        HumanActivitySample swimming = new HumanActivitySample(0, 0, 300, 0, 0, 0, 0);
        HumanActivitySample climbing = new HumanActivitySample(0, 0, 0, 200, 0, 0, 0);

        assertEquals(HumanActivityType.SWIMMING, swimming.dominantType());
        assertEquals(HumanActivityType.CLIMBING, climbing.dominantType());
        assertEquals(0.40D, swimming.exertionGain(), EPSILON);
        assertEquals(0.38D, climbing.exertionGain(), EPSILON);
    }

    @Test
    void invalidNegativeDeltasAreIgnored() {
        HumanActivitySample sample = new HumanActivitySample(-100, -100, -100, -100, -100, -100, -100);

        assertEquals(HumanActivityType.IDLE, sample.dominantType());
        assertFalse(sample.hasMovement());
        assertEquals(0.0D, sample.fatigueGain(50.0D), EPSILON);
    }
}
