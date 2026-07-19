package com.xxsx.earthhuman;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/** Server-authoritative entry point for deterministic core injuries. */
public final class HumanInjuryService {
    private static final String LAST_BARE_HAND_MINING_TICK =
            EarthHuman.MODID + ".movement.last_bare_hand_mining_tick";

    private HumanInjuryService() {
    }

    public static boolean applyCoreDamage(ServerPlayer player, HumanBodyStatus body,
                                          DamageSource source, float finalDamage) {
        if (player == null || body == null || source == null
                || !Float.isFinite(finalDamage) || finalDamage <= 0.0F) {
            return false;
        }
        CoreInjuryPolicy.Cause cause = classifyCoreCause(player, source);
        return applyOutcome(player, body, CoreInjuryPolicy.evaluate(
                cause, finalDamage, EarthHumanConfig.bodyDamageMultiplier()));
    }

    public static boolean applyBareHandHardMining(ServerPlayer player, HumanBodyStatus body) {
        if (player == null || body == null || !player.getMainHandItem().isEmpty()) {
            return false;
        }
        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();
        if (data.getLongOr(LAST_BARE_HAND_MINING_TICK, Long.MIN_VALUE) == now) {
            return false;
        }
        data.putLong(LAST_BARE_HAND_MINING_TICK, now);
        return applyOutcome(player, body, CoreInjuryPolicy.evaluate(
                CoreInjuryPolicy.Cause.BARE_HAND_HARD_MINING,
                0.0D,
                EarthHumanConfig.bodyDamageMultiplier()));
    }

    public static float rightHandMiningSpeedMultiplier(HumanBodyStatus body) {
        return body == null
                ? 1.0F
                : SpecificInjuryMath.rightHandMiningSpeedMultiplier(body.rightHandInjury());
    }

    private static CoreInjuryPolicy.Cause classifyCoreCause(ServerPlayer player, DamageSource source) {
        if (DamageProfile.classify(source) == DamageProfile.FALL) {
            return CoreInjuryPolicy.Cause.FALL;
        }
        if (source.is(DamageTypes.CACTUS) && player.getMainHandItem().isEmpty()) {
            return CoreInjuryPolicy.Cause.BARE_HAND_CACTUS;
        }
        return CoreInjuryPolicy.Cause.NONE;
    }

    private static boolean applyOutcome(ServerPlayer player, HumanBodyStatus body,
                                        CoreInjuryPolicy.Outcome outcome) {
        if (outcome == null || !outcome.changed()) {
            return false;
        }
        boolean changed = false;
        if (outcome.legSeverity() > 0.0D) {
            double before = body.legInjury();
            boolean injuryChanged = body.damageLegInjury(outcome.legSeverity()) > 0.0001D;
            if (injuryChanged) {
                body.warnSpecificInjury(player, "leg", before, body.legInjury());
                changed = true;
            }
        }
        if (outcome.rightHandSeverity() > 0.0D) {
            double before = body.rightHandInjury();
            boolean injuryChanged = body.damageRightHandInjury(outcome.rightHandSeverity()) > 0.0001D;
            if (injuryChanged) {
                body.warnSpecificInjury(player, "right_hand", before, body.rightHandInjury());
                changed = true;
            }
        }
        if (outcome.limbIntegrityDamage() > 0.0D) {
            changed |= body.damage(BodyPart.LIMBS, outcome.limbIntegrityDamage()) > 0.0001D;
        }
        return changed;
    }
}
