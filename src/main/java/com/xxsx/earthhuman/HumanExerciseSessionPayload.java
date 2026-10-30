package com.xxsx.earthhuman;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client state of the station exercise session, used for the camera and the arm motion. */
public record HumanExerciseSessionPayload(
        String actionId,
        int remainingTicks,
        int totalTicks) implements CustomPacketPayload {
    public static final HumanExerciseSessionPayload IDLE = new HumanExerciseSessionPayload("", 0, 0);
    public static final Type<HumanExerciseSessionPayload> TYPE = new Type<>(EarthHuman.id("exercise_session"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HumanExerciseSessionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.cast(), HumanExerciseSessionPayload::actionId,
            ByteBufCodecs.VAR_INT.cast(), HumanExerciseSessionPayload::remainingTicks,
            ByteBufCodecs.VAR_INT.cast(), HumanExerciseSessionPayload::totalTicks,
            HumanExerciseSessionPayload::new);

    public boolean active() {
        return !actionId.isEmpty() && remainingTicks > 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
