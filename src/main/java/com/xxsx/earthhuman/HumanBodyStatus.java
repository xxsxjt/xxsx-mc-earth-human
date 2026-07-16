package com.xxsx.earthhuman;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class HumanBodyStatus {
    private static final String PREFIX = EarthHuman.MODID + ".body.";
    private static final String INIT = PREFIX + "initialized";
    private static final String INIT_MODEL = PREFIX + "init_model";
    private static final String LAST_WARN_TIME = PREFIX + "last_warning_game_time";
    private static final String LAST_USE_SPREAD_TIME = PREFIX + "last_use_spread_game_time";
    private static final String LAST_VANILLA_MAX_HEALTH = PREFIX + "last_vanilla_max_health";
    private static final int CURRENT_INIT_MODEL = 4;
    private static final double BASE_VANILLA_HEALTH = 20.0D;
    private static final net.minecraft.resources.Identifier HEAD_HEALTH_LIMIT_ID =
            EarthHuman.id("head_health_limit");

    private final double[] health = new double[BodyPart.values().length];
    private double vanillaMaxHealth = BASE_VANILLA_HEALTH;
    public long lastWarningGameTime;
    public long lastUseSpreadGameTime;

    private HumanBodyStatus() {
        reset();
    }

    public static HumanBodyStatus get(Player player) {
        CompoundTag data = player.getPersistentData();
        HumanBodyStatus status = new HumanBodyStatus();
        if (data.getIntOr(INIT, 0) == 0) {
            status.initializeFromVanillaHealth(player);
            status.save(player);
            return status;
        }

        int model = data.getIntOr(INIT_MODEL, 1);
        status.vanillaMaxHealth = Math.max(1.0D, data.getDoubleOr(LAST_VANILLA_MAX_HEALTH, BASE_VANILLA_HEALTH));
        for (BodyPart part : BodyPart.values()) {
            status.health[part.ordinal()] = model < CURRENT_INIT_MODEL
                    ? status.migrateHealth(data, part, model)
                    : HumanStatus.clamp(data.getDoubleOr(healthKey(part), status.maxHealth(part)), 0.0D, status.maxHealth(part));
        }
        status.syncCapacityFromVanillaMax(player);
        if (model < CURRENT_INIT_MODEL) {
            status.save(player);
        }
        status.lastWarningGameTime = data.getLongOr(LAST_WARN_TIME, -1_000_000L);
        status.lastUseSpreadGameTime = data.getLongOr(LAST_USE_SPREAD_TIME, -1_000_000L);
        return status;
    }

    public void save(Player player) {
        syncCapacityFromVanillaMax(player);
        CompoundTag data = player.getPersistentData();
        data.putInt(INIT, 1);
        data.putInt(INIT_MODEL, CURRENT_INIT_MODEL);
        data.putDouble(LAST_VANILLA_MAX_HEALTH, vanillaMaxHealth);
        for (BodyPart part : BodyPart.values()) {
            data.putDouble(healthKey(part), HumanStatus.clamp(health(part), 0.0D, maxHealth(part)));
        }
        data.putLong(LAST_WARN_TIME, lastWarningGameTime);
        data.putLong(LAST_USE_SPREAD_TIME, lastUseSpreadGameTime);
    }

    public void copyTo(Player player) {
        save(player);
    }

    public void reset() {
        vanillaMaxHealth = BASE_VANILLA_HEALTH;
        for (BodyPart part : BodyPart.values()) {
            health[part.ordinal()] = baseMaxHealth(part);
        }
        lastWarningGameTime = -1_000_000L;
        lastUseSpreadGameTime = -1_000_000L;
    }

    public void stabilizeAfterDeathRespawn(Player player) {
        syncCapacityFromVanillaMax(player);
        for (BodyPart part : BodyPart.values()) {
            double minimumRatio = part == BodyPart.HEAD
                    ? EarthHumanConfig.respawnHeadMinimumRatio()
                    : EarthHumanConfig.respawnOtherMinimumRatio();
            double minimum = maxHealth(part) * minimumRatio;
            if (health(part) < minimum) {
                setHealth(part, minimum);
            }
        }
    }

    private void initializeFromVanillaHealth(Player player) {
        vanillaMaxHealth = Math.max(1.0D, player.getMaxHealth());
        double ratio = HumanStatus.clamp(player.getHealth() / vanillaMaxHealth, 0.0D, 1.0D);
        for (BodyPart part : BodyPart.values()) {
            health[part.ordinal()] = maxHealth(part) * ratio;
        }
        lastWarningGameTime = -1_000_000L;
        lastUseSpreadGameTime = -1_000_000L;
    }

    private double migrateHealth(CompoundTag data, BodyPart part, int oldModel) {
        double oldMax = oldMaxHealth(part, oldModel);
        double oldValue = data.getDoubleOr(healthKey(part), oldMax);
        return BodyHealthMath.migratePreservingRatio(oldValue, oldMax, maxHealth(part));
    }

    private double oldMaxHealth(BodyPart part, int oldModel) {
        if (oldModel <= 2) {
            return switch (part) {
                case HEAD -> 10.0D;
                case TORSO -> 16.0D;
                case LEFT_ARM, RIGHT_ARM -> 10.0D;
                case LEFT_LEG, RIGHT_LEG -> 12.0D;
            };
        }
        return maxHealth(part);
    }

    public double health(BodyPart part) {
        return health[part.ordinal()];
    }

    public double maxHealth(BodyPart part) {
        return baseMaxHealth(part) * vanillaMaxScale();
    }

    public double percent(BodyPart part) {
        return health(part) / maxHealth(part);
    }

    public void setHealth(BodyPart part, double value) {
        health[part.ordinal()] = HumanStatus.clamp(value, 0.0D, maxHealth(part));
    }

    public double damage(BodyPart part, double amount) {
        double applied = Math.max(0.0D, amount);
        double before = health(part);
        setHealth(part, before - applied);
        return before - health(part);
    }

    public BodyPart worstPart(boolean includeHead) {
        BodyPart worst = null;
        double worstRatio = Double.MAX_VALUE;
        for (BodyPart part : BodyPart.values()) {
            if (!includeHead && part == BodyPart.HEAD) {
                continue;
            }
            double ratio = percent(part);
            if (ratio < worstRatio) {
                worstRatio = ratio;
                worst = part;
            }
        }
        return worst == null ? BodyPart.HEAD : worst;
    }

    public boolean healWorst(ServerPlayer player, double amount, boolean includeHead) {
        syncCapacityFromVanillaMax(player);
        BodyPart part = worstPart(includeHead);
        if (!includeHead && health(part) >= maxHealth(part)) {
            return false;
        }
        return healPart(player, part, amount);
    }

    public boolean healPart(ServerPlayer player, BodyPart part, double amount) {
        syncCapacityFromVanillaMax(player);
        double before = health(part);
        setHealth(part, before + Math.max(0.0D, amount));
        if (part == BodyPart.HEAD) {
            syncVanillaFromHead(player);
        }
        return health(part) > before + 0.01D;
    }

    public void ensureHeadHealthLimit(ServerPlayer player) {
        syncCapacityFromVanillaMax(player);
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getModifier(HEAD_HEALTH_LIMIT_ID) != null) {
            maxHealth.removeModifier(HEAD_HEALTH_LIMIT_ID);
        }
        if (EarthHumanConfig.healthBridgeMode() == EarthHumanRules.HealthBridgeMode.HEAD_BAR) {
            syncVanillaFromHead(player);
        }
    }

    public void pullHeadFromVanilla(ServerPlayer player) {
        syncCapacityFromVanillaMax(player);
        double max = Math.max(1.0D, player.getMaxHealth());
        double ratio = HumanStatus.clamp(player.getHealth() / max, 0.0D, 1.0D);
        setHealth(BodyPart.HEAD, maxHealth(BodyPart.HEAD) * ratio);
    }

    public void pullHeadDamageFromVanilla(ServerPlayer player) {
        syncCapacityFromVanillaMax(player);
        double max = Math.max(1.0D, player.getMaxHealth());
        double ratio = HumanStatus.clamp(player.getHealth() / max, 0.0D, 1.0D);
        double reflected = maxHealth(BodyPart.HEAD) * ratio;
        if (reflected < health(BodyPart.HEAD)) {
            setHealth(BodyPart.HEAD, reflected);
        }
    }

    public boolean bridgeVanillaHealing(ServerPlayer player, double vanillaHealAmount) {
        syncCapacityFromVanillaMax(player);
        double remaining = Math.max(0.0D, vanillaHealAmount) * EarthHumanConfig.healingMultiplier();
        if (!Double.isFinite(remaining) || remaining <= 0.01D) {
            return false;
        }
        boolean changed = false;
        double headBodyPerVanilla = maxHealth(BodyPart.HEAD) / Math.max(1.0D, player.getMaxHealth());
        double headMissing = Math.max(0.0D, maxHealth(BodyPart.HEAD) - health(BodyPart.HEAD));
        if (headMissing > 0.0D && headBodyPerVanilla > 0.0D) {
            double vanillaUsed = Math.min(remaining, headMissing / headBodyPerVanilla);
            setHealth(BodyPart.HEAD, health(BodyPart.HEAD) + vanillaUsed * headBodyPerVanilla);
            remaining -= vanillaUsed;
            changed = vanillaUsed > 0.01D;
        }
        for (BodyPart part : HEALING_SPREAD_ORDER) {
            if (remaining <= 0.01D) {
                break;
            }
            double before = remaining;
            remaining = healWithOverflow(part, remaining);
            changed |= remaining < before - 0.01D;
        }
        if (changed) {
            syncVanillaFromHead(player);
        }
        return changed;
    }

    public void syncVanillaFromHead(ServerPlayer player) {
        if (EarthHumanConfig.healthBridgeMode() != EarthHumanRules.HealthBridgeMode.HEAD_BAR) {
            return;
        }
        double ratio = HumanStatus.clamp(percent(BodyPart.HEAD), 0.0D, 1.0D);
        float target = (float) HumanStatus.clamp(player.getMaxHealth() * ratio, 0.0D, player.getMaxHealth());
        if (Math.abs(player.getHealth() - target) > 0.05F) {
            player.setHealth(target);
        }
    }

    public boolean spreadFromUse(ServerPlayer player, BodyPart primary, BodyPart secondary, double baseAmount) {
        if (player.level().getGameTime() - lastUseSpreadGameTime < 20L) {
            return false;
        }
        BodyPart part = percent(primary) <= percent(secondary) ? primary : secondary;
        return spreadFromUse(player, part, baseAmount);
    }

    public boolean spreadFromUse(ServerPlayer player, double baseAmount, BodyPart... parts) {
        if (parts == null || parts.length == 0 || player.level().getGameTime() - lastUseSpreadGameTime < 20L) {
            return false;
        }
        BodyPart selected = parts[0];
        for (int i = 1; i < parts.length; i++) {
            if (percent(parts[i]) < percent(selected)) {
                selected = parts[i];
            }
        }
        return spreadFromUse(player, selected, baseAmount);
    }

    public boolean spreadFromUse(ServerPlayer player, BodyPart part, double baseAmount) {
        if (player.level().getGameTime() - lastUseSpreadGameTime < 20L) {
            return false;
        }
        double severity = useSeverity(part);
        if (severity <= 0.0D) {
            return false;
        }
        lastUseSpreadGameTime = player.level().getGameTime();
        double spreadMultiplier = EarthHumanConfig.bodySpreadMultiplier();
        boolean changed = spreadToAdjacent(player, part, Math.max(0.0D, baseAmount) * severity * spreadMultiplier, true);
        if (part != BodyPart.TORSO && useSeverity(BodyPart.TORSO) > 0.0D) {
            changed |= spreadToAdjacent(player, BodyPart.TORSO,
                    Math.max(0.0D, baseAmount) * 0.25D * useSeverity(BodyPart.TORSO) * spreadMultiplier, true);
        }
        if (changed) {
            syncVanillaFromHead(player);
        }
        return changed;
    }

    public boolean spreadFromImpact(ServerPlayer player, BodyPart part, double incomingDamage, double beforeHealth) {
        if (part == BodyPart.HEAD || incomingDamage <= 0.0D) {
            return false;
        }
        double afterHealth = health(part);
        double overkill = Math.max(0.0D, incomingDamage - beforeHealth);
        double amount;
        if (beforeHealth <= 0.01D) {
            amount = incomingDamage * 0.45D;
        } else if (afterHealth <= 0.01D) {
            amount = overkill * 0.55D + incomingDamage * 0.10D;
        } else if (percent(part) < 0.30D) {
            amount = incomingDamage * 0.06D;
        } else {
            amount = incomingDamage * 0.025D;
        }
        boolean changed = spreadToAdjacent(player, part, amount * EarthHumanConfig.bodySpreadMultiplier(), false);
        if (changed) {
            syncVanillaFromHead(player);
        }
        return changed;
    }

    public void applyFunctionalEffects(ServerPlayer player) {
        if (!EarthHumanConfig.bodyEffectsEnabled()) {
            return;
        }
        int brokenLegs = disabledCount(BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        int criticalLegs = criticalCount(BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        int brokenArms = disabledCount(BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM);
        int criticalArms = criticalCount(BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM);

        if (brokenLegs > 0 || criticalLegs > 0) {
            int amp = brokenLegs >= 2 ? 2 : (brokenLegs == 1 ? 1 : 0);
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, amp, true, false, true));
        }
        if (brokenArms > 0 || criticalArms > 0) {
            int armLayers = Math.min(2, brokenArms + criticalArms);
            int amplifier = Math.max(0, armLayers - 1);
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, amplifier, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, amplifier, true, false, true));
        }
        if (health(BodyPart.TORSO) <= 0.01D || percent(BodyPart.TORSO) < 0.25D) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, health(BodyPart.TORSO) <= 0.01D ? 1 : 0, true, false, true));
            player.causeFoodExhaustion(0.05F);
        }
    }

    private int disabledCount(BodyPart first, BodyPart second) {
        return (health(first) <= 0.01D ? 1 : 0) + (health(second) <= 0.01D ? 1 : 0);
    }

    private int criticalCount(BodyPart first, BodyPart second) {
        return (isCriticalButActive(first) ? 1 : 0) + (isCriticalButActive(second) ? 1 : 0);
    }

    private boolean isCriticalButActive(BodyPart part) {
        return health(part) > 0.01D && percent(part) < 0.30D;
    }

    public void warnDirectDamage(ServerPlayer player, BodyPart part, double before) {
        if (player.level().getGameTime() - lastWarningGameTime < 80L) {
            return;
        }
        double after = health(part);
        if (after <= 0.01D && before > 0.01D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.body.failed", Component.translatable(part.translationKey())));
            lastWarningGameTime = player.level().getGameTime();
        } else if (after / maxHealth(part) < 0.30D && before / maxHealth(part) >= 0.30D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.body.critical", Component.translatable(part.translationKey())));
            lastWarningGameTime = player.level().getGameTime();
        }
    }

    public Component describeLine(BodyPart part) {
        double value = health(part);
        return Component.translatable(
                "message.earth_human.body.line",
                Component.translatable(part.translationKey()),
                String.format(java.util.Locale.ROOT, "%.1f", value),
                String.format(java.util.Locale.ROOT, "%.1f", maxHealth(part)),
                stateText(part));
    }

    private Component stateText(BodyPart part) {
        double value = health(part);
        if (value <= 0.01D) {
            return Component.translatable("message.earth_human.body.state.failed");
        }
        if (percent(part) < 0.30D) {
            return Component.translatable("message.earth_human.body.state.critical");
        }
        if (percent(part) < 0.70D) {
            return Component.translatable("message.earth_human.body.state.injured");
        }
        return Component.translatable("message.earth_human.body.state.ok");
    }

    private void warnSpread(ServerPlayer player, BodyPart from, BodyPart to, boolean fromUse) {
        if (player.level().getGameTime() - lastWarningGameTime < 120L) {
            return;
        }
        player.sendSystemMessage(Component.translatable(
                fromUse ? "message.earth_human.body.spread.use" : "message.earth_human.body.spread.impact",
                Component.translatable(from.translationKey()),
                Component.translatable(to.translationKey())));
        lastWarningGameTime = player.level().getGameTime();
    }

    private boolean spreadToAdjacent(ServerPlayer player, BodyPart from, double totalAmount, boolean fromUse) {
        if (totalAmount <= 0.01D) {
            return false;
        }
        Set<BodyPart> visited = new HashSet<>();
        visited.add(from);
        return spreadToAdjacent(player, from, totalAmount, fromUse, visited, 0);
    }

    private boolean spreadToAdjacent(ServerPlayer player, BodyPart from, double totalAmount, boolean fromUse,
                                     Set<BodyPart> visited, int depth) {
        if (totalAmount <= 0.01D || depth > 2) {
            return false;
        }
        BodyPart[] adjacent = adjacentParts(from);
        double amount = totalAmount / adjacent.length;
        boolean changed = false;
        for (BodyPart to : adjacent) {
            if (!visited.add(to)) {
                continue;
            }
            double adjusted = to == BodyPart.HEAD ? amount * 0.70D : amount;
            if (health(to) <= 0.01D) {
                changed |= spreadToAdjacent(player, to, adjusted * 0.55D, fromUse, visited, depth + 1);
                continue;
            }
            if (damage(to, adjusted) > 0.01D) {
                changed = true;
                warnSpread(player, from, to, fromUse);
                if (health(to) <= 0.01D) {
                    changed |= spreadToAdjacent(player, to, adjusted * 0.35D, fromUse, visited, depth + 1);
                }
            }
        }
        return changed;
    }

    private double useSeverity(BodyPart part) {
        double ratio = percent(part);
        if (health(part) <= 0.01D) {
            return 1.0D;
        }
        if (ratio < 0.30D) {
            return 0.35D;
        }
        return 0.0D;
    }

    private static BodyPart[] adjacentParts(BodyPart part) {
        return switch (part) {
            case HEAD -> new BodyPart[]{BodyPart.TORSO};
            case TORSO -> new BodyPart[]{BodyPart.HEAD, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG};
            case LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG -> new BodyPart[]{BodyPart.TORSO};
        };
    }

    private void syncCapacityFromVanillaMax(Player player) {
        double currentMax = Math.max(1.0D, player.getMaxHealth());
        if (Math.abs(currentMax - vanillaMaxHealth) <= 0.01D) {
            return;
        }
        double oldScale = vanillaMaxScale();
        vanillaMaxHealth = currentMax;
        double newScale = vanillaMaxScale();
        for (BodyPart part : BodyPart.values()) {
            double oldMaximum = baseMaxHealth(part) * oldScale;
            double newMaximum = baseMaxHealth(part) * newScale;
            health[part.ordinal()] = BodyHealthMath.rescalePreservingRatio(health(part), oldMaximum, newMaximum);
        }
    }

    private double healWithOverflow(BodyPart part, double amount) {
        BodyHealthMath.HealingStep step = BodyHealthMath.applyHealing(health(part), maxHealth(part), amount);
        setHealth(part, step.health());
        return step.remaining();
    }

    private double vanillaMaxScale() {
        return Math.max(1.0D, vanillaMaxHealth) / BASE_VANILLA_HEALTH;
    }

    private static double baseMaxHealth(BodyPart part) {
        return part.maxHealth;
    }

    private static String healthKey(BodyPart part) {
        return PREFIX + part.id + ".health";
    }

    private static final BodyPart[] HEALING_SPREAD_ORDER = {
            BodyPart.TORSO,
            BodyPart.LEFT_ARM,
            BodyPart.RIGHT_ARM,
            BodyPart.LEFT_LEG,
            BodyPart.RIGHT_LEG
    };

    @Override
    public String toString() {
        return "HumanBodyStatus" + Arrays.toString(health);
    }
}
