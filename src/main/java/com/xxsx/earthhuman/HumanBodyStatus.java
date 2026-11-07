package com.xxsx.earthhuman;

import com.xxsx.earthhuman.api.BodyHealthBridgeApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class HumanBodyStatus {
    private static final String PREFIX = EarthHuman.MODID + ".body.";
    private static final String INIT = PREFIX + "initialized";
    private static final String INIT_MODEL = PREFIX + "init_model";
    private static final String LAST_WARN_TIME = PREFIX + "last_warning_game_time";
    private static final String LAST_USE_SPREAD_TIME = PREFIX + "last_use_spread_game_time";
    private static final String LAST_VANILLA_MAX_HEALTH = PREFIX + "last_vanilla_max_health";
    private static final String LEG_INJURY = PREFIX + "injury.leg";
    private static final String RIGHT_HAND_INJURY = PREFIX + "injury.right_hand";
    private static final int CURRENT_INIT_MODEL = 5;
    private static final double BASE_VANILLA_HEALTH = 20.0D;
    private static final net.minecraft.resources.Identifier HEAD_HEALTH_LIMIT_ID =
            EarthHuman.id("head_health_limit");
    private static final net.minecraft.resources.Identifier FRACTURE_HEALTH_PENALTY_ID =
            EarthHuman.id("fracture_max_health_penalty");
    private static final net.minecraft.resources.Identifier SOMATICS_HEALTH_BONUS_ID =
            EarthHuman.id("somatics_max_health_bonus");
    private static final net.minecraft.resources.Identifier SOMATICS_ATTACK_BONUS_ID =
            EarthHuman.id("somatics_attack_damage_bonus");
    private static final net.minecraft.resources.Identifier SOMATICS_SPEED_BONUS_ID =
            EarthHuman.id("somatics_movement_speed_bonus");
    private static final net.minecraft.resources.Identifier SOMATICS_SAFE_FALL_BONUS_ID =
            EarthHuman.id("somatics_safe_fall_bonus");
    private static final net.minecraft.resources.Identifier SOMATICS_KNOCKBACK_BONUS_ID =
            EarthHuman.id("somatics_knockback_resistance_bonus");

    private final double[] health = new double[BodyPart.values().length];
    private double vanillaMaxHealth = BASE_VANILLA_HEALTH;
    private double legInjury;
    private double rightHandInjury;
    public long lastWarningGameTime;
    public long lastUseSpreadGameTime;

    private HumanBodyStatus() {
        reset();
    }

    public static HumanBodyStatus get(Player player) {
        CompoundTag data = player.getPersistentData();
        HumanBodyStatus status = new HumanBodyStatus();
        if (data.getIntOr(INIT, 0) == 0) {
            status.initializeFromVanillaHealth(player);
            status.save(player);
            return status;
        }

        int model = data.getIntOr(INIT_MODEL, 1);
        status.vanillaMaxHealth = Math.max(1.0D, data.getDoubleOr(LAST_VANILLA_MAX_HEALTH, BASE_VANILLA_HEALTH));
        for (BodyPart part : BodyPart.values()) {
            status.health[part.ordinal()] = model < CURRENT_INIT_MODEL
                    ? status.migrateHealth(data, part, model)
                    : HumanStatus.clamp(status.readCurrentHealth(data, part), 0.0D, status.maxHealth(part));
        }
        status.syncCapacityFromVanillaMax(player);
        status.legInjury = HumanStatus.clamp(data.getDoubleOr(LEG_INJURY, 0.0D), 0.0D, 1.0D);
        status.rightHandInjury = HumanStatus.clamp(data.getDoubleOr(RIGHT_HAND_INJURY, 0.0D), 0.0D, 1.0D);
        if (model < CURRENT_INIT_MODEL) {
            status.save(player);
        }
        status.lastWarningGameTime = data.getLongOr(LAST_WARN_TIME, -1_000_000L);
        status.lastUseSpreadGameTime = data.getLongOr(LAST_USE_SPREAD_TIME, -1_000_000L);
        return status;
    }

    public void save(Player player) {
        syncCapacityFromVanillaMax(player);
        CompoundTag data = player.getPersistentData();
        data.putInt(INIT, 1);
        data.putInt(INIT_MODEL, CURRENT_INIT_MODEL);
        data.putDouble(LAST_VANILLA_MAX_HEALTH, vanillaMaxHealth);
        data.putDouble(LEG_INJURY, HumanStatus.clamp(legInjury, 0.0D, 1.0D));
        data.putDouble(RIGHT_HAND_INJURY, HumanStatus.clamp(rightHandInjury, 0.0D, 1.0D));
        for (BodyPart part : BodyPart.values()) {
            data.putDouble(healthKey(part), HumanStatus.clamp(health(part), 0.0D, maxHealth(part)));
        }
        data.putLong(LAST_WARN_TIME, lastWarningGameTime);
        data.putLong(LAST_USE_SPREAD_TIME, lastUseSpreadGameTime);
    }

    public void copyTo(Player player) {
        save(player);
    }

    public void reset() {
        vanillaMaxHealth = BASE_VANILLA_HEALTH;
        for (BodyPart part : BodyPart.values()) {
            health[part.ordinal()] = baseMaxHealth(part);
        }
        lastWarningGameTime = -1_000_000L;
        lastUseSpreadGameTime = -1_000_000L;
        legInjury = 0.0D;
        rightHandInjury = 0.0D;
    }

    public void stabilizeAfterDeathRespawn(Player player) {
        syncCapacityFromVanillaMax(player);
        for (BodyPart part : BodyPart.values()) {
            double minimumRatio = part == BodyPart.HEAD
                    ? EarthHumanConfig.respawnHeadMinimumRatio()
                    : EarthHumanConfig.respawnOtherMinimumRatio();
            double minimum = maxHealth(part) * minimumRatio;
            if (health(part) < minimum) {
                setHealth(part, minimum);
            }
        }
    }

    private void initializeFromVanillaHealth(Player player) {
        vanillaMaxHealth = Math.max(1.0D, player.getMaxHealth());
        double ratio = HumanStatus.clamp(player.getHealth() / vanillaMaxHealth, 0.0D, 1.0D);
        for (BodyPart part : BodyPart.values()) {
            health[part.ordinal()] = maxHealth(part) * ratio;
        }
        lastWarningGameTime = -1_000_000L;
        lastUseSpreadGameTime = -1_000_000L;
    }

    private double migrateHealth(CompoundTag data, BodyPart part, int oldModel) {
        double oldMax = oldMaxHealth(part, oldModel);
        double oldValue = part == BodyPart.LIMBS
                ? oldStoredLimbHealth(data, oldModel)
                : data.getDoubleOr(healthKey(part), oldMax);
        return BodyHealthMath.migratePreservingRatio(oldValue, oldMax, maxHealth(part));
    }

    private double oldMaxHealth(BodyPart part, int oldModel) {
        if (oldModel <= 2) {
            return switch (part) {
                case HEAD -> 10.0D;
                case TORSO -> 16.0D;
                case LIMBS -> 44.0D;
            };
        }
        return part == BodyPart.LIMBS ? 8.0D : part.maxHealth;
    }

    private double readCurrentHealth(CompoundTag data, BodyPart part) {
        double fallback = maxHealth(part);
        if (part != BodyPart.LIMBS) {
            return data.getDoubleOr(healthKey(part), fallback);
        }
        if (data.contains(healthKey(part))) {
            return data.getDoubleOr(healthKey(part), fallback);
        }
        return oldStoredLimbHealth(data, CURRENT_INIT_MODEL);
    }

    private static double oldStoredLimbHealth(CompoundTag data, int oldModel) {
        double armMax = oldModel <= 2 ? 10.0D : 2.0D;
        double legMax = oldModel <= 2 ? 12.0D : 2.0D;
        double leftArm = data.getDoubleOr(PREFIX + "left_arm.health", armMax);
        double rightArm = data.getDoubleOr(PREFIX + "right_arm.health", armMax);
        double leftLeg = data.getDoubleOr(PREFIX + "left_leg.health", legMax);
        double rightLeg = data.getDoubleOr(PREFIX + "right_leg.health", legMax);
        return leftArm + rightArm + leftLeg + rightLeg;
    }

    public double health(BodyPart part) {
        return health[part.ordinal()];
    }

    public double maxHealth(BodyPart part) {
        return baseMaxHealth(part) * vanillaMaxScale();
    }

    public double percent(BodyPart part) {
        return health(part) / maxHealth(part);
    }

    /** 0 means no injury; 1 means a serious leg injury. */
    public double legInjury() {
        return legInjury;
    }

    /** 0 means no injury; 1 means a serious right-hand injury. */
    public double rightHandInjury() {
        return rightHandInjury;
    }

    public boolean hasSpecificInjury() {
        return legInjury > 0.001D || rightHandInjury > 0.001D;
    }

    public boolean isLegFractured() {
        return SpecificInjuryMath.isFractured(legInjury);
    }

    public boolean isRightHandFractured() {
        return SpecificInjuryMath.isFractured(rightHandInjury);
    }

    public boolean hasFracture() {
        return isLegFractured() || isRightHandFractured();
    }

    public double damageLegInjury(double amount) {
        double before = legInjury;
        legInjury = SpecificInjuryMath.addSeverity(before, amount);
        return legInjury - before;
    }

    public double damageRightHandInjury(double amount) {
        double before = rightHandInjury;
        rightHandInjury = SpecificInjuryMath.addSeverity(before, amount);
        return rightHandInjury - before;
    }

    public boolean recoverSpecificInjuries(double amount) {
        double recovery = Math.max(0.0D, amount);
        double beforeLeg = legInjury;
        double beforeHand = rightHandInjury;
        legInjury = SpecificInjuryMath.recoverSeverity(legInjury, recovery);
        rightHandInjury = SpecificInjuryMath.recoverSeverity(rightHandInjury, recovery);
        return Math.abs(beforeLeg - legInjury) > 0.0001D
                || Math.abs(beforeHand - rightHandInjury) > 0.0001D;
    }

    public boolean recoverNaturalInjuries(double amount) {
        double recovery = Math.max(0.0D, amount);
        double beforeLeg = legInjury;
        double beforeHand = rightHandInjury;
        legInjury = SpecificInjuryMath.recoverNaturalSeverity(legInjury, recovery);
        rightHandInjury = SpecificInjuryMath.recoverNaturalSeverity(rightHandInjury, recovery);
        return Math.abs(beforeLeg - legInjury) > 0.0001D
                || Math.abs(beforeHand - rightHandInjury) > 0.0001D;
    }

    public boolean recoverNaturalBodyAndInjuries(ServerPlayer player, double partHealAmount, double injuryRecoveryAmount) {
        syncCapacityFromVanillaMax(player);
        boolean changed = recoverNaturalInjuries(injuryRecoveryAmount);
        double heal = Math.max(0.0D, partHealAmount);
        if (heal > 0.0001D) {
            for (BodyPart part : BodyPart.values()) {
                double capRatio = (part == BodyPart.LIMBS && hasFracture()) ? 0.65D : 1.0D;
                double cap = maxHealth(part) * capRatio;
                double before = health(part);
                if (before < cap - 0.001D) {
                    setHealth(part, Math.min(cap, before + heal));
                    changed |= health(part) > before + 0.001D;
                }
            }
        }
        if (changed) {
            syncVanillaFromHead(player);
        }
        return changed;
    }

    public boolean treatFracture(ServerPlayer player, double severityReduction, double partHealAmount) {
        syncCapacityFromVanillaMax(player);
        boolean hadFracture = hasFracture();
        boolean changed = recoverSpecificInjuries(severityReduction);
        if (partHealAmount > 0.01D) {
            BodyPart worst = worstPart(true);
            double before = health(worst);
            setHealth(worst, before + partHealAmount);
            changed |= health(worst) > before + 0.01D;
            double beforeLimbs = health(BodyPart.LIMBS);
            setHealth(BodyPart.LIMBS, beforeLimbs + partHealAmount * 0.6D);
            changed |= health(BodyPart.LIMBS) > beforeLimbs + 0.01D;
        }
        applyFunctionalEffects(player);
        syncVanillaFromHead(player);
        if (hadFracture && !hasFracture()) {
            player.sendSystemMessage(Component.translatable("message.earth_human.injury.fracture_set"));
        }
        return changed;
    }

    public void setHealth(BodyPart part, double value) {
        health[part.ordinal()] = HumanStatus.clamp(value, 0.0D, maxHealth(part));
    }

    public double damage(BodyPart part, double amount) {
        double applied = Math.max(0.0D, amount);
        double before = health(part);
        setHealth(part, before - applied);
        return before - health(part);
    }

    public BodyPart worstPart(boolean includeHead) {
        BodyPart worst = null;
        double worstRatio = Double.MAX_VALUE;
        for (BodyPart part : BodyPart.values()) {
            if (!includeHead && part == BodyPart.HEAD) {
                continue;
            }
            double ratio = percent(part);
            if (ratio < worstRatio) {
                worstRatio = ratio;
                worst = part;
            }
        }
        return worst == null ? BodyPart.HEAD : worst;
    }

    public boolean healWorst(ServerPlayer player, double amount, boolean includeHead) {
        syncCapacityFromVanillaMax(player);
        BodyPart part = worstPart(includeHead);
        if (!includeHead && health(part) >= maxHealth(part)) {
            return recoverSpecificInjuries(Math.max(0.0D, amount) / 4.0D);
        }
        return healPart(player, part, amount);
    }

    public boolean healPart(ServerPlayer player, BodyPart part, double amount) {
        syncCapacityFromVanillaMax(player);
        double before = health(part);
        setHealth(part, before + Math.max(0.0D, amount));
        boolean injuryChanged = false;
        if (part == BodyPart.LIMBS) {
            injuryChanged = recoverSpecificInjuries(
                    Math.max(0.0D, amount) / Math.max(1.0D, maxHealth(part)));
        }
        if (part == BodyPart.HEAD) {
            syncVanillaFromHead(player);
        }
        return health(part) > before + 0.01D || injuryChanged;
    }

    public void ensureHeadHealthLimit(ServerPlayer player) {
        if (!BodyHealthBridgeApi.active()) {
            return;
        }
        syncCapacityFromVanillaMax(player);
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getModifier(HEAD_HEALTH_LIMIT_ID) != null) {
            maxHealth.removeModifier(HEAD_HEALTH_LIMIT_ID);
        }
        if (BodyHealthBridgeApi.mode() == BodyHealthBridgeApi.Mode.HEAD_BAR) {
            syncVanillaFromHead(player);
        }
    }

    public void pullHeadFromVanilla(ServerPlayer player) {
        syncCapacityFromVanillaMax(player);
        double max = Math.max(1.0D, player.getMaxHealth());
        double ratio = HumanStatus.clamp(player.getHealth() / max, 0.0D, 1.0D);
        setHealth(BodyPart.HEAD, maxHealth(BodyPart.HEAD) * ratio);
    }

    public void pullHeadDamageFromVanilla(ServerPlayer player) {
        syncCapacityFromVanillaMax(player);
        double max = Math.max(1.0D, player.getMaxHealth());
        double ratio = HumanStatus.clamp(player.getHealth() / max, 0.0D, 1.0D);
        double reflected = maxHealth(BodyPart.HEAD) * ratio;
        if (reflected < health(BodyPart.HEAD)) {
            setHealth(BodyPart.HEAD, reflected);
        }
    }

    public boolean bridgeVanillaHealing(ServerPlayer player, double vanillaHealAmount) {
        syncCapacityFromVanillaMax(player);
        double remaining = Math.max(0.0D, vanillaHealAmount) * EarthHumanConfig.healingMultiplier();
        if (!Double.isFinite(remaining) || remaining <= 0.01D) {
            return false;
        }
        boolean changed = false;
        double headBodyPerVanilla = maxHealth(BodyPart.HEAD) / Math.max(1.0D, player.getMaxHealth());
        double headMissing = Math.max(0.0D, maxHealth(BodyPart.HEAD) - health(BodyPart.HEAD));
        if (headMissing > 0.0D && headBodyPerVanilla > 0.0D) {
            double vanillaUsed = Math.min(remaining, headMissing / headBodyPerVanilla);
            setHealth(BodyPart.HEAD, health(BodyPart.HEAD) + vanillaUsed * headBodyPerVanilla);
            remaining -= vanillaUsed;
            changed = vanillaUsed > 0.01D;
        }
        for (BodyPart part : HEALING_SPREAD_ORDER) {
            if (remaining <= 0.01D) {
                break;
            }
            double before = remaining;
            remaining = healWithOverflow(part, remaining);
            changed |= remaining < before - 0.01D;
        }
        changed |= recoverNaturalInjuries(Math.max(0.0D, vanillaHealAmount) * 0.14D * EarthHumanConfig.healingMultiplier());
        if (changed) {
            syncVanillaFromHead(player);
        }
        return changed;
    }

    public void syncVanillaFromHead(ServerPlayer player) {
        if (!BodyHealthBridgeApi.active()
                || BodyHealthBridgeApi.mode() != BodyHealthBridgeApi.Mode.HEAD_BAR) {
            return;
        }
        double ratio = HumanStatus.clamp(percent(BodyPart.HEAD), 0.0D, 1.0D);
        float target = (float) HumanStatus.clamp(player.getMaxHealth() * ratio, 0.0D, player.getMaxHealth());
        if (Math.abs(player.getHealth() - target) > 0.05F) {
            player.setHealth(target);
        }
    }

    public boolean spreadFromUse(ServerPlayer player, BodyPart primary, BodyPart secondary, double baseAmount) {
        if (player.level().getGameTime() - lastUseSpreadGameTime < 20L) {
            return false;
        }
        BodyPart part = percent(primary) <= percent(secondary) ? primary : secondary;
        return spreadFromUse(player, part, baseAmount);
    }

    public boolean spreadFromUse(ServerPlayer player, double baseAmount, BodyPart... parts) {
        if (parts == null || parts.length == 0 || player.level().getGameTime() - lastUseSpreadGameTime < 20L) {
            return false;
        }
        BodyPart selected = parts[0];
        for (int i = 1; i < parts.length; i++) {
            if (percent(parts[i]) < percent(selected)) {
                selected = parts[i];
            }
        }
        return spreadFromUse(player, selected, baseAmount);
    }

    public boolean spreadFromUse(ServerPlayer player, BodyPart part, double baseAmount) {
        if (player.level().getGameTime() - lastUseSpreadGameTime < 20L) {
            return false;
        }
        double severity = useSeverity(part);
        if (severity <= 0.0D) {
            return false;
        }
        lastUseSpreadGameTime = player.level().getGameTime();
        double spreadMultiplier = EarthHumanConfig.bodySpreadMultiplier();
        boolean changed = spreadToAdjacent(player, part, Math.max(0.0D, baseAmount) * severity * spreadMultiplier, true);
        if (part != BodyPart.TORSO && useSeverity(BodyPart.TORSO) > 0.0D) {
            changed |= spreadToAdjacent(player, BodyPart.TORSO,
                    Math.max(0.0D, baseAmount) * 0.25D * useSeverity(BodyPart.TORSO) * spreadMultiplier, true);
        }
        if (changed) {
            syncVanillaFromHead(player);
        }
        return changed;
    }

    public boolean spreadFromImpact(ServerPlayer player, BodyPart part, double incomingDamage, double beforeHealth) {
        if (part == BodyPart.HEAD || incomingDamage <= 0.0D) {
            return false;
        }
        double afterHealth = health(part);
        double overkill = Math.max(0.0D, incomingDamage - beforeHealth);
        double amount;
        if (beforeHealth <= 0.01D) {
            amount = incomingDamage * 0.45D;
        } else if (afterHealth <= 0.01D) {
            amount = overkill * 0.55D + incomingDamage * 0.10D;
        } else if (percent(part) < 0.30D) {
            amount = incomingDamage * 0.06D;
        } else {
            amount = incomingDamage * 0.025D;
        }
        boolean changed = spreadToAdjacent(player, part, amount * EarthHumanConfig.bodySpreadMultiplier(), false);
        if (changed) {
            syncVanillaFromHead(player);
        }
        return changed;
    }

    public void applyFunctionalEffects(ServerPlayer player) {
        updateMaxHealthModifiers(player);
        if (!EarthHumanConfig.bodyEffectsEnabled()) {
            return;
        }
        if (isLegFractured()) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 1, true, false, true));
        } else if (legInjury >= 0.30D) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 0, true, false, true));
        }
        if (health(BodyPart.TORSO) <= 0.01D || percent(BodyPart.TORSO) < 0.25D) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, health(BodyPart.TORSO) <= 0.01D ? 1 : 0, true, false, true));
            player.causeFoodExhaustion(0.05F);
        }
    }

    public double fractureMaxHealthPenalty() {
        double penalty = 0.0D;
        if (isLegFractured()) {
            penalty -= 4.0D;
        }
        if (isRightHandFractured()) {
            penalty -= 4.0D;
        }
        return penalty;
    }

    public static double somaticsMaxHealthBonus(ServerPlayer player) {
        if (player == null || !EarthHumanConfig.trainingEnabled()) {
            return 0.0D;
        }
        HumanTrainingStatus training = HumanTrainingStatus.get(player);
        double mult = SomaticsLoreBridge.somaticMultiplier(player, training);
        // 1.0x -> +0 HP (20 total); 5.0x -> +80 HP (100 total, 50 hearts = 5x peak human)
        return Math.min(80.0D, Math.round((mult - 1.0D) * 10.0D) * 2.0D);
    }

    private void updateMaxHealthModifiers(ServerPlayer player) {
        var maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }
        double penalty = EarthHumanConfig.bodyEffectsEnabled() ? fractureMaxHealthPenalty() : 0.0D;
        double bonus = somaticsMaxHealthBonus(player);
        setTransientModifier(maxHealthAttr, FRACTURE_HEALTH_PENALTY_ID, penalty);
        setTransientModifier(maxHealthAttr, SOMATICS_HEALTH_BONUS_ID, bonus);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        if (EarthHumanConfig.trainingEnabled()) {
            HumanTrainingStatus training = HumanTrainingStatus.get(player);
            double mult = SomaticsLoreBridge.somaticMultiplier(player, training);
            double progress = Math.max(0.0D, (mult - 1.0D) / 4.0D); // 0.0 .. 1.0
            var attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttr != null) {
                setTransientModifier(attackAttr, SOMATICS_ATTACK_BONUS_ID, progress * 7.0D);
            }
            var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                setTransientModifier(speedAttr, SOMATICS_SPEED_BONUS_ID, progress * 0.055D);
            }
            var fallAttr = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);
            if (fallAttr != null) {
                setTransientModifier(fallAttr, SOMATICS_SAFE_FALL_BONUS_ID, progress * 12.0D);
            }
            var kbAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kbAttr != null) {
                setTransientModifier(kbAttr, SOMATICS_KNOCKBACK_BONUS_ID, progress * 0.80D);
            }
        }
    }

    private static void setTransientModifier(net.minecraft.world.entity.ai.attributes.AttributeInstance instance,
                                             net.minecraft.resources.Identifier id,
                                             double amount) {
        var existing = instance.getModifier(id);
        if (Math.abs(amount) < 0.01D) {
            if (existing != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (existing != null && Math.abs(existing.amount() - amount) < 0.01D) {
            return;
        }
        if (existing != null) {
            instance.removeModifier(id);
        }
        instance.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                id, amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
    }

    public void warnDirectDamage(ServerPlayer player, BodyPart part, double before) {
        if (player.level().getGameTime() - lastWarningGameTime < 80L) {
            return;
        }
        double after = health(part);
        if (after <= 0.01D && before > 0.01D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.body.failed", Component.translatable(part.translationKey())));
            lastWarningGameTime = player.level().getGameTime();
        } else if (after / maxHealth(part) < 0.30D && before / maxHealth(part) >= 0.30D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.body.critical", Component.translatable(part.translationKey())));
            lastWarningGameTime = player.level().getGameTime();
        }
    }

    public void warnSpecificInjury(ServerPlayer player, String id, double before, double after) {
        if (player.level().getGameTime() - lastWarningGameTime < 80L) {
            return;
        }
        String key = null;
        if (after >= SpecificInjuryMath.FRACTURE_THRESHOLD && before < SpecificInjuryMath.FRACTURE_THRESHOLD) {
            key = "message.earth_human.injury." + id + ".serious";
        } else if (after >= 0.30D && before < 0.30D) {
            key = "message.earth_human.injury." + id + ".started";
        }
        if (key != null) {
            player.sendSystemMessage(Component.translatable(key));
            lastWarningGameTime = player.level().getGameTime();
        }
    }

    public Component describeLine(BodyPart part) {
        double value = health(part);
        return Component.translatable(
                "message.earth_human.body.line",
                Component.translatable(part.translationKey()),
                String.format(java.util.Locale.ROOT, "%.1f", value),
                String.format(java.util.Locale.ROOT, "%.1f", maxHealth(part)),
                stateText(part));
    }

    public Component describeSpecificInjury(String id, double severity) {
        return Component.translatable(
                "message.earth_human.body.injury.line",
                Component.translatable("bodyinjury.earth_human." + id),
                Math.round(HumanStatus.clamp(1.0D - severity, 0.0D, 1.0D) * 100.0D),
                injuryStateText(severity));
    }

    private Component injuryStateText(double severity) {
        if (SpecificInjuryMath.isFractured(severity)) {
            return Component.translatable("message.earth_human.body.state.critical");
        }
        if (severity >= 0.30D) {
            return Component.translatable("message.earth_human.body.state.injured");
        }
        return Component.translatable("message.earth_human.body.state.ok");
    }

    private Component stateText(BodyPart part) {
        double value = health(part);
        if (value <= 0.01D) {
            return Component.translatable("message.earth_human.body.state.failed");
        }
        if (percent(part) < 0.30D) {
            return Component.translatable("message.earth_human.body.state.critical");
        }
        if (percent(part) < 0.70D) {
            return Component.translatable("message.earth_human.body.state.injured");
        }
        return Component.translatable("message.earth_human.body.state.ok");
    }

    private void warnSpread(ServerPlayer player, BodyPart from, BodyPart to, boolean fromUse) {
        if (player.level().getGameTime() - lastWarningGameTime < 120L) {
            return;
        }
        player.sendSystemMessage(Component.translatable(
                fromUse ? "message.earth_human.body.spread.use" : "message.earth_human.body.spread.impact",
                Component.translatable(from.translationKey()),
                Component.translatable(to.translationKey())));
        lastWarningGameTime = player.level().getGameTime();
    }

    private boolean spreadToAdjacent(ServerPlayer player, BodyPart from, double totalAmount, boolean fromUse) {
        if (totalAmount <= 0.01D) {
            return false;
        }
        Set<BodyPart> visited = new HashSet<>();
        visited.add(from);
        return spreadToAdjacent(player, from, totalAmount, fromUse, visited, 0);
    }

    private boolean spreadToAdjacent(ServerPlayer player, BodyPart from, double totalAmount, boolean fromUse,
                                     Set<BodyPart> visited, int depth) {
        if (totalAmount <= 0.01D || depth > 2) {
            return false;
        }
        BodyPart[] adjacent = adjacentParts(from);
        double amount = totalAmount / adjacent.length;
        boolean changed = false;
        for (BodyPart to : adjacent) {
            if (!visited.add(to)) {
                continue;
            }
            double adjusted = to == BodyPart.HEAD ? amount * 0.70D : amount;
            if (health(to) <= 0.01D) {
                changed |= spreadToAdjacent(player, to, adjusted * 0.55D, fromUse, visited, depth + 1);
                continue;
            }
            if (damage(to, adjusted) > 0.01D) {
                changed = true;
                warnSpread(player, from, to, fromUse);
                if (health(to) <= 0.01D) {
                    changed |= spreadToAdjacent(player, to, adjusted * 0.35D, fromUse, visited, depth + 1);
                }
            }
        }
        return changed;
    }

    private double useSeverity(BodyPart part) {
        double ratio = percent(part);
        if (health(part) <= 0.01D) {
            return 1.0D;
        }
        if (ratio < 0.30D) {
            return 0.35D;
        }
        return 0.0D;
    }

    private static BodyPart[] adjacentParts(BodyPart part) {
        return switch (part) {
            case HEAD -> new BodyPart[]{BodyPart.TORSO};
            case TORSO -> new BodyPart[]{BodyPart.HEAD, BodyPart.LIMBS};
            case LIMBS -> new BodyPart[]{BodyPart.TORSO};
        };
    }

    private void syncCapacityFromVanillaMax(Player player) {
        double rawMax = player.getMaxHealth();
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            var fractureMod = attr.getModifier(FRACTURE_HEALTH_PENALTY_ID);
            if (fractureMod != null) {
                rawMax -= fractureMod.amount();
            }
        }
        double currentMax = Math.max(1.0D, rawMax);
        if (Math.abs(currentMax - vanillaMaxHealth) <= 0.01D) {
            return;
        }
        double oldScale = vanillaMaxScale();
        vanillaMaxHealth = currentMax;
        double newScale = vanillaMaxScale();
        for (BodyPart part : BodyPart.values()) {
            double oldMaximum = baseMaxHealth(part) * oldScale;
            double newMaximum = baseMaxHealth(part) * newScale;
            health[part.ordinal()] = BodyHealthMath.rescalePreservingRatio(health(part), oldMaximum, newMaximum);
        }
    }

    private double healWithOverflow(BodyPart part, double amount) {
        BodyHealthMath.HealingStep step = BodyHealthMath.applyHealing(health(part), maxHealth(part), amount);
        setHealth(part, step.health());
        return step.remaining();
    }

    private double vanillaMaxScale() {
        return Math.max(1.0D, vanillaMaxHealth) / BASE_VANILLA_HEALTH;
    }

    private static double baseMaxHealth(BodyPart part) {
        return part.maxHealth;
    }

    private static String healthKey(BodyPart part) {
        return PREFIX + part.id + ".health";
    }

    private static final BodyPart[] HEALING_SPREAD_ORDER = {
            BodyPart.TORSO,
            BodyPart.LIMBS
    };

    @Override
    public String toString() {
        return "HumanBodyStatus" + Arrays.toString(health);
    }
}
