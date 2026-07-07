package com.xxsx.earthhuman;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class HumanNetwork {
    private HumanNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("0.1.2")
                .playToClient(HumanStatusSyncPayload.TYPE, HumanStatusSyncPayload.CODEC);
    }

    public static void sync(ServerPlayer player, HumanStatus status) {
        PacketDistributor.sendToPlayer(player, HumanStatusSyncPayload.fromStatus(status));
    }

    public static void sync(ServerPlayer player) {
        sync(player, HumanStatus.get(player));
    }
}
