package com.xxsx.earthhuman;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HumanOpenStatusPayload(int initialTab) implements CustomPacketPayload {
    public static final Type<HumanOpenStatusPayload> TYPE =
            new Type<>(EarthHuman.id("open_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HumanOpenStatusPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.cast(), HumanOpenStatusPayload::initialTab,
            HumanOpenStatusPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
