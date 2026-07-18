package com.xxsx.earthhuman;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HumanOpenGuidePayload() implements CustomPacketPayload {
    public static final HumanOpenGuidePayload INSTANCE = new HumanOpenGuidePayload();
    public static final Type<HumanOpenGuidePayload> TYPE =
            new Type<>(EarthHuman.id("open_guide"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HumanOpenGuidePayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
