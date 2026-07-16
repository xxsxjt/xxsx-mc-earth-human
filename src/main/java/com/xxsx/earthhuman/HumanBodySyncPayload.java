package com.xxsx.earthhuman;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HumanBodySyncPayload(
        double head,
        double torso,
        double leftArm,
        double rightArm,
        double leftLeg,
        double rightLeg) implements CustomPacketPayload {
    public static final Type<HumanBodySyncPayload> TYPE =
            new Type<>(EarthHuman.id("human_body_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HumanBodySyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE.cast(), HumanBodySyncPayload::head,
            ByteBufCodecs.DOUBLE.cast(), HumanBodySyncPayload::torso,
            ByteBufCodecs.DOUBLE.cast(), HumanBodySyncPayload::leftArm,
            ByteBufCodecs.DOUBLE.cast(), HumanBodySyncPayload::rightArm,
            ByteBufCodecs.DOUBLE.cast(), HumanBodySyncPayload::leftLeg,
            ByteBufCodecs.DOUBLE.cast(), HumanBodySyncPayload::rightLeg,
            HumanBodySyncPayload::new);

    public static HumanBodySyncPayload fromBody(HumanBodyStatus body) {
        return new HumanBodySyncPayload(
                body.percent(BodyPart.HEAD),
                body.percent(BodyPart.TORSO),
                body.percent(BodyPart.LEFT_ARM),
                body.percent(BodyPart.RIGHT_ARM),
                body.percent(BodyPart.LEFT_LEG),
                body.percent(BodyPart.RIGHT_LEG));
    }

    public double value(BodyPart part) {
        return switch (part) {
            case HEAD -> head;
            case TORSO -> torso;
            case LEFT_ARM -> leftArm;
            case RIGHT_ARM -> rightArm;
            case LEFT_LEG -> leftLeg;
            case RIGHT_LEG -> rightLeg;
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
