package com.xxsx.earthhuman;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HumanCareActionPayload(String actionId) implements CustomPacketPayload {
    public static final Type<HumanCareActionPayload> TYPE =
            new Type<>(EarthHuman.id("care_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HumanCareActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.cast(), HumanCareActionPayload::actionId,
            HumanCareActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
