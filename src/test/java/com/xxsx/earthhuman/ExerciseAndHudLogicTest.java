package com.xxsx.earthhuman;

import com.xxsx.earthhuman.client.HumanHudOverlay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExerciseAndHudLogicTest {
    @Test
    void resolvesAllCalisthenicsActionsAndAliases() {
        assertEquals(ExerciseAction.PUSH_UP, ExerciseAction.byId("push_up"));
        assertEquals(ExerciseAction.PUSH_UP, ExerciseAction.byId("pushup"));
        assertEquals(ExerciseAction.SQUAT, ExerciseAction.byId("squat"));
        assertEquals(ExerciseAction.SIT_UP, ExerciseAction.byId("sit_up"));
        assertEquals(ExerciseAction.SIT_UP, ExerciseAction.byId("situp"));
        assertEquals(ExerciseAction.PULL_UP, ExerciseAction.byId("pull_up"));
        assertEquals(ExerciseAction.PULL_UP, ExerciseAction.byId("pullup"));
    }

    @Test
    void hudOverlayDisplaysOnlyWhenAbnormalOrActive() {
        assertFalse(HumanHudOverlay.shouldDisplayStatus(
                100.0D, 37.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, false, false));

        // Active exercise triggers HUD
        assertTrue(HumanHudOverlay.shouldDisplayStatus(
                100.0D, 37.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, true, false));

        // Abnormal temperature, thirst, or high fatigue triggers HUD
        assertTrue(HumanHudOverlay.shouldDisplayStatus(
                100.0D, 38.5D, 0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, false, false));
        assertTrue(HumanHudOverlay.shouldDisplayStatus(
                55.0D, 37.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, false, false));
        assertTrue(HumanHudOverlay.shouldDisplayStatus(
                100.0D, 37.0D, 60.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, false, false));

        // Right hand abrasion or body part injury triggers HUD
        assertTrue(HumanHudOverlay.shouldDisplayStatus(
                100.0D, 37.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.15D, false, false));
    }

    @Test
    void minorAbrasionHealsNaturallyWhileFractureRequiresTreatment() {
        double minorHandAbrasion = 0.25D;
        double healed = SpecificInjuryMath.recoverNaturalSeverity(minorHandAbrasion, 0.10D);
        assertEquals(0.15D, healed, 1.0E-6D);

        double fracturedLeg = 0.80D;
        assertEquals(0.80D, SpecificInjuryMath.recoverNaturalSeverity(fracturedLeg, 0.20D), 1.0E-6D);
        double splinted = SpecificInjuryMath.recoverSeverity(fracturedLeg, 0.68D);
        assertEquals(0.12D, splinted, 1.0E-6D);
        assertFalse(SpecificInjuryMath.isFractured(splinted));
    }
}
