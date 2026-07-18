package com.xxsx.earthhuman;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

public final class HumanTrainingStatus {
    private static final String PREFIX = EarthHuman.MODID + ".training.";
    private static final String LAST_WARN_TIME = PREFIX + "last_warning_game_time";
    private static final String LAST_MANUAL_TIME = PREFIX + "last_manual_game_time";
    private static final String SESSION_LOAD_SUFFIX = ".session_load";
    private static final String XP_SUFFIX = ".xp";

    private final double[] xp = new double[BodyPart.values().length];
    private final double[] sessionLoad = new double[BodyPart.values().length];
    public long lastWarningGameTime;
    public long lastManualGameTime;

    private HumanTrainingStatus() {
    }

    public static HumanTrainingStatus get(Player player) {
        CompoundTag data = player.getPersistentData();
        HumanTrainingStatus status = new HumanTrainingStatus();
        for (BodyPart part : BodyPart.values()) {
            status.xp[part.ordinal()] = HumanStatus.clamp(data.getDoubleOr(xpKey(part), 0.0D), 0.0D, 10_000.0D);
            status.sessionLoad[part.ordinal()] = HumanStatus.clamp(data.getDoubleOr(loadKey(part), 0.0D), 0.0D, 100.0D);
        }
        status.lastWarningGameTime = data.getLongOr(LAST_WARN_TIME, -1_000_000L);
        status.lastManualGameTime = data.getLongOr(LAST_MANUAL_TIME, -1_000_000L);
        return status;
    }

    public void save(Player player) {
        CompoundTag data = player.getPersistentData();
        for (BodyPart part : BodyPart.values()) {
            data.putDouble(xpKey(part), HumanStatus.clamp(xp(part), 0.0D, 10_000.0D));
            data.putDouble(loadKey(part), HumanStatus.clamp(load(part), 0.0D, 100.0D));
        }
        data.putLong(LAST_WARN_TIME, lastWarningGameTime);
        data.putLong(LAST_MANUAL_TIME, lastManualGameTime);
    }

    public void copyTo(Player player) {
        save(player);
    }

    public void reset() {
        for (BodyPart part : BodyPart.values()) {
            xp[part.ordinal()] = 0.0D;
            sessionLoad[part.ordinal()] = 0.0D;
        }
        lastWarningGameTime = -1_000_000L;
        lastManualGameTime = -1_000_000L;
    }

    public double xp(BodyPart part) {
        return xp[part.ordinal()];
    }

    public double load(BodyPart part) {
        return sessionLoad[part.ordinal()];
    }

    public int level(BodyPart part) {
        return Math.min(20, (int) Math.floor(Math.sqrt(xp(part) / 18.0D)));
    }

    public double train(ServerPlayer player, BodyPart part, double xpGain, double loadGain, double fatigueGain) {
        int index = part.ordinal();
        xp[index] = HumanStatus.clamp(xp[index] + Math.max(0.0D, xpGain), 0.0D, 10_000.0D);
        sessionLoad[index] = HumanStatus.clamp(sessionLoad[index] + Math.max(0.0D, loadGain), 0.0D, 100.0D);
        if (fatigueGain > 0.0D) {
            HumanStatus status = HumanStatus.get(player);
            status.fatigue = HumanStatus.clamp(status.fatigue + fatigueGain, 0.0D, 100.0D);
            status.save(player);
            HumanNetwork.sync(player, status);
        }
        return sessionLoad[index];
    }

    public double adjustLoad(BodyPart part, double amount) {
        int index = part.ordinal();
        sessionLoad[index] = HumanStatus.clamp(sessionLoad[index] + amount, 0.0D, 100.0D);
        return sessionLoad[index];
    }

    public void recoverTick(Player player, boolean sleeping, boolean resting) {
        if (!sleeping && !resting) {
            return;
        }
        double amount = sleeping ? 0.85D : 0.020D;
        for (BodyPart part : BodyPart.values()) {
            int index = part.ordinal();
            sessionLoad[index] = Math.max(0.0D, sessionLoad[index] - amount);
        }
    }

    public boolean shouldWarn(ServerPlayer player, int cooldownTicks) {
        if (player.level().getGameTime() - lastWarningGameTime < cooldownTicks) {
            return false;
        }
        lastWarningGameTime = player.level().getGameTime();
        return true;
    }

    public boolean canManualExercise(ServerPlayer player, int cooldownTicks) {
        return player.level().getGameTime() - lastManualGameTime >= cooldownTicks;
    }

    public void markManualExercise(ServerPlayer player) {
        lastManualGameTime = player.level().getGameTime();
    }

    public Component describeLine(BodyPart part) {
        return Component.translatable(
                "message.earth_human.training.line",
                Component.translatable(part.translationKey()),
                level(part),
                Math.round(load(part)));
    }

    public String summary() {
        return String.format(Locale.ROOT,
                "head=%d torso=%d limbs=%d",
                level(BodyPart.HEAD),
                level(BodyPart.TORSO),
                level(BodyPart.LIMBS));
    }

    private static String xpKey(BodyPart part) {
        return PREFIX + part.id + XP_SUFFIX;
    }

    private static String loadKey(BodyPart part) {
        return PREFIX + part.id + SESSION_LOAD_SUFFIX;
    }
}
