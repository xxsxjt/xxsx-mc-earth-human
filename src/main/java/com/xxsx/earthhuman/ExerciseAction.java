package com.xxsx.earthhuman;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum ExerciseAction {
    PUSH_UP("push_up", 3.2D, 16.0D, 4.5D,
            new BodyPart[]{BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.TORSO}),
    SQUAT("squat", 3.0D, 15.0D, 4.0D,
            new BodyPart[]{BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG, BodyPart.TORSO}),
    CORE_TRAINING("core", 2.8D, 14.0D, 3.8D,
            new BodyPart[]{BodyPart.TORSO}),
    STRETCH("stretch", 0.8D, -12.0D, -2.5D,
            new BodyPart[]{BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG, BodyPart.TORSO}),
    DUMBBELL_CURL("dumbbell", 4.0D, 18.0D, 5.0D,
            new BodyPart[]{BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM}),
    RESISTANCE_BAND_ROW("band", 3.6D, 17.0D, 4.6D,
            new BodyPart[]{BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.TORSO});

    public final String id;
    public final double xpGain;
    public final double loadGain;
    public final double fatigueGain;
    public final BodyPart[] parts;

    ExerciseAction(String id, double xpGain, double loadGain, double fatigueGain, BodyPart[] parts) {
        this.id = id;
        this.xpGain = xpGain;
        this.loadGain = loadGain;
        this.fatigueGain = fatigueGain;
        this.parts = parts;
    }

    public Component displayName() {
        return Component.translatable("exercise.earth_human." + id);
    }

    public static ExerciseAction byId(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (ExerciseAction action : values()) {
            if (action.id.equals(normalized)) {
                return action;
            }
        }
        return null;
    }
}
