package com.xxsx.earthhuman.api;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.HumanActivityType;

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

    public static ActivityInput normalizeActivity(String activityId, double distanceBlocks, double loadGain) {
        HumanActivityType activity = HumanActivityType.byId(activityId);
        if (activity == HumanActivityType.IDLE || activity == HumanActivityType.RESTING
                || activity == HumanActivityType.SLEEPING) {
            return null;
        }
        if (!Double.isFinite(distanceBlocks) || !Double.isFinite(loadGain)) {
            return null;
        }
        return new ActivityInput(activity,
                Math.max(0.0D, Math.min(distanceBlocks, 10_000.0D)),
                Math.max(0.0D, Math.min(loadGain, 50.0D)));
    }

    public record ActivityInput(HumanActivityType activity, double distanceBlocks, double loadGain) {
    }
}
