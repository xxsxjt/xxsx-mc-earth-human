package com.xxsx.earthhuman;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Server-authoritative entry point for deterministic core injuries. */
public final class HumanInjuryService {
    private static final String LAST_BARE_HAND_MINING_TICK =
            EarthHuman.MODID + ".movement.last_bare_hand_mining_tick";
    private static final String LAST_BARE_HAND_HINT_TICK =
            EarthHuman.MODID + ".movement.last_bare_hand_hint_tick";

    private HumanInjuryService() {
    }

    public static boolean isHardBlockRequiringTool(BlockState state, BlockGetter level, BlockPos pos) {
        if (state == null || level == null || pos == null) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 1.5F) {
            return false;
        }
        return state.requiresCorrectToolForDrops() || state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    @SuppressWarnings("deprecation")
    public static boolean isBareHandHardBlockMining(ServerPlayer player, BlockState state, BlockPos pos) {
        if (player == null || state == null || pos == null) {
            return false;
        }
        if (!isHardBlockRequiringTool(state, player.level(), pos)) {
            return false;
        }
        if (player.getMainHandItem().isEmpty()) {
            return true;
        }
        return !player.hasCorrectToolForDrops(state) && player.getDestroySpeed(state, pos) <= 1.05F;
    }

    public static boolean applyCoreDamage(ServerPlayer player, HumanBodyStatus body,
                                          DamageSource source, float finalDamage) {
        if (player == null || body == null || source == null
                || !Float.isFinite(finalDamage) || finalDamage <= 0.0F) {
            return false;
        }
        CoreInjuryPolicy.Cause cause = classifyCoreCause(player, source);
        double multiplier = EarthHumanConfig.bodyDamageMultiplier();
        if (cause == CoreInjuryPolicy.Cause.FALL) {
            HumanTrainingStatus training = HumanTrainingStatus.get(player);
            double fallProtection = (SomaticsLoreBridge.hasTendonAndBone(player, training) ? 0.65D : 1.0D)
                    * Math.max(0.40D, 1.0D - training.level(BodyPart.LIMBS) * 0.03D);
            multiplier *= fallProtection;
        }
        return applyOutcome(player, body, CoreInjuryPolicy.evaluate(
                cause, finalDamage, multiplier));
    }

    public static boolean applyBareHandHardMining(ServerPlayer player, HumanBodyStatus body) {
        if (player == null || body == null) {
            return false;
        }
        HumanTrainingStatus training = HumanTrainingStatus.get(player);
        if (SomaticsLoreBridge.hasMartialSomatics(player, training)) {
            return false;
        }
        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();
        if (data.getLongOr(LAST_BARE_HAND_MINING_TICK, Long.MIN_VALUE) == now) {
            return false;
        }
        data.putLong(LAST_BARE_HAND_MINING_TICK, now);
        double somaticsFactor = (SomaticsLoreBridge.hasTendonAndBone(player, training) ? 0.20D : 0.45D)
                * Math.max(0.15D, 1.0D - training.level(BodyPart.LIMBS) * 0.05D);
        boolean changed = applyOutcome(player, body, CoreInjuryPolicy.evaluate(
                CoreInjuryPolicy.Cause.BARE_HAND_HARD_MINING,
                0.0D,
                EarthHumanConfig.bodyDamageMultiplier() * somaticsFactor * 8.0D));
        if (now % 15L == 0L) {
            changed |= body.damage(BodyPart.LIMBS, 0.35D * somaticsFactor) > 0.0001D;
            body.save(player);
            HumanNetwork.syncBody(player, body);
        }
        if (changed && now - data.getLongOr(LAST_BARE_HAND_HINT_TICK, -1000L) >= 40L) {
            data.putLong(LAST_BARE_HAND_HINT_TICK, now);
            body.save(player);
            HumanNetwork.syncBody(player, body);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.45F, 1.15F);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        2, 0.20D, 0.15D, 0.20D, 0.02D);
            }
            player.sendOverlayMessage(Component.translatable("message.earth_human.injury.right_hand.bare_hand_hint"));
        }
        return changed;
    }

    public static boolean applyBareHandHardBlockBroken(ServerPlayer player, HumanBodyStatus body) {
        if (player == null || body == null) {
            return false;
        }
        HumanTrainingStatus training = HumanTrainingStatus.get(player);
        if (SomaticsLoreBridge.hasMartialSomatics(player, training)) {
            return false;
        }
        double somaticsFactor = (SomaticsLoreBridge.hasTendonAndBone(player, training) ? 0.40D : 1.0D)
                * Math.max(0.25D, 1.0D - training.level(BodyPart.LIMBS) * 0.04D);
        double before = body.rightHandInjury();
        boolean changed = body.damageRightHandInjury(0.18D * somaticsFactor) > 0.0001D;
        if (changed) {
            body.warnSpecificInjury(player, "right_hand", before, body.rightHandInjury());
        }
        changed |= body.damage(BodyPart.LIMBS, 1.4D * somaticsFactor) > 0.0001D;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.65F, 1.05F);
        if (player.level() instanceof ServerLevel serverLevel) {
            player.hurtServer(serverLevel, serverLevel.damageSources().generic(), 1.0F);
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    3, 0.25D, 0.20D, 0.25D, 0.03D);
        }
        player.sendOverlayMessage(Component.translatable("message.earth_human.injury.right_hand.bare_hand_hint"));
        return changed;
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
