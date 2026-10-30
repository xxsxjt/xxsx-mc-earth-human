package com.xxsx.earthhuman;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A training station placed on the ground. Right-clicking it starts the matching exercise session,
 * which switches the camera to third person while the action runs.
 */
public class ExerciseStationBlock extends Block {
    public static final MapCodec<ExerciseStationBlock> CODEC = simpleCodec(ExerciseStationBlock::new);
    private static final Map<Block, ExerciseAction> ACTIONS = new IdentityHashMap<>();
    private static final VoxelShape SHAPE = Block.column(16.0D, 0.0D, 2.0D);

    public ExerciseStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    static ExerciseStationBlock create(BlockBehaviour.Properties properties, ExerciseAction action) {
        ExerciseStationBlock block = new ExerciseStationBlock(properties);
        ACTIONS.put(block, action);
        return block;
    }

    public static ExerciseAction actionOf(BlockState state) {
        return ACTIONS.get(state.getBlock());
    }

    @Override
    protected MapCodec<? extends ExerciseStationBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        ExerciseAction action = actionOf(state);
        if (action == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        return HumanExerciseSession.start(serverPlayer, action, pos)
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }
}
