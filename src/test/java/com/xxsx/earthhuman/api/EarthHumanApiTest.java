package com.xxsx.earthhuman.api;

import com.xxsx.earthhuman.BodyPart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EarthHumanApiTest {
    @Test
    void exposesVersionOneContract() {
        assertEquals(1, EarthHumanApiContract.apiVersion());
    }

    @Test
    void resolvesUniqueValidBodyParts() {
        assertEquals(List.of(BodyPart.TORSO, BodyPart.LIMBS),
                EarthHumanApiContract.resolveBodyParts("torso", "LEFT_ARM", "torso", "unknown", null));
    }

    @Test
    void ignoresMissingBodyPartList() {
        assertEquals(List.of(), EarthHumanApiContract.resolveBodyParts((String[]) null));
    }

    @Test
    void normalizesExternalActivityWithoutAcceptingIdleOrInfiniteInput() {
        var input = EarthHumanApiContract.normalizeActivity("sprinting", 12.5D, 4.0D);
        assertEquals(com.xxsx.earthhuman.HumanActivityType.SPRINTING, input.activity());
        assertEquals(12.5D, input.distanceBlocks());
        assertEquals(4.0D, input.loadGain());
        assertEquals(null, EarthHumanApiContract.normalizeActivity("idle", 1.0D, 1.0D));
        assertEquals(null, EarthHumanApiContract.normalizeActivity("walking", Double.NaN, 1.0D));
    }
}
