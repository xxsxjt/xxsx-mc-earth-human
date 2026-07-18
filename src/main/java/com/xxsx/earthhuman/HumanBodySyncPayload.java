package com.xxsx.earthhuman;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HumanBodySyncPayload(
        double head,
        double torso,
        double limbs) implements CustomPacketPayload {
    public static final Type<HumanBodySyncPayload> TYPE =
            new Type<>(EarthHuman.id("human_body_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HumanBodySyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE.cast(), HumanBodySyncPayload::head,
            ByteBufCodecs.DOUBLE.cast(), HumanBodySyncPayload::torso,
            ByteBufCodecs.DOUBLE.cast(), HumanBodySyncPayload::limbs,
            HumanBodySyncPayload::new);

    /** Compatibility constructor for clients compiled against the six-region beta payload. */
    @Deprecated
    public HumanBodySyncPayload(double head, double torso, double leftArm, double rightArm,
                                double leftLeg, double rightLeg) {
        this(head, torso, (leftArm + rightArm + leftLeg + rightLeg) / 4.0D);
    }

    @Deprecated public double leftArm() { return limbs; }
    @Deprecated public double rightArm() { return limbs; }
    @Deprecated public double leftLeg() { return limbs; }
    @Deprecated public double rightLeg() { return limbs; }

    public static HumanBodySyncPayload fromBody(HumanBodyStatus body) {
        return new HumanBodySyncPayload(
                body.percent(BodyPart.HEAD),
                body.percent(BodyPart.TORSO),
                body.percent(BodyPart.LIMBS));
    }

    public double value(BodyPart part) {
        return switch (part) {
            case HEAD -> head;
            case TORSO -> torso;
            case LIMBS -> limbs;
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
