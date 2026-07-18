package com.xxsx.earthhuman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@SuppressWarnings("deprecation")
class BodyPartTest {
    @Test
    void exposesOnlyTheCoarseThreeRegionModel() {
        assertEquals(3, BodyPart.values().length);
        assertEquals(BodyPart.LIMBS, BodyPart.byId("limbs"));
        assertEquals(BodyPart.LIMBS, BodyPart.byId("left_leg"));
        assertEquals(BodyPart.LIMBS, BodyPart.byId("RIGHT_ARM"));
    }

    @Test
    void legacyAliasesResolveToTheSharedLimbRegion() {
        assertSame(BodyPart.LIMBS, BodyPart.LEFT_ARM);
        assertSame(BodyPart.LIMBS, BodyPart.RIGHT_ARM);
        assertSame(BodyPart.LIMBS, BodyPart.LEFT_LEG);
        assertSame(BodyPart.LIMBS, BodyPart.RIGHT_LEG);
    }
}
