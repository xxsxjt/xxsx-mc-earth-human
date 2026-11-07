package com.xxsx.earthhuman;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Locale;

public final class HumanExercise {
    private static final int MANUAL_COOLDOWN_TICKS = 32;
    private static final double STRAIN_LOAD_THRESHOLD = 72.0D;
    private static final double SEVERE_STRAIN_LOAD_THRESHOLD = 90.0D;

    private HumanExercise() {
    }

    public static void handlePayload(HumanExerciseActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ExerciseAction action = ExerciseAction.byId(payload.actionId());
                if (action != null) {
                    HumanExerciseSession.start(player, action, null);
                }
            }
        });
    }

    public static boolean hasNearbyEquipment(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 2, 3))) {
            if (ExerciseStationBlock.actionOf(player.level().getBlockState(pos)) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean apply(ServerPlayer player, ExerciseAction action, boolean fromEquipment) {
        if (!EarthHumanConfig.trainingEnabled()) {
            player.sendSystemMessage(Component.translatable("message.earth_human.training.disabled"));
            return false;
        }
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }

        HumanTrainingStatus training = HumanTrainingStatus.get(player);
        if (!training.canManualExercise(player, MANUAL_COOLDOWN_TICKS)) {
            player.sendSystemMessage(Component.translatable("message.earth_human.training.too_fast"));
            return false;
        }
        training.markManualExercise(player);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

        double somaticsXpMult = (SomaticsLoreBridge.hasResistanceConditioning(player, training) ? 1.40D : 1.0D)
                * (fromEquipment ? 1.35D : 1.0D);
        double stretchMult = SomaticsLoreBridge.hasBreathAndStretch(player, training) ? 1.50D : 1.0D;

        double worstLoad = 0.0D;
        for (BodyPart part : action.parts) {
            double gainMultiplier = EarthHumanConfig.trainingGainMultiplier() * somaticsXpMult;
            double load = training.train(player, part,
                    action.xpGain * gainMultiplier,
                    Math.max(0.0D, action.loadGain) * EarthHumanConfig.trainingGainMultiplier(),
                    0.0D);
            if (action.loadGain < 0.0D) {
                load = training.adjustLoad(part, action.loadGain * stretchMult * EarthHumanConfig.recoveryMultiplier());
            }
            worstLoad = Math.max(worstLoad, load);
        }

        HumanStatus status = HumanStatus.get(player);
        if (EarthHumanConfig.fatigueEnabled()) {
            double fatigueDelta = action.fatigueGain >= 0.0D
                    ? action.fatigueGain * EarthHumanConfig.fatigueGainMultiplier()
                    : action.fatigueGain * stretchMult * EarthHumanConfig.recoveryMultiplier();
            status.fatigue = HumanStatus.clamp(status.fatigue + fatigueDelta, 0.0D, 100.0D);
        }
        status.save(player);

        if (action.loadGain > 0.0D) {
            applyStrainIfNeeded(player, training, action, worstLoad);
        }
        training.save(player);
        SomaticsLoreBridge.syncTrainingToLore(player, training);
        HumanBodyStatus body = HumanBodyStatus.get(player);
        body.applyFunctionalEffects(player);
        body.save(player);
        HumanNetwork.sync(player, status);
        player.sendSystemMessage(Component.translatable(
                fromEquipment ? "message.earth_human.training.used_equipment" : "message.earth_human.training.did_action",
                action.displayName(),
                Math.round(worstLoad),
                Math.round(status.fatigue)));
        return true;
    }

    public static void applyPassiveUse(ServerPlayer player, BodyPart part, double xpGain, double loadGain) {
        if (!EarthHumanConfig.trainingEnabled() || player.isCreative() || player.isSpectator()) {
            return;
        }
        HumanTrainingStatus training = HumanTrainingStatus.get(player);
        double gainMultiplier = EarthHumanConfig.trainingGainMultiplier();
        double load = training.train(player, part, xpGain * gainMultiplier, loadGain * gainMultiplier, 0.0D);
        if (load >= SEVERE_STRAIN_LOAD_THRESHOLD && training.shouldWarn(player, 240)) {
            player.addEffect(new MobEffectInstance(EarthHumanEffects.MUSCLE_STRAIN, 20 * 40, 1, true, false, true));
            player.sendSystemMessage(Component.translatable("message.earth_human.training.strain", Component.translatable(part.translationKey())));
        } else if (load >= STRAIN_LOAD_THRESHOLD && training.shouldWarn(player, 240)) {
            player.addEffect(new MobEffectInstance(EarthHumanEffects.MUSCLE_STRAIN, 20 * 25, 0, true, false, true));
            player.sendSystemMessage(Component.translatable("message.earth_human.training.overload", Component.translatable(part.translationKey())));
        }
        training.save(player);
    }

    public static void recover(ServerPlayer player, boolean sleeping, boolean resting) {
        if (!EarthHumanConfig.trainingEnabled()) {
            return;
        }
        HumanTrainingStatus training = HumanTrainingStatus.get(player);
        training.recoverTick(player, sleeping, resting);
        training.save(player);
    }

    public static String actionList() {
        StringBuilder builder = new StringBuilder();
        for (ExerciseAction action : ExerciseAction.values()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(action.id);
        }
        return builder.toString();
    }

    private static void applyStrainIfNeeded(ServerPlayer player, HumanTrainingStatus training, ExerciseAction action, double worstLoad) {
        if (worstLoad < STRAIN_LOAD_THRESHOLD) {
            return;
        }
        int amplifier = worstLoad >= SEVERE_STRAIN_LOAD_THRESHOLD ? 1 : 0;
        int duration = (amplifier + 1) * 20 * 45;
        player.addEffect(new MobEffectInstance(EarthHumanEffects.MUSCLE_STRAIN, duration, amplifier, true, false, true));
        if (training.shouldWarn(player, 80)) {
            BodyPart part = action.parts[0];
            player.sendSystemMessage(Component.translatable("message.earth_human.training.strain", Component.translatable(part.translationKey())));
        }
    }

    public static Component availableActionsText() {
        return Component.literal(actionList().toLowerCase(Locale.ROOT));
    }
}
