package com.xxsx.earthhuman;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class HumanNetwork {
    private HumanNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("0.1.19")
                .playToClient(HumanStatusSyncPayload.TYPE, HumanStatusSyncPayload.CODEC)
                .playToClient(HumanBodySyncPayload.TYPE, HumanBodySyncPayload.CODEC)
                .playToClient(HumanOpenSettingsPayload.TYPE, HumanOpenSettingsPayload.CODEC)
                .playToClient(HumanOpenStatusPayload.TYPE, HumanOpenStatusPayload.CODEC)
                .playToClient(HumanOpenGuidePayload.TYPE, HumanOpenGuidePayload.CODEC)
                .playToClient(HumanCareStatePayload.TYPE, HumanCareStatePayload.CODEC)
                .playToClient(HumanExerciseSessionPayload.TYPE, HumanExerciseSessionPayload.CODEC)
                .playToServer(HumanCareActionPayload.TYPE, HumanCareActionPayload.CODEC, HumanCareActions::handlePayload)
                .playToServer(HumanCareCancelPayload.TYPE, HumanCareCancelPayload.CODEC, HumanCareActions::handleCancelPayload);
    }

    public static void sync(ServerPlayer player, HumanStatus status) {
        PacketDistributor.sendToPlayer(player, HumanStatusSyncPayload.fromStatus(status));
        PacketDistributor.sendToPlayer(player, HumanBodySyncPayload.fromBody(HumanBodyStatus.get(player)));
    }

    public static void sync(ServerPlayer player) {
        sync(player, HumanStatus.get(player));
    }

    public static void syncBody(ServerPlayer player, HumanBodyStatus body) {
        PacketDistributor.sendToPlayer(player, HumanBodySyncPayload.fromBody(body));
    }

    public static void openSettings(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, HumanOpenSettingsPayload.INSTANCE);
    }

    public static void openStatus(ServerPlayer player, int initialTab) {
        sync(player);
        HumanCareActions.syncState(player, "");
        PacketDistributor.sendToPlayer(player, new HumanOpenStatusPayload(initialTab));
    }

    public static void openGuide(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, HumanOpenGuidePayload.INSTANCE);
    }

    public static void syncCare(ServerPlayer player, HumanCareStatePayload state) {
        PacketDistributor.sendToPlayer(player, state);
    }

    public static void syncExercise(ServerPlayer player, HumanExerciseSessionPayload state) {
        PacketDistributor.sendToPlayer(player, state);
    }
}
