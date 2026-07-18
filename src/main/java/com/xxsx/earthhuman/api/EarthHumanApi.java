package com.xxsx.earthhuman.api;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.EarthHumanConfig;
import com.xxsx.earthhuman.HumanBodyStatus;
import com.xxsx.earthhuman.HumanNetwork;
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

    static List<BodyPart> resolveBodyParts(String... bodyPartIds) {
        return EarthHumanApiContract.resolveBodyParts(bodyPartIds);
    }

    public record RecoveryResult(double fatigueReduced, double bodyHealed) {
        public boolean changed() {
            return fatigueReduced > 0.01D || bodyHealed > 0.01D;
        }
    }

    public record HumanSnapshot(double hydration, double bodyTemperature, double fatigue,
                                double exertionLoad, String currentActivity,
                                double headIntegrity, double bodyIntegrity) {
    }
}
