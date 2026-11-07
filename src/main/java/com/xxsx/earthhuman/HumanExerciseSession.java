package com.xxsx.earthhuman;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * Server-owned exercise session started from a placed training station. The player has to stay at
 * the station until it finishes; the reward is applied through HumanExercise on completion.
 */
public final class HumanExerciseSession {
    private static final String PREFIX = EarthHuman.MODID + ".exercise.";
    private static final String ACTIVE_ACTION = PREFIX + "action";
    private static final String STATION_POS = PREFIX + "station";
    private static final String HAS_STATION = PREFIX + "has_station";
    private static final String FINISH_TICK = PREFIX + "finish";
    private static final int COOLDOWN_TICKS = 32;
    private static final int MAX_DISTANCE_SQUARED = 6 * 6;

    private HumanExerciseSession() {
    }

    public static boolean start(ServerPlayer player, ExerciseAction action, BlockPos pos) {
        if (!EarthHumanConfig.trainingEnabled()) {
            player.sendSystemMessage(Component.translatable("message.earth_human.training.disabled"));
            return false;
        }
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        if (data.contains(ACTIVE_ACTION)) {
            ExerciseAction current = ExerciseAction.byId(data.getString(ACTIVE_ACTION).orElse(""));
            if (current == action) {
                cancel(player);
                return true;
            }
            if (pos == null) {
                clear(player);
            } else {
                player.sendSystemMessage(Component.translatable("message.earth_human.training.station_busy"));
                return false;
            }
        }
        HumanTrainingStatus training = HumanTrainingStatus.get(player);
        if (!training.canManualExercise(player, COOLDOWN_TICKS)) {
            player.sendSystemMessage(Component.translatable("message.earth_human.training.too_fast"));
            return false;
        }
        data.putString(ACTIVE_ACTION, action.id);
        data.putBoolean(HAS_STATION, pos != null);
        data.putLong(STATION_POS, (pos != null ? pos : player.blockPosition()).asLong());
        data.putLong(FINISH_TICK, player.level().getGameTime() + action.durationTicks);
        player.swing(InteractionHand.MAIN_HAND, true);
        sync(player, action, action.durationTicks);
        player.sendSystemMessage(Component.translatable(
                "message.earth_human.training.station_started",
                Component.translatable(action.translationKey()),
                String.format(Locale.ROOT, "%.1f", action.durationTicks / 20.0D)));
        return true;
    }

    public static void tick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ACTIVE_ACTION)) {
            return;
        }
        if (!player.isAlive()) {
            clear(player);
            return;
        }
        ExerciseAction action = ExerciseAction.byId(data.getString(ACTIVE_ACTION).orElse(""));
        if (action == null) {
            clear(player);
            return;
        }
        BlockPos pos = BlockPos.of(data.getLong(STATION_POS).orElse(0L));
        boolean hasStation = data.getBooleanOr(HAS_STATION, false);
        if (hasStation) {
            BlockState state = player.level().getBlockState(pos);
            if (ExerciseStationBlock.actionOf(state) != action
                    || player.blockPosition().distSqr(pos) > MAX_DISTANCE_SQUARED) {
                clear(player);
                player.sendSystemMessage(Component.translatable("message.earth_human.training.station_left"));
                return;
            }
        } else if (player.blockPosition().distSqr(pos) > MAX_DISTANCE_SQUARED) {
            clear(player);
            player.sendSystemMessage(Component.translatable("message.earth_human.training.station_left"));
            return;
        }
        long now = player.level().getGameTime();
        long finish = data.getLong(FINISH_TICK).orElse(now);
        if (now < finish) {
            if (now % 8L == 0L && player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        player.getX(), player.getY() + 1.1D, player.getZ(),
                        2, 0.22D, 0.18D, 0.22D, 0.02D);
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 1.45D, player.getZ(),
                        1, 0.12D, 0.08D, 0.12D, 0.008D);
            }
            if (now % 14L == 0L) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 0.35F, 1.10F);
            }
            if (now % 10L == 0L) {
                sync(player, action, (int) Math.max(0L, finish - now));
            }
            return;
        }
        clear(player);
        HumanExercise.apply(player, action, hasStation || HumanExercise.hasNearbyEquipment(player));
    }

    public static void cancel(ServerPlayer player) {
        if (player.getPersistentData().contains(ACTIVE_ACTION)) {
            clear(player);
            player.sendOverlayMessage(Component.translatable("message.earth_human.training.stopped"));
        }
    }

    private static void clear(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(ACTIVE_ACTION);
        data.remove(STATION_POS);
        data.remove(HAS_STATION);
        data.remove(FINISH_TICK);
        HumanNetwork.syncExercise(player, HumanExerciseSessionPayload.IDLE);
    }

    private static void sync(ServerPlayer player, ExerciseAction action, int remainingTicks) {
        HumanNetwork.syncExercise(player, new HumanExerciseSessionPayload(
                action == null ? "" : action.id, remainingTicks, action == null ? 0 : action.durationTicks));
    }
}
