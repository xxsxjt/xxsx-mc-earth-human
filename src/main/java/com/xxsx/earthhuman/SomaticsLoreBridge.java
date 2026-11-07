package com.xxsx.earthhuman;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Soft reflection bridge connecting earth_human's Somatic Training (锻体) with:
 * 1. earth_lore's SOM (锻体学) discipline & knowledge nodes
 * 2. xuanhuan_earth's Qi Cultivation (玄学吐纳)
 * 3. fantasy_earth's Arcane Attunement (魔力调谐)
 */
public final class SomaticsLoreBridge {
    public static final String NODE_CALISTHENICS_BASICS = "som/calisthenics_basics";
    public static final String NODE_BREATH_AND_STRETCH = "som/breath_and_stretch";
    public static final String NODE_TENDON_AND_BONE = "som/tendon_and_bone";
    public static final String NODE_RESISTANCE_CONDITIONING = "som/resistance_conditioning";
    public static final String NODE_VISCERAL_VITALITY = "som/visceral_vitality";
    public static final String NODE_MARTIAL_SOMATICS = "som/martial_somatics";
    public static final String NODE_FIVEFOLD_PEAK = "som/fivefold_peak";

    public static final String[] ALL_SOM_NODES = {
            NODE_CALISTHENICS_BASICS,
            NODE_BREATH_AND_STRETCH,
            NODE_TENDON_AND_BONE,
            NODE_RESISTANCE_CONDITIONING,
            NODE_VISCERAL_VITALITY,
            NODE_MARTIAL_SOMATICS,
            NODE_FIVEFOLD_PEAK
    };

    private SomaticsLoreBridge() {
    }

