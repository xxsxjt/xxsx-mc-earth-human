package com.xxsx.earthhuman;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HumanOpenSettingsPayload() implements CustomPacketPayload {
    public static final HumanOpenSettingsPayload INSTANCE = new HumanOpenSettingsPayload();
    public static final Type<HumanOpenSettingsPayload> TYPE =
            new Type<>(EarthHuman.id("open_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HumanOpenSettingsPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
