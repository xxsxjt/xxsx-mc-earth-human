package com.xxsx.earthhuman;

import java.util.Locale;

public enum ExerciseAction {
    PUSH_UP("push_up", 3.2D, 16.0D, 4.5D, 40,
            new BodyPart[]{BodyPart.LIMBS, BodyPart.TORSO}),
    SQUAT("squat", 3.0D, 15.0D, 4.0D, 44,
            new BodyPart[]{BodyPart.LIMBS, BodyPart.TORSO}),
    SIT_UP("sit_up", 3.0D, 14.5D, 3.9D, 40,
            new BodyPart[]{BodyPart.TORSO}),
    PULL_UP("pull_up", 3.6D, 17.5D, 4.8D, 44,
            new BodyPart[]{BodyPart.LIMBS, BodyPart.TORSO}),
    CORE_TRAINING("core", 2.8D, 14.0D, 3.8D, 40,
            new BodyPart[]{BodyPart.TORSO}),
    STRETCH("stretch", 0.8D, -12.0D, -2.5D, 62,
            new BodyPart[]{BodyPart.LIMBS, BodyPart.TORSO}),
    DUMBBELL_CURL("dumbbell", 4.0D, 18.0D, 5.0D, 42,
            new BodyPart[]{BodyPart.LIMBS}),
    RESISTANCE_BAND_ROW("band", 3.6D, 17.0D, 4.6D, 48,
            new BodyPart[]{BodyPart.LIMBS, BodyPart.TORSO});

    public final String id;
    public final double xpGain;
    public final double loadGain;
    public final double fatigueGain;
    public final int durationTicks;
    public final BodyPart[] parts;

    ExerciseAction(String id, double xpGain, double loadGain, double fatigueGain, int durationTicks, BodyPart[] parts) {
        this.id = id;
        this.xpGain = xpGain;
        this.loadGain = loadGain;
        this.fatigueGain = fatigueGain;
        this.durationTicks = durationTicks;
        this.parts = parts;
    }

    public String translationKey() {
        return "exercise.earth_human." + id;
    }

    public static ExerciseAction byId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("pushup".equals(normalized) || "push-up".equals(normalized)) {
            return PUSH_UP;
        }
        if ("situp".equals(normalized) || "sit-up".equals(normalized)) {
            return SIT_UP;
        }
        if ("pullup".equals(normalized) || "pull-up".equals(normalized)) {
            return PULL_UP;
        }
        for (ExerciseAction action : values()) {
            if (action.id.equals(normalized)) {
                return action;
            }
        }
        return null;
    }
}
