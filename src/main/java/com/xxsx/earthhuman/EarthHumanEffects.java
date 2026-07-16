package com.xxsx.earthhuman;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EarthHumanEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, EarthHuman.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> MUSCLE_STRAIN =
            EFFECTS.register("muscle_strain", MuscleStrainEffect::new);

    private EarthHumanEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
