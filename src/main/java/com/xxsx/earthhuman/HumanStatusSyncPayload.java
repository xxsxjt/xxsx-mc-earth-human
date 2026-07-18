package com.xxsx.earthhuman;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HumanStatusSyncPayload(
        double hydration,
        double bodyTemperature,
        double fatigue,
        double exertionLoad,
        double airQuality,
        int currentActivityId) implements CustomPacketPayload {
    public static final Type<HumanStatusSyncPayload> TYPE =
            new Type<>(EarthHuman.id("human_status_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HumanStatusSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE.cast(), HumanStatusSyncPayload::hydration,
            ByteBufCodecs.DOUBLE.cast(), HumanStatusSyncPayload::bodyTemperature,
            ByteBufCodecs.DOUBLE.cast(), HumanStatusSyncPayload::fatigue,
            ByteBufCodecs.DOUBLE.cast(), HumanStatusSyncPayload::exertionLoad,
            ByteBufCodecs.DOUBLE.cast(), HumanStatusSyncPayload::airQuality,
            ByteBufCodecs.VAR_INT.cast(), HumanStatusSyncPayload::currentActivityId,
            HumanStatusSyncPayload::new);

    public static HumanStatusSyncPayload fromStatus(HumanStatus status) {
        return new HumanStatusSyncPayload(
                status.hydration,
                status.bodyTemperature,
                status.fatigue,
                status.exertionLoad,
                status.airQuality,
                status.currentActivity.networkId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
