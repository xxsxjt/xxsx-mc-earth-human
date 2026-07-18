package com.xxsx.earthhuman.api;

import com.xxsx.earthhuman.BodyPart;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure-Java contract helpers shared by runtime integration and unit tests. */
public final class EarthHumanApiContract {
    public static final int API_VERSION = 1;

    private EarthHumanApiContract() {
    }

    public static int apiVersion() {
        return API_VERSION;
    }

    static List<BodyPart> resolveBodyParts(String... bodyPartIds) {
        if (bodyPartIds == null || bodyPartIds.length == 0) {
            return List.of();
        }
        Set<BodyPart> parts = new LinkedHashSet<>();
        for (String id : bodyPartIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            BodyPart part = BodyPart.byId(id);
            if (part != null) {
                parts.add(part);
            }
        }
        return List.copyOf(parts);
    }
}
