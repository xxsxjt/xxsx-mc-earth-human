package com.xxsx.earthhuman;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EarthHumanBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EarthHuman.MODID);

    public static final DeferredBlock<ExerciseStationBlock> TRAINING_MAT =
            station("training_mat", ExerciseAction.STRETCH);
    public static final DeferredBlock<ExerciseStationBlock> RESISTANCE_BAND =
            station("resistance_band", ExerciseAction.RESISTANCE_BAND_ROW);
    public static final DeferredBlock<ExerciseStationBlock> SIMPLE_DUMBBELL =
            station("simple_dumbbell", ExerciseAction.DUMBBELL_CURL);

    private EarthHumanBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static DeferredBlock<ExerciseStationBlock> station(String id, ExerciseAction action) {
        return BLOCKS.registerBlock(id,
                properties -> ExerciseStationBlock.create(properties, action),
                () -> BlockBehaviour.Properties.of()
                        .noCollision()
                        .noOcclusion()
                        .instabreak()
                        .sound(SoundType.WOOL)
                        .pushReaction(PushReaction.DESTROY));
    }
}