    public static boolean isLoreLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("earth_lore");
    }

    public static boolean isXuanhuanLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("xuanhuan_earth");
    }

    public static boolean isFantasyLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("fantasy_earth");
    }

    public static boolean isTheologyLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("earth_theology");
    }

    /**
     * Reads the player's Piety (0..100) from earth_theology's synced PietyAttachment when installed.
     */
    public static int queryTheologyPiety(Player player) {
        if (player == null || !isTheologyLoaded()) {
            return 0;
        }
        try {
            Class<?> cls = Class.forName("com.xxsx.earththeology.EarthTheology");
            Object pietyHolder = cls.getField("PIETY").get(null);
            Method getAttachmentType = pietyHolder.getClass().getMethod("get");
            Object attachmentType = getAttachmentType.invoke(pietyHolder);
            Method getData = net.neoforged.neoforge.attachment.AttachmentHolder.class.getMethod(
                    "getData", net.neoforged.neoforge.attachment.AttachmentType.class);
            Object attachment = getData.invoke(player, attachmentType);
            if (attachment != null) {
                Method valueMethod = attachment.getClass().getMethod("value");
                Object val = valueMethod.invoke(attachment);
                if (val instanceof Number number) {
                    return number.intValue();
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /**
     * Returns true if the player currently has earth_theology:blessed active.
     */
    public static boolean hasTheologyBlessing(Player player) {
        return hasTheologyEffect(player, "blessed");
    }

    /**
     * Returns true if the player currently has earth_theology:serenity active.
     */
    public static boolean hasTheologySerenity(Player player) {
        return hasTheologyEffect(player, "serenity");
    }

    private static boolean hasTheologyEffect(Player player, String path) {
        if (player == null || !isTheologyLoaded()) {
            return false;
        }
        try {
            var holderOpt = BuiltInRegistries.MOB_EFFECT.get(Identifier.fromNamespaceAndPath("earth_theology", path));
            if (holderOpt.isPresent()) {
                return player.hasEffect(holderOpt.get());
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static int computeUnlockedMask(ServerPlayer player, HumanTrainingStatus training) {
        int mask = 1; // bit 0: calisthenics_basics is always unlocked (T0 root)
        if (hasBreathAndStretch(player, training)) {
            mask |= (1 << 1);
        }
        if (hasTendonAndBone(player, training)) {
            mask |= (1 << 2);
        }
        if (hasResistanceConditioning(player, training)) {
            mask |= (1 << 3);
        }
        if (hasVisceralVitality(player, training)) {
            mask |= (1 << 4);
        }
        if (hasMartialSomatics(player, training)) {
            mask |= (1 << 5);
        }
        if (hasFivefoldPeak(player, training)) {
            mask |= (1 << 6);
        }
        return mask;
    }

    public static boolean hasBreathAndStretch(ServerPlayer player) {
        return hasBreathAndStretch(player, HumanTrainingStatus.get(player));
    }

    public static boolean hasBreathAndStretch(ServerPlayer player, HumanTrainingStatus training) {
        int total = totalLevel(training);
        return total >= 3 || serverKnowsNode(player, NODE_BREATH_AND_STRETCH);
    }

    public static boolean hasTendonAndBone(ServerPlayer player) {
        return hasTendonAndBone(player, HumanTrainingStatus.get(player));
    }

    public static boolean hasTendonAndBone(ServerPlayer player, HumanTrainingStatus training) {
        return training.level(BodyPart.LIMBS) >= 2 || serverKnowsNode(player, NODE_TENDON_AND_BONE);
    }

    public static boolean hasResistanceConditioning(ServerPlayer player) {
        return hasResistanceConditioning(player, HumanTrainingStatus.get(player));
    }

    public static boolean hasResistanceConditioning(ServerPlayer player, HumanTrainingStatus training) {
        return totalLevel(training) >= 8 || serverKnowsNode(player, NODE_RESISTANCE_CONDITIONING);
    }

    public static boolean hasVisceralVitality(ServerPlayer player) {
        return hasVisceralVitality(player, HumanTrainingStatus.get(player));
    }

    public static boolean hasVisceralVitality(ServerPlayer player, HumanTrainingStatus training) {
        return training.level(BodyPart.TORSO) >= 4 || serverKnowsNode(player, NODE_VISCERAL_VITALITY);
    }

    public static boolean hasMartialSomatics(ServerPlayer player) {
        return hasMartialSomatics(player, HumanTrainingStatus.get(player));
    }

    public static boolean hasMartialSomatics(ServerPlayer player, HumanTrainingStatus training) {
        return totalLevel(training) >= 15 || serverKnowsNode(player, NODE_MARTIAL_SOMATICS);
    }

    public static boolean hasFivefoldPeak(ServerPlayer player) {
        return hasFivefoldPeak(player, HumanTrainingStatus.get(player));
    }

    public static boolean hasFivefoldPeak(ServerPlayer player, HumanTrainingStatus training) {
        return totalLevel(training) >= 45 || serverKnowsNode(player, NODE_FIVEFOLD_PEAK);
    }

    /**
     * Returns the current Somatic Multiplier in [1.0, 5.0] (up to 5x normal human peak).
     */
    public static double somaticMultiplier(ServerPlayer player, HumanTrainingStatus training) {
        int total = totalLevel(training);
        double ratio = Math.min(1.0D, total / 150.0D);
        double bonus = 4.0D * ratio;
        if (hasVisceralVitality(player, training)) {
            bonus = Math.max(bonus, 0.8D);
        }
        if (hasMartialSomatics(player, training)) {
            bonus = Math.max(bonus, 1.6D);
        }
        if (hasFivefoldPeak(player, training)) {
            bonus = Math.max(bonus, 3.0D + ratio);
        }
        return Math.min(5.0D, 1.0D + bonus);
    }

    public static int totalLevel(HumanTrainingStatus training) {
        if (training == null) {
            return 0;
        }
        return training.level(BodyPart.HEAD) + training.level(BodyPart.TORSO) + training.level(BodyPart.LIMBS);
    }

    public static void syncTrainingToLore(ServerPlayer player, HumanTrainingStatus training) {
        if (player == null || training == null || !isLoreLoaded()) {
            return;
        }
        List<Identifier> toGrant = new ArrayList<>();
        toGrant.add(EarthHuman.id(NODE_CALISTHENICS_BASICS));
        if (totalLevel(training) >= 3 && !serverKnowsNode(player, NODE_BREATH_AND_STRETCH)) {
            toGrant.add(EarthHuman.id(NODE_BREATH_AND_STRETCH));
        }
        if (training.level(BodyPart.LIMBS) >= 2 && !serverKnowsNode(player, NODE_TENDON_AND_BONE)) {
            toGrant.add(EarthHuman.id(NODE_TENDON_AND_BONE));
        }
        if (totalLevel(training) >= 8 && !serverKnowsNode(player, NODE_RESISTANCE_CONDITIONING)) {
            toGrant.add(EarthHuman.id(NODE_RESISTANCE_CONDITIONING));
        }
        if (training.level(BodyPart.TORSO) >= 4 && !serverKnowsNode(player, NODE_VISCERAL_VITALITY)) {
            toGrant.add(EarthHuman.id(NODE_VISCERAL_VITALITY));
        }
        if (totalLevel(training) >= 15 && !serverKnowsNode(player, NODE_MARTIAL_SOMATICS)) {
            toGrant.add(EarthHuman.id(NODE_MARTIAL_SOMATICS));
        }
        if (totalLevel(training) >= 45 && !serverKnowsNode(player, NODE_FIVEFOLD_PEAK)) {
            toGrant.add(EarthHuman.id(NODE_FIVEFOLD_PEAK));
        }
        if (toGrant.size() > 1) {
            try {
                Class<?> cls = Class.forName("com.xxsx.earthlore.knowledge.KnowledgeService");
                Method grantAll = cls.getMethod("grantAll", ServerPlayer.class, java.util.Collection.class);
                grantAll.invoke(null, player, toGrant);
            } catch (Throwable ignored) {
            }
        }
    }

    public static boolean serverKnowsNode(ServerPlayer player, String nodePath) {
        if (player == null || !isLoreLoaded()) {
            return false;
        }
        try {
            Class<?> cls = Class.forName("com.xxsx.earthlore.knowledge.KnowledgeService");
            Method knows = cls.getMethod("knows", UUID.class, Identifier.class);
            Object result = knows.invoke(null, player.getUUID(), EarthHuman.id(nodePath));
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
