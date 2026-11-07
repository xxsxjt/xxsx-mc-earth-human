package com.xxsx.earthhuman;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HumanExerciseActionPayload(String actionId) implements CustomPacketPayload {
    public static final Type<HumanExerciseActionPayload> TYPE =
            new Type<>(EarthHuman.id("exercise_action"));

    public static final StreamCodec<ByteBuf, HumanExerciseActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, HumanExerciseActionPayload::actionId,
            HumanExerciseActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
