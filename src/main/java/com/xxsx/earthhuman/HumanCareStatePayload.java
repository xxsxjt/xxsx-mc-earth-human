package com.xxsx.earthhuman;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HumanCareStatePayload(
        String actionId,
        int remainingTicks,
        int totalTicks,
        String feedbackKey) implements CustomPacketPayload {
    public static final HumanCareStatePayload IDLE = new HumanCareStatePayload("", 0, 0, "");
    public static final Type<HumanCareStatePayload> TYPE = new Type<>(EarthHuman.id("care_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HumanCareStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.cast(), HumanCareStatePayload::actionId,
            ByteBufCodecs.VAR_INT.cast(), HumanCareStatePayload::remainingTicks,
            ByteBufCodecs.VAR_INT.cast(), HumanCareStatePayload::totalTicks,
            ByteBufCodecs.STRING_UTF8.cast(), HumanCareStatePayload::feedbackKey,
            HumanCareStatePayload::new);

    public boolean active() {
        return !actionId.isEmpty() && remainingTicks > 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
