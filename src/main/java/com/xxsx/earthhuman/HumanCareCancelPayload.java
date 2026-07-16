package com.xxsx.earthhuman;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HumanCareCancelPayload() implements CustomPacketPayload {
    public static final HumanCareCancelPayload INSTANCE = new HumanCareCancelPayload();
    public static final Type<HumanCareCancelPayload> TYPE = new Type<>(EarthHuman.id("care_cancel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HumanCareCancelPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
