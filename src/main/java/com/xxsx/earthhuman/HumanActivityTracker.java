package com.xxsx.earthhuman;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

public final class HumanActivityTracker {
    private static final String PREFIX = EarthHuman.MODID + ".activity.stats.";
    private static final String INITIALIZED = PREFIX + "initialized";

    private static final Counter WALK = counter("walk", Stats.WALK_ONE_CM);
    private static final Counter CROUCH = counter("crouch", Stats.CROUCH_ONE_CM);
    private static final Counter SPRINT = counter("sprint", Stats.SPRINT_ONE_CM);
    private static final Counter SWIM = counter("swim", Stats.SWIM_ONE_CM);
    private static final Counter WALK_ON_WATER = counter("walk_on_water", Stats.WALK_ON_WATER_ONE_CM);
    private static final Counter WALK_UNDER_WATER = counter("walk_under_water", Stats.WALK_UNDER_WATER_ONE_CM);
    private static final Counter CLIMB = counter("climb", Stats.CLIMB_ONE_CM);
    private static final Counter MINECART = counter("minecart", Stats.MINECART_ONE_CM);
    private static final Counter BOAT = counter("boat", Stats.BOAT_ONE_CM);
    private static final Counter PIG = counter("pig", Stats.PIG_ONE_CM);
    private static final Counter HAPPY_GHAST = counter("happy_ghast", Stats.HAPPY_GHAST_ONE_CM);
    private static final Counter HORSE = counter("horse", Stats.HORSE_ONE_CM);
    private static final Counter STRIDER = counter("strider", Stats.STRIDER_ONE_CM);
    private static final Counter NAUTILUS = counter("nautilus", Stats.NAUTILUS_ONE_CM);
    private static final Counter AVIATE = counter("aviate", Stats.AVIATE_ONE_CM);
    private static final Counter FLY = counter("fly", Stats.FLY_ONE_CM);
    private static final Counter[] ALL = {
            WALK, CROUCH, SPRINT, SWIM, WALK_ON_WATER, WALK_UNDER_WATER, CLIMB,
            MINECART, BOAT, PIG, HAPPY_GHAST, HORSE, STRIDER, NAUTILUS, AVIATE, FLY
    };

    private HumanActivityTracker() {
    }

    public static void initialize(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        for (Counter counter : ALL) {
            data.putInt(counter.key, value(player, counter.stat));
        }
        data.putBoolean(INITIALIZED, true);
    }

    public static HumanActivitySample sample(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBooleanOr(INITIALIZED, false)) {
            initialize(player);
            return HumanActivitySample.EMPTY;
        }

        int walking = add(delta(player, data, WALK), delta(player, data, CROUCH));
        int sprinting = delta(player, data, SPRINT);
        int swimming = add(delta(player, data, SWIM), delta(player, data, WALK_ON_WATER), delta(player, data, WALK_UNDER_WATER));
        int climbing = delta(player, data, CLIMB);
        int riding = add(
                delta(player, data, MINECART),
                delta(player, data, BOAT),
                delta(player, data, PIG),
                delta(player, data, HAPPY_GHAST),
                delta(player, data, HORSE),
                delta(player, data, STRIDER),
                delta(player, data, NAUTILUS));
        int elytra = delta(player, data, AVIATE);
        int flying = delta(player, data, FLY);
        return new HumanActivitySample(walking, sprinting, swimming, climbing, riding, elytra, flying);
    }

    private static int delta(ServerPlayer player, CompoundTag data, Counter counter) {
        int current = value(player, counter.stat);
        int previous = data.getIntOr(counter.key, current);
        data.putInt(counter.key, current);
        return current >= previous ? current - previous : 0;
    }

    private static int value(ServerPlayer player, Identifier stat) {
        return player.getStats().getValue(Stats.CUSTOM, stat);
    }

    private static Counter counter(String id, Identifier stat) {
        return new Counter(PREFIX + id, stat);
    }

    private static int add(int... values) {
        long total = 0L;
        for (int value : values) {
            total += Math.max(0, value);
        }
        return (int)Math.min(Integer.MAX_VALUE, total);
    }

    private record Counter(String key, Identifier stat) {
    }
}
