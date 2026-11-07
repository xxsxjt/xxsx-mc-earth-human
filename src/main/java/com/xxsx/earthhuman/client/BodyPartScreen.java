package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.ExerciseAction;
import net.minecraft.client.gui.screens.Screen;

/**
 * Dedicated Body Part & Exercise Switching Screen.
 * Opens directly to the Body Part diagnostics & exercise control tab (TAB_BODY),
 * providing clear status meters for all body parts and highlighted toggle buttons
 * for Push-up (俯卧撑), Squat (深蹲), Sit-up (仰卧起坐), Pull-up (引体向上), and Stop (停止锻炼).
 */
public class BodyPartScreen extends EarthHumanStatusScreen {
    public static final ExerciseAction[] PRIMARY_EXERCISES = {
            ExerciseAction.PUSH_UP,
            ExerciseAction.SQUAT,
            ExerciseAction.SIT_UP,
            ExerciseAction.PULL_UP
    };

    public BodyPartScreen(Screen parent) {
        super(parent, TAB_BODY);
    }
}
