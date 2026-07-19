package com.xxsx.earthhuman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreInjuryPolicyTest {
    @Test
    void ordinaryCombatAndUnknownCausesProduceNoCoreInjury() {
        var outcome = CoreInjuryPolicy.evaluate(CoreInjuryPolicy.Cause.NONE, 12.0D, 1.0D);
        assertFalse(outcome.changed());
        assertEquals(0.0D, outcome.legSeverity());
        assertEquals(0.0D, outcome.rightHandSeverity());
        assertEquals(0.0D, outcome.limbIntegrityDamage());
    }

    @Test
    void fallOnlyTargetsLegConditionAndLimbIntegrity() {
        var outcome = CoreInjuryPolicy.evaluate(CoreInjuryPolicy.Cause.FALL, 6.0D, 1.0D);
        assertTrue(outcome.changed());
        assertEquals(0.48D, outcome.legSeverity(), 0.000001D);
        assertEquals(0.0D, outcome.rightHandSeverity(), 0.000001D);
        assertEquals(1.08D, outcome.limbIntegrityDamage(), 0.000001D);
    }

    @Test
    void handCausesNeverCreateLegPenalties() {
        var cactus = CoreInjuryPolicy.evaluate(CoreInjuryPolicy.Cause.BARE_HAND_CACTUS, 1.0D, 1.0D);
        var mining = CoreInjuryPolicy.evaluate(CoreInjuryPolicy.Cause.BARE_HAND_HARD_MINING, 0.0D, 1.0D);
        assertEquals(0.0D, cactus.legSeverity(), 0.000001D);
        assertEquals(0.10D, cactus.rightHandSeverity(), 0.000001D);
        assertEquals(0.0D, mining.legSeverity(), 0.000001D);
        assertEquals(0.002D, mining.rightHandSeverity(), 0.000001D);
    }
}
