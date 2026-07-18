package com.xxsx.earthhuman;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EarthHumanConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<String> GAMEPLAY_PRESET;
    private static final ModConfigSpec.ConfigValue<String> HEALTH_BRIDGE_MODE;

    private static final ModConfigSpec.BooleanValue ENABLE_BODY_HEALTH;
    private static final ModConfigSpec.BooleanValue ENABLE_BODY_EFFECTS;
    private static final ModConfigSpec.BooleanValue ENABLE_HYDRATION;
    private static final ModConfigSpec.BooleanValue ENABLE_TEMPERATURE;
    private static final ModConfigSpec.BooleanValue ENABLE_FATIGUE;
    private static final ModConfigSpec.BooleanValue ENABLE_TRAINING;
    private static final ModConfigSpec.BooleanValue ENABLE_AIR_QUALITY;

    private static final ModConfigSpec.DoubleValue HYDRATION_LOSS_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue FATIGUE_GAIN_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue RECOVERY_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue TEMPERATURE_RATE_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue BODY_DAMAGE_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue BODY_SPREAD_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue HEALING_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue TRAINING_GAIN_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue AIR_QUALITY_LOSS_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue AIR_QUALITY_RECOVERY_MULTIPLIER;

    private static final ModConfigSpec.DoubleValue RESPAWN_HEAD_MINIMUM_RATIO;
    private static final ModConfigSpec.DoubleValue RESPAWN_OTHER_MINIMUM_RATIO;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Core gameplay mode and compatibility behavior.").push("gameplay");
        GAMEPLAY_PRESET = builder
                .comment("Base tuning preset: casual, standard, realistic, or custom.")
                .define("preset", "standard", EarthHumanConfig::isPresetValue);
        HEALTH_BRIDGE_MODE = builder
                .comment("Health bridge: vanilla_compatible, head_bar, or overlay_only.",
                        "vanilla_compatible preserves vanilla/modded damage and healing as the authority.",
                        "head_bar routes non-head damage into body parts and is less compatible.",
                        "overlay_only tracks body state without applying body-part penalties.")
                .define("health_bridge_mode", "vanilla_compatible", EarthHumanConfig::isBridgeModeValue);
        builder.pop();

        builder.comment("Independent server-authoritative system switches.").push("systems");
        ENABLE_BODY_HEALTH = builder.define("body_health", true);
        ENABLE_BODY_EFFECTS = builder.define("body_functional_effects", true);
        ENABLE_HYDRATION = builder.define("hydration", true);
        ENABLE_TEMPERATURE = builder.define("temperature", true);
        ENABLE_FATIGUE = builder.define("fatigue", true);
        ENABLE_TRAINING = builder.define("training", true);
        ENABLE_AIR_QUALITY = builder.define("air_quality", true);
        builder.pop();

        builder.comment("Fine tuning. Preset multipliers are applied before these values.").push("multipliers");
        HYDRATION_LOSS_MULTIPLIER = range(builder, "hydration_loss", 1.0D);
        FATIGUE_GAIN_MULTIPLIER = range(builder, "fatigue_gain", 1.0D);
        RECOVERY_MULTIPLIER = range(builder, "recovery", 1.0D);
        TEMPERATURE_RATE_MULTIPLIER = range(builder, "temperature_rate", 1.0D);
        BODY_DAMAGE_MULTIPLIER = range(builder, "body_damage", 1.0D);
        BODY_SPREAD_MULTIPLIER = range(builder, "body_spread", 1.0D);
        HEALING_MULTIPLIER = range(builder, "healing", 1.0D);
        TRAINING_GAIN_MULTIPLIER = range(builder, "training_gain", 1.0D);
        AIR_QUALITY_LOSS_MULTIPLIER = range(builder, "air_quality_loss", 1.0D);
        AIR_QUALITY_RECOVERY_MULTIPLIER = range(builder, "air_quality_recovery", 1.0D);
        builder.pop();

        builder.comment("Minimum health restored on death without wiping long-term injuries.").push("respawn");
        RESPAWN_HEAD_MINIMUM_RATIO = builder.defineInRange("head_minimum_ratio", 0.35D, 0.0D, 1.0D);
        RESPAWN_OTHER_MINIMUM_RATIO = builder.defineInRange("other_parts_minimum_ratio", 0.15D, 0.0D, 1.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private EarthHumanConfig() {
    }

    public static EarthHumanRules.GameplayPreset preset() {
        return EarthHumanRules.GameplayPreset.byId(GAMEPLAY_PRESET.get());
    }

    public static EarthHumanRules.HealthBridgeMode healthBridgeMode() {
        return EarthHumanRules.HealthBridgeMode.byId(HEALTH_BRIDGE_MODE.get());
    }

    public static boolean bodyHealthEnabled() {
        return ENABLE_BODY_HEALTH.get();
    }

    public static boolean bodyEffectsEnabled() {
        return bodyHealthEnabled()
                && ENABLE_BODY_EFFECTS.get()
                && healthBridgeMode() != EarthHumanRules.HealthBridgeMode.OVERLAY_ONLY;
    }

    public static boolean hydrationEnabled() {
        return ENABLE_HYDRATION.get();
    }

    public static boolean temperatureEnabled() {
        return ENABLE_TEMPERATURE.get();
    }

    public static boolean fatigueEnabled() {
        return ENABLE_FATIGUE.get();
    }

    public static boolean trainingEnabled() {
        return ENABLE_TRAINING.get();
    }

    public static boolean airQualityEnabled() {
        return ENABLE_AIR_QUALITY.get();
    }

    public static double hydrationLossMultiplier() {
        return preset().hydrationLoss * HYDRATION_LOSS_MULTIPLIER.get();
    }

    public static double fatigueGainMultiplier() {
        return preset().fatigueGain * FATIGUE_GAIN_MULTIPLIER.get();
    }

    public static double recoveryMultiplier() {
        return preset().recovery * RECOVERY_MULTIPLIER.get();
    }

    public static double temperatureRateMultiplier() {
        return preset().temperatureRate * TEMPERATURE_RATE_MULTIPLIER.get();
    }

    public static double bodyDamageMultiplier() {
        return preset().bodyDamage * BODY_DAMAGE_MULTIPLIER.get();
    }

    public static double bodySpreadMultiplier() {
        return preset().bodySpread * BODY_SPREAD_MULTIPLIER.get();
    }

    public static double healingMultiplier() {
        return preset().healing * HEALING_MULTIPLIER.get();
    }

    public static double trainingGainMultiplier() {
        return preset().trainingGain * TRAINING_GAIN_MULTIPLIER.get();
    }

    public static double airQualityLossMultiplier() {
        return AIR_QUALITY_LOSS_MULTIPLIER.get();
    }

    public static double airQualityRecoveryMultiplier() {
        return AIR_QUALITY_RECOVERY_MULTIPLIER.get();
    }

    public static double respawnHeadMinimumRatio() {
        return RESPAWN_HEAD_MINIMUM_RATIO.get();
    }

    public static double respawnOtherMinimumRatio() {
        return RESPAWN_OTHER_MINIMUM_RATIO.get();
    }

    private static ModConfigSpec.DoubleValue range(ModConfigSpec.Builder builder, String key, double defaultValue) {
        return builder.defineInRange(key, defaultValue, 0.0D, 10.0D);
    }

    private static boolean isPresetValue(Object value) {
        return value instanceof String text && EarthHumanRules.GameplayPreset.isValid(text);
    }

    private static boolean isBridgeModeValue(Object value) {
        return value instanceof String text && EarthHumanRules.HealthBridgeMode.isValid(text);
    }
}
