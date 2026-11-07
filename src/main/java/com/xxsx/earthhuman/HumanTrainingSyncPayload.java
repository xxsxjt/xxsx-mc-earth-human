package com.xxsx.earthhuman;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record HumanTrainingSyncPayload(
        double headXp,
        double torsoXp,
        double limbsXp,
        double headLoad,
        double torsoLoad,
        double limbsLoad,
        int unlockedMask
) implements CustomPacketPayload {
    public static final Type<HumanTrainingSyncPayload> TYPE =
            new Type<>(EarthHuman.id("training_sync"));

    public static final HumanTrainingSyncPayload EMPTY =
            new HumanTrainingSyncPayload(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1);

    public static final StreamCodec<ByteBuf, HumanTrainingSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                ByteBufCodecs.DOUBLE.encode(buf, payload.headXp);
                ByteBufCodecs.DOUBLE.encode(buf, payload.torsoXp);
                ByteBufCodecs.DOUBLE.encode(buf, payload.limbsXp);
                ByteBufCodecs.DOUBLE.encode(buf, payload.headLoad);
                ByteBufCodecs.DOUBLE.encode(buf, payload.torsoLoad);
                ByteBufCodecs.DOUBLE.encode(buf, payload.limbsLoad);
                ByteBufCodecs.VAR_INT.encode(buf, payload.unlockedMask);
            },
            buf -> new HumanTrainingSyncPayload(
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf))
    );

    public static HumanTrainingSyncPayload fromPlayer(ServerPlayer player) {
        HumanTrainingStatus training = HumanTrainingStatus.get(player);
        int mask = SomaticsLoreBridge.computeUnlockedMask(player, training);
        return new HumanTrainingSyncPayload(
                training.xp(BodyPart.HEAD),
                training.xp(BodyPart.TORSO),
                training.xp(BodyPart.LIMBS),
                training.load(BodyPart.HEAD),
                training.load(BodyPart.TORSO),
                training.load(BodyPart.LIMBS),
                mask);
    }

    public double xp(BodyPart part) {
        return switch (part) {
            case HEAD -> headXp;
            case TORSO -> torsoXp;
            case LIMBS -> limbsXp;
        };
    }

    public double load(BodyPart part) {
        return switch (part) {
            case HEAD -> headLoad;
            case TORSO -> torsoLoad;
            case LIMBS -> limbsLoad;
        };
    }

    public int level(BodyPart part) {
        return Math.min(50, (int) Math.floor(Math.sqrt(Math.max(0.0D, xp(part)) / 12.0D)));
    }

    public int totalLevel() {
        return level(BodyPart.HEAD) + level(BodyPart.TORSO) + level(BodyPart.LIMBS);
    }

    public double strengthFactor() {
        int total = totalLevel();
        double ratio = Math.min(1.0D, total / 150.0D);
        return 1.0D + 4.0D * ratio;
    }

    public boolean isNodeUnlocked(int index) {
        return (unlockedMask & (1 << index)) != 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
