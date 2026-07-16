package com.xxsx.earthhuman;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class HumanStatus {
    private static final String PREFIX = EarthHuman.MODID + ".";
    private static final String INIT = PREFIX + "initialized";
    private static final String HYDRATION = PREFIX + "hydration";
    private static final String BODY_TEMP = PREFIX + "body_temperature";
    private static final String FATIGUE = PREFIX + "fatigue";
    private static final String WARN_TIME = PREFIX + "last_warning_game_time";

    public double hydration;
    public double bodyTemperature;
    public double fatigue;
    public long lastWarningGameTime;

    public HumanStatus(double hydration, double bodyTemperature, double fatigue, long lastWarningGameTime) {
        this.hydration = hydration;
        this.bodyTemperature = bodyTemperature;
        this.fatigue = fatigue;
        this.lastWarningGameTime = lastWarningGameTime;
    }

    public static HumanStatus get(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.getIntOr(INIT, 0) == 0) {
            HumanStatus fresh = new HumanStatus(100.0D, 37.0D, 0.0D, 0);
            fresh.save(player);
            return fresh;
        }
        return new HumanStatus(
                clamp(data.getDoubleOr(HYDRATION, 100.0D), 0.0D, 100.0D),
                clamp(data.getDoubleOr(BODY_TEMP, 37.0D), 32.0D, 42.5D),
                clamp(data.getDoubleOr(FATIGUE, 0.0D), 0.0D, 100.0D),
                data.getLongOr(WARN_TIME, -1_000_000L));
    }

    public void save(Player player) {
        CompoundTag data = player.getPersistentData();
        data.putInt(INIT, 1);
        data.putDouble(HYDRATION, clamp(hydration, 0.0D, 100.0D));
        data.putDouble(BODY_TEMP, clamp(bodyTemperature, 32.0D, 42.5D));
        data.putDouble(FATIGUE, clamp(fatigue, 0.0D, 100.0D));
        data.putLong(WARN_TIME, lastWarningGameTime);
    }

    public void copyTo(Player player) {
        save(player);
    }

    public void reset() {
        hydration = 100.0D;
        bodyTemperature = 37.0D;
        fatigue = 0.0D;
        lastWarningGameTime = -1_000_000L;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
