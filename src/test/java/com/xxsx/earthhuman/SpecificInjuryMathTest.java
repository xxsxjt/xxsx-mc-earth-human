package com.xxsx.earthhuman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecificInjuryMathTest {
    @Test
    void severityIsClampedAndRecoversWithoutGoingNegative() {
        assertEquals(1.0D, SpecificInjuryMath.addSeverity(0.9D, 0.4D), 0.000001D);
        assertEquals(0.0D, SpecificInjuryMath.recoverSeverity(0.1D, 0.4D), 0.000001D);
    }

    @Test
    void deterministicInjuriesScaleFromFinalDamage() {
        assertEquals(0.48D, SpecificInjuryMath.fallSeverityDelta(6.0D, 1.0D), 0.000001D);
        assertEquals(0.20D, SpecificInjuryMath.cactusSeverityDelta(1.0D, 2.0D), 0.000001D);
        assertEquals(0.002D, SpecificInjuryMath.bareHandMiningSeverityDelta(1.0D), 0.000001D);
    }

    @Test
    void rightHandPenaltyHasStableSeverityBands() {
        assertEquals(1.0F, SpecificInjuryMath.rightHandMiningSpeedMultiplier(0.09D));
        assertEquals(0.82F, SpecificInjuryMath.rightHandMiningSpeedMultiplier(0.10D));
        assertEquals(0.60F, SpecificInjuryMath.rightHandMiningSpeedMultiplier(0.30D));
        assertEquals(0.35F, SpecificInjuryMath.rightHandMiningSpeedMultiplier(0.75D));
    }
}
