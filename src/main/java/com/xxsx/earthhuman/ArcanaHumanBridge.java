package com.xxsx.earthhuman;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

public final class ArcanaHumanBridge {
    private static final String PREFIX = "earth_online_arcana.";
    private static final String CULTIVATION_LEVEL = PREFIX + "cultivation_level";
    private static final String MAGIC_RESEARCH_LEVEL = PREFIX + "magic_research_level";
    private static final String BIGU_LEVEL = PREFIX + "bigu_level";
    private static final String ARCANE_BODY_WARD_LEVEL = PREFIX + "arcane_body_ward_level";
    private static final String FASTING_FOOD_BONUS = PREFIX + "fasting_food_bonus";
    private static final String BREATH_CAPACITY_BONUS = PREFIX + "breath_capacity_bonus";
    private static final String ENDURANCE_BONUS = PREFIX + "endurance_bonus";
    private static final String BODY_TEMPERING_BONUS = PREFIX + "body_tempering_bonus";

    public final int cultivationLevel;
    public final int magicResearchLevel;
    public final int biguLevel;
    public final int arcaneBodyWardLevel;
    public final double fastingFoodBonus;
    public final double breathCapacityBonus;
    public final double enduranceBonus;
    public final double bodyTemperingBonus;

    private ArcanaHumanBridge(int cultivationLevel, int magicResearchLevel, int biguLevel,
                             int arcaneBodyWardLevel, double fastingFoodBonus,
                             double breathCapacityBonus, double enduranceBonus,
                             double bodyTemperingBonus) {
        this.cultivationLevel = cultivationLevel;
        this.magicResearchLevel = magicResearchLevel;
        this.biguLevel = biguLevel;
        this.arcaneBodyWardLevel = arcaneBodyWardLevel;
        this.fastingFoodBonus = fastingFoodBonus;
        this.breathCapacityBonus = breathCapacityBonus;
        this.enduranceBonus = enduranceBonus;
        this.bodyTemperingBonus = bodyTemperingBonus;
    }

    public static ArcanaHumanBridge read(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        return new ArcanaHumanBridge(
                Math.max(0, tag.getIntOr(CULTIVATION_LEVEL, 0)),
                Math.max(0, tag.getIntOr(MAGIC_RESEARCH_LEVEL, 0)),
                Math.max(0, tag.getIntOr(BIGU_LEVEL, 0)),
                Math.max(0, tag.getIntOr(ARCANE_BODY_WARD_LEVEL, 0)),
                positive(tag, FASTING_FOOD_BONUS),
                positive(tag, BREATH_CAPACITY_BONUS),
                positive(tag, ENDURANCE_BONUS),
                positive(tag, BODY_TEMPERING_BONUS));
    }

    public boolean hasAny() {
        return cultivationLevel > 0 || magicResearchLevel > 0 || biguLevel > 0
                || arcaneBodyWardLevel > 0 || fastingFoodBonus > 0.0D
                || breathCapacityBonus > 0.0D || enduranceBonus > 0.0D
                || bodyTemperingBonus > 0.0D;
    }

    public double hydrationLossMultiplier() {
        double reduction = 0.04D * cultivationLevel
                + 0.18D * biguLevel
                + 0.20D * fastingFoodBonus
                + 0.10D * bodyTemperingBonus;
        return clamp(1.0D - reduction, 0.30D, 1.0D);
    }

    public double fatigueGainMultiplier() {
        double reduction = 0.05D * cultivationLevel
                + 0.04D * magicResearchLevel
                + 0.22D * enduranceBonus
                + 0.16D * bodyTemperingBonus
                + 0.08D * arcaneBodyWardLevel;
        return clamp(1.0D - reduction, 0.35D, 1.0D);
    }

    public double recoveryMultiplier() {
        double bonus = 0.04D * cultivationLevel
                + 0.03D * magicResearchLevel
                + 0.25D * enduranceBonus;
        return clamp(1.0D + bonus, 1.0D, 1.75D);
    }

    public double temperatureStressMultiplier() {
        double reduction = 0.05D * cultivationLevel
                + 0.06D * magicResearchLevel
                + 0.22D * bodyTemperingBonus
                + 0.12D * arcaneBodyWardLevel;
        return clamp(1.0D - reduction, 0.45D, 1.0D);
    }

    public double foodExhaustionMultiplier() {
        double reduction = 0.08D * cultivationLevel
                + 0.30D * biguLevel
                + 0.35D * fastingFoodBonus
                + 0.12D * enduranceBonus;
        return clamp(1.0D - reduction, 0.15D, 1.0D);
    }

    public int extraAirSupply() {
        double air = 40.0D * cultivationLevel
                + 30.0D * magicResearchLevel
                + breathCapacityBonus;
        return (int) clamp(air, 0.0D, 1200.0D);
    }

    public double adaptationScore() {
        return cultivationLevel + magicResearchLevel + biguLevel + arcaneBodyWardLevel
                + fastingFoodBonus + enduranceBonus + bodyTemperingBonus
                + breathCapacityBonus / 120.0D;
    }

    public void applyPassiveBodySupport(ServerPlayer player) {
        if (!hasAny()) {
            return;
        }
        supportAir(player);
        supportFasting(player);
    }

    private void supportAir(ServerPlayer player) {
        int extraAir = extraAirSupply();
        if (extraAir <= 0 || !player.isUnderWater()) {
            return;
        }
        int max = player.getMaxAirSupply() + extraAir;
        int restoredPerSecond = 2 + Math.max(0, extraAir / 80);
        player.setAirSupply(Math.min(max, player.getAirSupply() + restoredPerSecond));
    }

    private void supportFasting(ServerPlayer player) {
        if (biguLevel <= 0 && fastingFoodBonus <= 0.0D) {
            return;
        }
        if (player.tickCount % 200 != 0) {
            return;
        }
        FoodData food = player.getFoodData();
        int floor = Math.min(19, 10 + biguLevel * 2 + (int) Math.floor(fastingFoodBonus * 4.0D));
        if (food.getFoodLevel() < floor) {
            food.setFoodLevel(food.getFoodLevel() + 1);
            food.setSaturation(Math.min(6.0F, food.getSaturationLevel() + 0.25F + 0.25F * biguLevel));
        }
    }

    private static double positive(CompoundTag tag, String key) {
        return Math.max(0.0D, tag.getDoubleOr(key, 0.0D));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
