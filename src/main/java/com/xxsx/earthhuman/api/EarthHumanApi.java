package com.xxsx.earthhuman.api;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.EarthHumanConfig;
import com.xxsx.earthhuman.HumanBodyStatus;
import com.xxsx.earthhuman.HumanNetwork;
import com.xxsx.earthhuman.HumanActivityType;
import com.xxsx.earthhuman.HumanExercise;
import com.xxsx.earthhuman.HumanStatus;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Objects;

/** Stable server-authoritative integration surface for optional companion mods. */
public final class EarthHumanApi {
    public static final int API_VERSION = EarthHumanApiContract.API_VERSION;

    private EarthHumanApi() {
    }

    public static int apiVersion() {
        return EarthHumanApiContract.apiVersion();
    }

    public static HumanSnapshot snapshot(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        HumanStatus status = HumanStatus.get(player);
        HumanBodyStatus body = HumanBodyStatus.get(player);
        double totalIntegrity = 0.0D;
        for (BodyPart part : BodyPart.values()) {
            totalIntegrity += body.percent(part);
        }
        return new HumanSnapshot(
                status.hydration,
                status.bodyTemperature,
                status.fatigue,
                status.exertionLoad,
                status.currentActivity == null ? "idle" : status.currentActivity.id,
                body.percent(BodyPart.HEAD),
                totalIntegrity / BodyPart.values().length);
    }

    /**
     * Applies recovery through Earth Human's current model, configuration, persistence and sync pipeline.
     * The body heal value is applied to each unique valid body-part id.
     */
    public static RecoveryResult recover(ServerPlayer player, double fatigueReduction,
                                         double bodyHealPerPart, String... bodyPartIds) {
        Objects.requireNonNull(player, "player");
        HumanStatus status = HumanStatus.get(player);
        HumanBodyStatus body = HumanBodyStatus.get(player);

        double fatigueBefore = status.fatigue;
        if (Double.isFinite(fatigueReduction) && fatigueReduction > 0.0D) {
            status.fatigue = HumanStatus.clamp(status.fatigue - fatigueReduction, 0.0D, 100.0D);
        }

        double healed = 0.0D;
        double configuredHeal = Double.isFinite(bodyHealPerPart)
                ? Math.max(0.0D, bodyHealPerPart) * EarthHumanConfig.healingMultiplier()
                : 0.0D;
        if (configuredHeal > 0.0D) {
            for (BodyPart part : resolveBodyParts(bodyPartIds)) {
                double before = body.health(part);
                body.healPart(player, part, configuredHeal);
                healed += body.health(part) - before;
            }
        }

        status.save(player);
        body.save(player);
        HumanNetwork.sync(player, status);
        return new RecoveryResult(fatigueBefore - status.fatigue, healed);
    }

    /**
     * Lets an optional mod report one server-authoritative custom activity without touching private NBT.
     * Distance is in blocks for training attribution; loadGain is the activity's immediate load contribution.
     */
    public static ActivityResult applyActivity(ServerPlayer player, String activityId,
                                               double distanceBlocks, double loadGain,
                                               String... bodyPartIds) {
        Objects.requireNonNull(player, "player");
        EarthHumanApiContract.ActivityInput input =
                EarthHumanApiContract.normalizeActivity(activityId, distanceBlocks, loadGain);
        if (input == null) {
            return ActivityResult.rejected();
        }

        HumanStatus status = HumanStatus.get(player);
        double oldLoad = status.exertionLoad;
        double oldFatigue = status.fatigue;
        status.currentActivity = input.activity();
        status.exertionLoad = HumanStatus.clamp(
                status.exertionLoad + input.loadGain() * EarthHumanConfig.fatigueGainMultiplier(),
                0.0D, 100.0D);
        if (EarthHumanConfig.fatigueEnabled()) {
            status.fatigue = HumanStatus.clamp(
                    status.fatigue + input.loadGain() * 0.35D * EarthHumanConfig.fatigueGainMultiplier(),
                    0.0D, 100.0D);
        }
        status.lastActiveGameTime = player.level().getGameTime();

        double trainingDistance = input.distanceBlocks() * 0.01D;
        for (BodyPart part : resolveBodyParts(bodyPartIds)) {
            HumanExercise.applyPassiveUse(player, part, trainingDistance * 0.02D, trainingDistance * 0.04D);
        }
        status.save(player);
        HumanNetwork.sync(player, status);
        return new ActivityResult(true, input.activity(), status.exertionLoad - oldLoad,
                status.fatigue - oldFatigue, trainingDistance);
    }

    public static double airQuality(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return HumanStatus.get(player).airQuality;
    }

    public static DetailedSnapshot detailedSnapshot(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        HumanBodyStatus body = HumanBodyStatus.get(player);
        List<PartSnapshot> parts = new java.util.ArrayList<>();
        for (BodyPart part : BodyPart.values()) {
            parts.add(new PartSnapshot(part.id, body.health(part), body.maxHealth(part), body.percent(part)));
        }
        return new DetailedSnapshot(snapshot(player), HumanStatus.get(player).airQuality, List.copyOf(parts));
    }

    static List<BodyPart> resolveBodyParts(String... bodyPartIds) {
        return EarthHumanApiContract.resolveBodyParts(bodyPartIds);
    }

    public record RecoveryResult(double fatigueReduced, double bodyHealed) {
        public boolean changed() {
            return fatigueReduced > 0.01D || bodyHealed > 0.01D;
        }
    }

    public record ActivityResult(boolean accepted, HumanActivityType activity,
                                 double loadAdded, double fatigueAdded, double trainingDistance) {
        public static ActivityResult rejected() {
            return new ActivityResult(false, HumanActivityType.IDLE, 0.0D, 0.0D, 0.0D);
        }
    }

    public record DetailedSnapshot(HumanSnapshot vitals, double airQuality, List<PartSnapshot> bodyParts) {
    }

    public record PartSnapshot(String id, double health, double maxHealth, double integrity) {
    }

    public record HumanSnapshot(double hydration, double bodyTemperature, double fatigue,
                                double exertionLoad, String currentActivity,
                                double headIntegrity, double bodyIntegrity) {
    }
}
