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
        assertEquals(List.of(BodyPart.TORSO, BodyPart.LEFT_ARM),
                EarthHumanApiContract.resolveBodyParts("torso", "LEFT_ARM", "torso", "unknown", null));
    }

    @Test
    void ignoresMissingBodyPartList() {
        assertEquals(List.of(), EarthHumanApiContract.resolveBodyParts((String[]) null));
    }
}
