package com.xxsx.earthhuman;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class HumanEvents {
    private static final String MOVEMENT_PREFIX = EarthHuman.MODID + ".movement.";
    private static final String LAST_ON_GROUND = MOVEMENT_PREFIX + "last_on_ground";
    private static final String LAST_JUMP_TIME = MOVEMENT_PREFIX + "last_jump_game_time";
    private static final String LAST_ACTIVE_TIME = MOVEMENT_PREFIX + "last_active_game_time";
    private static final String EXERTION_LOAD = MOVEMENT_PREFIX + "exertion_load";
    private static final String LAST_TRACKED_HEALTH = MOVEMENT_PREFIX + "last_tracked_health";
    private static final String SLEEP_PREFIX = EarthHuman.MODID + ".sleep.";
    private static final String SLEEP_ACTIVE = SLEEP_PREFIX + "active";
    private static final String SLEEP_NAP = SLEEP_PREFIX + "nap";
    private static final String SLEEP_START_GAME_TIME = SLEEP_PREFIX + "start_game_time";
    private static final String SLEEP_START_CLOCK_TIME = SLEEP_PREFIX + "start_clock_time";
    private static final double FALL_LEG_DAMAGE_MULTIPLIER = 1.35D;
    private static final double MELEE_REACH = 3.25D;
    private static final long DAY_TICKS = 24_000L;
    private static final int DAY_NAP_WAKE_TICKS = 80;
    private static final long MAX_SLEEP_RECOVERY_TICKS = DAY_TICKS;

    private HumanEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        HumanCareActions.tick(player);

        if (player.isCreative() || player.isSpectator()) {
            markGroundState(player);
            if (player.tickCount % 20 != 0) {
                return;
            }
            HumanStatus status = HumanStatus.get(player);
            if (EarthHumanConfig.hydrationEnabled()) {
                status.hydration = Math.min(100.0D, status.hydration + 0.05D);
            }
            if (EarthHumanConfig.fatigueEnabled()) {
                status.fatigue = Math.max(0.0D, status.fatigue - 0.15D);
            }
            if (EarthHumanConfig.temperatureEnabled()) {
                status.bodyTemperature += (37.0D - status.bodyTemperature) * 0.03D;
            }
            status.save(player);
            HumanNetwork.sync(player, status);
            return;
        }

        if (EarthHumanConfig.fatigueEnabled()) {
            trackSleepStart(player);
        } else {
            clearSleepTracking(player.getPersistentData());
        }

        HumanBodyStatus body = HumanBodyStatus.get(player);
        bridgePassiveVanillaHealthChange(player, body);
        if (EarthHumanConfig.bodyHealthEnabled() && handleJumpTakeoff(player, body)) {
            body.applyFunctionalEffects(player);
            body.save(player);
            HumanNetwork.syncBody(player, body);
        }
        if (player.tickCount % 20 != 0) {
            return;
        }

        HumanStatus status = HumanStatus.get(player);
        body.ensureHeadHealthLimit(player);

        double movement = horizontalSpeedSqr(player.getDeltaMovement());
        boolean moving = movement > 0.0025D;
        boolean sprinting = player.isSprinting();
        boolean resting = !moving && player.onGround();
        double exertionLoad = updateExertionLoad(player, moving, sprinting, player.isSleeping());
        boolean recentExertion = isRecentExertion(player, exertionLoad);
        if (moving && EarthHumanConfig.bodyHealthEnabled()) {
            body.spreadFromUse(player, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG, sprinting ? 0.28D : 0.12D);
            HumanExercise.applyPassiveUse(player, BodyPart.LEFT_LEG, sprinting ? 0.065D : 0.030D, sprinting ? 0.070D : 0.030D);
            HumanExercise.applyPassiveUse(player, BodyPart.RIGHT_LEG, sprinting ? 0.065D : 0.030D, sprinting ? 0.070D : 0.030D);
        }
        body.applyFunctionalEffects(player);
        ArcanaHumanBridge arcana = ArcanaHumanBridge.read(player);
        arcana.applyPassiveBodySupport(player);
        HumanExercise.recover(player, player.isSleeping(), resting && !recentExertion);

        if (EarthHumanConfig.hydrationEnabled()) {
            double hydrationLoss = 0.010D;
            if (sprinting) {
                hydrationLoss += 0.035D;
            } else if (moving) {
                hydrationLoss += 0.010D;
            }
            if (player.level().dimension() == Level.NETHER) {
                hydrationLoss += 0.030D;
            }
            status.hydration -= hydrationLoss
                    * arcana.hydrationLossMultiplier()
                    * EarthHumanConfig.hydrationLossMultiplier();
        }

        if (EarthHumanConfig.fatigueEnabled()) {
            double gainMultiplier = arcana.fatigueGainMultiplier() * EarthHumanConfig.fatigueGainMultiplier();
            double recoveryMultiplier = arcana.recoveryMultiplier() * EarthHumanConfig.recoveryMultiplier();
            if (player.isSleeping()) {
                status.fatigue -= 1.25D * recoveryMultiplier;
            } else if (sprinting) {
                status.fatigue += (0.46D + exertionLoad * 0.006D) * gainMultiplier;
            } else if (moving) {
                status.fatigue += (0.13D + exertionLoad * 0.0025D) * gainMultiplier;
            } else if (resting) {
                if (recentExertion) {
                    status.fatigue -= 0.025D * recoveryMultiplier;
                } else {
                    status.fatigue -= 0.14D * recoveryMultiplier;
                }
            }
            if (EarthHumanConfig.hydrationEnabled() && status.hydration < 25.0D) {
                status.fatigue += 0.055D * gainMultiplier;
            }
            if (player.hasEffect(EarthHumanEffects.MUSCLE_STRAIN)) {
                status.fatigue += 0.070D * gainMultiplier;
            }
        }

        if (EarthHumanConfig.temperatureEnabled()) {
            double targetTemperature = 37.0D;
            if (player.level().dimension() == Level.NETHER) {
                targetTemperature += 1.10D;
            }
            if (player.isInWaterOrRain() || player.level().isRainingAt(player.blockPosition())) {
                targetTemperature -= 0.55D;
            }
            if (EarthHumanConfig.hydrationEnabled() && status.hydration < 15.0D && targetTemperature > 37.0D) {
                targetTemperature += 0.35D;
            }
            double temperatureDrift = Math.abs(targetTemperature - 37.0D) > 0.05D
                    ? 0.018D * arcana.temperatureStressMultiplier()
                    : 0.018D * arcana.recoveryMultiplier();
            status.bodyTemperature += (targetTemperature - status.bodyTemperature)
                    * temperatureDrift
                    * EarthHumanConfig.temperatureRateMultiplier();
        }

        warnIfNeeded(player, status);
        applyGentleConsequences(player, status, arcana);
        body.save(player);
        status.save(player);
        HumanNetwork.sync(player, status);
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.isCreative()
                || player.isSpectator()
                || !EarthHumanConfig.bodyHealthEnabled()
                || EarthHumanConfig.healthBridgeMode() != EarthHumanRules.HealthBridgeMode.HEAD_BAR
                || shouldBypassBodySystem(event.getSource())) {
            return;
        }
        float damage = event.getNewDamage();
        if (!Float.isFinite(damage) || damage <= 0.0F) {
            return;
        }

        HumanBodyStatus body = HumanBodyStatus.get(player);
        body.ensureHeadHealthLimit(player);
        if (isFallLike(event.getSource())) {
            applyFallDamageToLegs(player, body, damage);
            body.applyFunctionalEffects(player);
            body.save(player);
            HumanNetwork.syncBody(player, body);
            event.setNewDamage(0.0F);
            return;
        }
        if (event.getSource().is(HumanDamageTags.FULL_BODY)) {
            applyHeadBarFullBodyDamage(player, body, damage);
            body.applyFunctionalEffects(player);
            body.save(player);
            HumanNetwork.syncBody(player, body);
            event.setNewDamage(0.0F);
            return;
        }
        BodyPart part = chooseDamagedPart(player, event.getSource(), damage);
        if (part == BodyPart.HEAD) {
            return;
        }

        double routedDamage = damage * EarthHumanConfig.bodyDamageMultiplier();
        double before = body.health(part);
        body.damage(part, routedDamage);
        body.spreadFromImpact(player, part, routedDamage, before);
        body.warnDirectDamage(player, part, before);
        body.applyFunctionalEffects(player);
        body.save(player);
        HumanNetwork.syncBody(player, body);
        event.setNewDamage(0.0F);
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getPersistentData().putFloat(LAST_TRACKED_HEALTH, player.getHealth());
        if (player.isCreative() || player.isSpectator() || !EarthHumanConfig.bodyHealthEnabled()) {
            return;
        }

        HumanBodyStatus body = HumanBodyStatus.get(player);
        if (EarthHumanConfig.healthBridgeMode() == EarthHumanRules.HealthBridgeMode.HEAD_BAR) {
            body.pullHeadFromVanilla(player);
            body.save(player);
            HumanNetwork.syncBody(player, body);
            return;
        }
        if (shouldBypassBodySystem(event.getSource())) {
            return;
        }
        float damage = event.getHealthDamage();
        if (!Float.isFinite(damage) || damage <= 0.0F) {
            return;
        }

        double headBefore = body.health(BodyPart.HEAD);
        body.pullHeadDamageFromVanilla(player);
        if (isFallLike(event.getSource())) {
            applyCompatibleFallDamageToLegs(player, body, damage);
        } else if (event.getSource().is(HumanDamageTags.FULL_BODY)) {
            applyCompatibleFullBodyDamage(player, body, damage);
        } else {
            BodyPart part = chooseDamagedPart(player, event.getSource(), damage);
            if (part == BodyPart.HEAD) {
                body.warnDirectDamage(player, BodyPart.HEAD, headBefore);
            } else {
                applyCompatiblePartDamage(player, body, part, damage);
            }
        }
        body.applyFunctionalEffects(player);
        body.save(player);
        HumanNetwork.syncBody(player, body);
    }

    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        float amount = event.getAmount();
        if (!Float.isFinite(amount) || amount <= 0.0F) {
            return;
        }

        float expectedHealth = Math.min(player.getMaxHealth(), player.getHealth() + amount);
        if (EarthHumanConfig.bodyHealthEnabled()) {
            HumanBodyStatus body = HumanBodyStatus.get(player);
            if (body.bridgeVanillaHealing(player, amount)) {
                body.save(player);
                HumanNetwork.syncBody(player, body);
            }
            if (EarthHumanConfig.healthBridgeMode() == EarthHumanRules.HealthBridgeMode.HEAD_BAR) {
                event.setAmount(0.0F);
                expectedHealth = player.getHealth();
            }
        }
        player.getPersistentData().putFloat(LAST_TRACKED_HEALTH, expectedHealth);
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        HumanBodyStatus body = HumanBodyStatus.get(player);
        if (EarthHumanConfig.bodyHealthEnabled()) {
            body.spreadFromUse(player, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, 0.18D);
        }
        HumanExercise.applyPassiveUse(player, BodyPart.LEFT_ARM, 0.030D, 0.038D);
        HumanExercise.applyPassiveUse(player, BodyPart.RIGHT_ARM, 0.030D, 0.038D);
        if (EarthHumanConfig.bodyEffectsEnabled()) {
            float multiplier = armUseSpeedMultiplier(body);
            if (player.hasEffect(EarthHumanEffects.MUSCLE_STRAIN)) {
                multiplier *= 0.86F;
            }
            if (multiplier < 0.99F) {
                event.setNewSpeed(event.getNewSpeed() * multiplier);
            }
        }
        body.save(player);
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        HumanBodyStatus body = HumanBodyStatus.get(player);
        if (EarthHumanConfig.bodyHealthEnabled()) {
            body.spreadFromUse(player, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, 0.26D);
        }
        HumanExercise.applyPassiveUse(player, BodyPart.LEFT_ARM, 0.045D, 0.055D);
        HumanExercise.applyPassiveUse(player, BodyPart.RIGHT_ARM, 0.045D, 0.055D);
        HumanExercise.applyPassiveUse(player, BodyPart.TORSO, 0.020D, 0.030D);
        body.applyFunctionalEffects(player);
        body.save(player);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handleInteractionUse(event.getEntity(), 0.10D);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        handleInteractionUse(event.getEntity(), 0.12D);
    }

    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || !EarthHumanConfig.hydrationEnabled()) {
            return;
        }
        double restored = hydrationRestoredBy(event.getItem());
        if (restored <= 0.0D) {
            return;
        }

        HumanStatus status = HumanStatus.get(player);
        double before = status.hydration;
        status.hydration = Math.min(100.0D, status.hydration + restored);
        status.save(player);
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer && status.hydration > before + 0.1D) {
            HumanNetwork.sync(serverPlayer, status);
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.earth_human.hydration_restored",
                    Math.round(status.hydration)));
        }
    }

    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        ServerPlayer player = event.getEntity();
        if (player.isCreative() || player.isSpectator() || !EarthHumanConfig.fatigueEnabled()) {
            return;
        }
        Player.BedSleepingProblem problem = event.getProblem();
        if (problem == null) {
            markSleepStart(player, false);
            return;
        }
        if (!isDaytimeRest(player) || !isDaytimeBedRuleFailure(event)) {
            return;
        }
        event.setProblem(null);
        markSleepStart(player, true);
        player.sendSystemMessage(Component.translatable("message.earth_human.sleep.nap_start"));
    }

    public static void onCanContinueSleeping(CanContinueSleepingEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.getPersistentData().getBooleanOr(SLEEP_NAP, false)) {
            return;
        }
        if (player.getSleepTimer() >= DAY_NAP_WAKE_TICKS) {
            player.stopSleepInBed(true, true);
            return;
        }
        Player.BedSleepingProblem problem = event.getProblem();
        if (canUseBedForRest(problem)) {
            event.setContinueSleeping(true);
        }
    }

    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.getPersistentData().getBooleanOr(SLEEP_ACTIVE, false)) {
            return;
        }
        finishSleepRecovery(player);
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        HumanStatus.get(event.getOriginal()).copyTo(event.getEntity());
        HumanBodyStatus body = HumanBodyStatus.get(event.getOriginal());
        if (event.isWasDeath()) {
            body.stabilizeAfterDeathRespawn(event.getEntity());
        }
        body.copyTo(event.getEntity());
        HumanTrainingStatus.get(event.getOriginal()).copyTo(event.getEntity());
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HumanStatus.get(player).save(player);
        HumanBodyStatus body = HumanBodyStatus.get(player);
        if (!player.isCreative() && !player.isSpectator()) {
            body.ensureHeadHealthLimit(player);
        }
        body.save(player);
        player.getPersistentData().putFloat(LAST_TRACKED_HEALTH, player.getHealth());
        HumanNetwork.sync(player);
        player.sendSystemMessage(Component.translatable("message.earth_human.loaded"));
        player.sendSystemMessage(Component.translatable("message.earth_human.quick_help"));
    }

    public static double hydrationRestoredBy(ItemStack stack) {
        if (stack.is(Items.POTION)) {
            return 35.0D;
        }
        if (stack.is(Items.MILK_BUCKET)) {
            return 25.0D;
        }
        if (stack.is(Items.HONEY_BOTTLE)) {
            return 12.0D;
        }
        if (stack.is(Items.MUSHROOM_STEW) || stack.is(Items.RABBIT_STEW)
                || stack.is(Items.BEETROOT_SOUP) || stack.is(Items.SUSPICIOUS_STEW)) {
            return 18.0D;
        }
        return 0.0D;
    }

    private static void warnIfNeeded(ServerPlayer player, HumanStatus status) {
        long now = player.level().getGameTime();
        if (now - status.lastWarningGameTime < 600L) {
            return;
        }

        if (EarthHumanConfig.hydrationEnabled() && status.hydration < 20.0D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.warning.thirst"));
            status.lastWarningGameTime = now;
        } else if (EarthHumanConfig.fatigueEnabled() && status.fatigue > 80.0D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.warning.fatigue"));
            status.lastWarningGameTime = now;
        } else if (EarthHumanConfig.temperatureEnabled() && status.bodyTemperature < 35.5D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.warning.cold"));
            status.lastWarningGameTime = now;
        } else if (EarthHumanConfig.temperatureEnabled() && status.bodyTemperature > 38.2D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.warning.heat"));
            status.lastWarningGameTime = now;
        }
    }

    private static void trackSleepStart(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (player.isSleeping()) {
            if (!data.getBooleanOr(SLEEP_ACTIVE, false)) {
                markSleepStart(player, isDaytimeRest(player));
            }
            if (data.getBooleanOr(SLEEP_NAP, false) && player.getSleepTimer() >= DAY_NAP_WAKE_TICKS) {
                player.stopSleepInBed(true, true);
            }
        } else if (data.getBooleanOr(SLEEP_ACTIVE, false)) {
            finishSleepRecovery(player);
        }
    }

    private static void markSleepStart(ServerPlayer player, boolean nap) {
        CompoundTag data = player.getPersistentData();
        data.putBoolean(SLEEP_ACTIVE, true);
        data.putBoolean(SLEEP_NAP, nap);
        data.putLong(SLEEP_START_GAME_TIME, player.level().getGameTime());
        data.putLong(SLEEP_START_CLOCK_TIME, clockTime(player.level()));
    }

    private static void finishSleepRecovery(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!EarthHumanConfig.fatigueEnabled()) {
            clearSleepTracking(data);
            return;
        }
        long startGameTime = data.getLongOr(SLEEP_START_GAME_TIME, player.level().getGameTime());
        long startClockTime = data.getLongOr(SLEEP_START_CLOCK_TIME, clockTime(player.level()));
        boolean nap = data.getBooleanOr(SLEEP_NAP, false);

        long elapsedTicks = Math.max(0L, player.level().getGameTime() - startGameTime);
        long clockDelta = positiveClockDelta(startClockTime, clockTime(player.level()));
        long recoveryTicks = Math.max(elapsedTicks, clockDelta);
        recoveryTicks = Math.max(recoveryTicks, player.getSleepTimer());
        recoveryTicks = Math.min(recoveryTicks, MAX_SLEEP_RECOVERY_TICKS);

        HumanStatus status = HumanStatus.get(player);
        double before = status.fatigue;
        ArcanaHumanBridge arcana = ArcanaHumanBridge.read(player);
        double recovery = sleepRecoveryForTicks(recoveryTicks, nap)
                * arcana.recoveryMultiplier()
                * EarthHumanConfig.recoveryMultiplier();
        status.fatigue = HumanStatus.clamp(status.fatigue - recovery, 0.0D, 100.0D);
        status.save(player);
        HumanNetwork.sync(player, status);
        clearSleepTracking(data);

        double recovered = before - status.fatigue;
        if (recovered >= 0.5D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.sleep.recovered", Math.round(recovered), Math.round(status.fatigue)));
        }
    }

    private static boolean canUseBedForRest(Player.BedSleepingProblem problem) {
        return problem == null || problem == Player.BedSleepingProblem.OTHER_PROBLEM;
    }

    private static boolean isDaytimeBedRuleFailure(CanPlayerSleepEvent event) {
        BedRule rule = event.getLevel().environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, event.getPos());
        Player.BedSleepingProblem problem = event.getProblem();
        return problem != null
                && !rule.explodes()
                && rule.canSetSpawn(event.getLevel())
                && !rule.canSleep(event.getLevel())
                && problem.equals(rule.asProblem());
    }

    private static boolean isDaytimeRest(ServerPlayer player) {
        long dayTime = Math.floorMod(clockTime(player.level()), DAY_TICKS);
        return dayTime >= 1_000L && dayTime < 12_500L;
    }

    private static long clockTime(ServerLevel level) {
        return level.getDefaultClockTime();
    }

    private static long positiveClockDelta(long start, long end) {
        long raw = end - start;
        if (raw >= 0L) {
            return raw;
        }
        long startDay = Math.floorMod(start, DAY_TICKS);
        long endDay = Math.floorMod(end, DAY_TICKS);
        return Math.floorMod(endDay - startDay, DAY_TICKS);
    }

    private static double sleepRecoveryForTicks(long ticks, boolean nap) {
        double seconds = ticks / 20.0D;
        double perSecond = nap ? 0.45D : 0.12D;
        double cap = nap ? 10.0D : 100.0D;
        return Math.min(cap, seconds * perSecond);
    }

    private static void clearSleepTracking(CompoundTag data) {
        data.remove(SLEEP_ACTIVE);
        data.remove(SLEEP_NAP);
        data.remove(SLEEP_START_GAME_TIME);
        data.remove(SLEEP_START_CLOCK_TIME);
    }

    private static void applyGentleConsequences(ServerPlayer player, HumanStatus status, ArcanaHumanBridge arcana) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (EarthHumanConfig.hydrationEnabled() && status.hydration < 18.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, status.hydration < 8.0D ? 1 : 0, true, false, true));
            player.causeFoodExhaustion((float) (0.08F * arcana.foodExhaustionMultiplier()));
            if (status.hydration < 5.0D && player.tickCount % 100 == 0) {
                player.hurtServer(level, level.damageSources().dryOut(), 1.0F);
            }
        }
        if (EarthHumanConfig.fatigueEnabled() && status.fatigue > 75.0D) {
            double bufferedFatigue = status.fatigue - arcana.adaptationScore() * 2.0D;
            int amplifier = bufferedFatigue > 92.0D ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, amplifier, true, false, true));
            if (bufferedFatigue > 88.0D) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, true, false, true));
            }
        }
        if (EarthHumanConfig.temperatureEnabled() && status.bodyTemperature < 34.8D) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, status.bodyTemperature < 34.0D ? 1 : 0, true, false, true));
            if (status.bodyTemperature < 33.5D && player.tickCount % 120 == 0) {
                player.hurtServer(level, level.damageSources().freeze(), 1.0F);
            }
        } else if (status.bodyTemperature > 39.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, status.bodyTemperature > 40.0D ? 1 : 0, true, false, true));
            player.causeFoodExhaustion((float) (0.06F * arcana.foodExhaustionMultiplier()));
            if (status.bodyTemperature > 40.5D && player.tickCount % 120 == 0) {
                player.hurtServer(level, level.damageSources().hotFloor(), 1.0F);
            }
        }
    }

    private static double horizontalSpeedSqr(Vec3 vec) {
        return vec.x * vec.x + vec.z * vec.z;
    }

    private static void bridgePassiveVanillaHealthChange(ServerPlayer player, HumanBodyStatus body) {
        CompoundTag data = player.getPersistentData();
        float current = player.getHealth();
        float previous = data.getFloatOr(LAST_TRACKED_HEALTH, current);
        if (EarthHumanConfig.bodyHealthEnabled()
                && current > previous + 0.05F
                && body.bridgeVanillaHealing(player, current - previous)) {
            body.save(player);
            HumanNetwork.syncBody(player, body);
        }
        data.putFloat(LAST_TRACKED_HEALTH, player.getHealth());
    }

    private static boolean shouldBypassBodySystem(DamageSource source) {
        return source.is(HumanDamageTags.BYPASSES_BODY)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.GENERIC_KILL);
    }

    private static boolean isFallLike(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.FLY_INTO_WALL);
    }

    private static BodyPart chooseDamagedPart(ServerPlayer player, DamageSource source, float damage) {
        if (isFallLike(source)) {
            return alternating(player, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        }
        if (source.is(HumanDamageTags.HEAD_PRIORITY)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.DRAGON_BREATH)) {
            return BodyPart.HEAD;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            BodyPart explosionPart = chooseExplosionImpactPart(player, source);
            if (explosionPart != null) {
                return explosionPart;
            }
        }
        if (source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypes.DRY_OUT)) {
            return chooseEnvironmentalContactPart(player, source);
        }
        if (source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.STING)
                || source.is(DamageTypes.SWEET_BERRY_BUSH)) {
            return chooseLowContactPart(player, source);
        }

        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            BodyPart projectilePart = chooseProjectileImpactPart(player, source);
            if (projectilePart != null) {
                return projectilePart;
            }
        }

        BodyPart meleePart = chooseMeleeImpactPart(player, source);
        if (meleePart != null) {
            return meleePart;
        }

        int roll = Math.floorMod(player.tickCount * 31 + source.getMsgId().hashCode() + Math.round(damage * 10.0F), 100);
        if (roll < 10) return BodyPart.HEAD;
        if (roll < 45) return BodyPart.TORSO;
        if (roll < 59) return BodyPart.LEFT_ARM;
        if (roll < 73) return BodyPart.RIGHT_ARM;
        if (roll < 87) return BodyPart.LEFT_LEG;
        return BodyPart.RIGHT_LEG;
    }

    private static BodyPart chooseProjectileImpactPart(ServerPlayer player, DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct == null || direct == player || direct instanceof LivingEntity) {
            return null;
        }
        AABB playerBox = player.getBoundingBox().inflate(0.20D);
        Vec3 hit = direct.position();
        if (!playerBox.contains(hit)) {
            Vec3 velocity = direct.getDeltaMovement();
            if (velocity.lengthSqr() > 1.0E-6D) {
                Vec3 start = hit.subtract(velocity.normalize().scale(2.0D));
                Vec3 end = hit.add(velocity.normalize().scale(2.0D));
                hit = playerBox.clip(start, end).orElseGet(() -> estimateImpactPoint(player, start, end));
            } else {
                hit = closestPointOnPlayerBox(player, hit);
            }
        }
        return bodyPartFromImpactPoint(player, hit);
    }

    private static BodyPart chooseMeleeImpactPart(ServerPlayer player, DamageSource source) {
        Entity attacker = source.getDirectEntity();
        if (attacker == null) {
            attacker = source.getEntity();
        }
        if (!(attacker instanceof LivingEntity livingAttacker) || attacker == player) {
            return null;
        }
        if (attacker.distanceToSqr(player) > 25.0D) {
            return null;
        }

        Vec3 start = livingAttacker.getEyePosition();
        Vec3 look = livingAttacker.getViewVector(1.0F).normalize();
        Vec3 end = start.add(look.scale(MELEE_REACH));
        AABB playerBox = player.getBoundingBox().inflate(0.12D);
        Vec3 hit = playerBox.clip(start, end).orElseGet(() -> estimateImpactPoint(player, start, end));
        return bodyPartFromImpactPoint(player, hit);
    }

    private static BodyPart chooseExplosionImpactPart(ServerPlayer player, DamageSource source) {
        Entity direct = source.getDirectEntity();
        Entity owner = source.getEntity();
        Entity origin = direct != null && direct != player ? direct : owner;
        if (origin == null || origin == player) {
            return BodyPart.TORSO;
        }

        Vec3 blast = origin.position();
        Vec3 hit = closestPointOnPlayerBox(player, blast);
        double vertical = verticalRatio(player, hit);
        double distance = Math.sqrt(Math.max(0.0D, origin.distanceToSqr(player)));

        if (distance < 1.75D) {
            return BodyPart.TORSO;
        }
        if (vertical >= 0.78D) {
            return BodyPart.HEAD;
        }
        if (vertical < 0.38D) {
            return sidePart(player, hit, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        }
        if (Math.abs(localRightOffset(player, hit)) > 0.24D) {
            return sidePart(player, hit, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM);
        }
        return BodyPart.TORSO;
    }

    private static BodyPart chooseEnvironmentalContactPart(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FREEZING) || source.is(DamageTypes.DRY_OUT)) {
            return source.is(DamageTypeTags.IS_FREEZING)
                    ? alternating(player, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG)
                    : BodyPart.TORSO;
        }
        if (source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.CAMPFIRE)) {
            return alternating(player, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        }
        if (source.is(DamageTypes.LAVA)) {
            return player.isInLava() ? BodyPart.TORSO : alternating(player, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        }
        return player.onGround() ? alternating(player, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG) : BodyPart.TORSO;
    }

    private static BodyPart chooseLowContactPart(ServerPlayer player, DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct != null && direct != player) {
            Vec3 hit = closestPointOnPlayerBox(player, direct.position());
            return bodyPartFromImpactPoint(player, hit);
        }
        return player.onGround() ? alternating(player, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG) : alternating(player, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM);
    }

    private static Vec3 estimateImpactPoint(ServerPlayer player, Vec3 start, Vec3 end) {
        AABB box = player.getBoundingBox();
        Vec3 center = box.getCenter();
        Vec3 line = end.subtract(start);
        double lineLenSqr = line.lengthSqr();
        if (lineLenSqr <= 1.0E-6D) {
            return center;
        }
        double t = HumanStatus.clamp(center.subtract(start).dot(line) / lineLenSqr, 0.0D, 1.0D);
        Vec3 projected = start.add(line.scale(t));
        return new Vec3(
                HumanStatus.clamp(projected.x, box.minX, box.maxX),
                HumanStatus.clamp(projected.y, box.minY, box.maxY),
                HumanStatus.clamp(projected.z, box.minZ, box.maxZ));
    }

    private static BodyPart bodyPartFromImpactPoint(ServerPlayer player, Vec3 hit) {
        double vertical = verticalRatio(player, hit);
        double side = localRightOffset(player, hit);

        if (vertical >= 0.78D) {
            return BodyPart.HEAD;
        }
        if (vertical < 0.42D) {
            return sidePart(player, hit, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        }
        if (Math.abs(side) > 0.28D) {
            return sidePart(player, hit, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM);
        }
        return BodyPart.TORSO;
    }

    private static double verticalRatio(ServerPlayer player, Vec3 hit) {
        AABB box = player.getBoundingBox();
        double height = Math.max(0.1D, box.getYsize());
        return HumanStatus.clamp((hit.y - box.minY) / height, 0.0D, 1.0D);
    }

    private static double localRightOffset(ServerPlayer player, Vec3 hit) {
        Vec3 center = player.getBoundingBox().getCenter();
        Vec3 relative = hit.subtract(center);
        double yaw = Math.toRadians(player.getYRot());
        Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        return relative.dot(right);
    }

    private static BodyPart sidePart(ServerPlayer player, Vec3 hit, BodyPart left, BodyPart right) {
        return localRightOffset(player, hit) < 0.0D ? left : right;
    }

    private static Vec3 closestPointOnPlayerBox(ServerPlayer player, Vec3 point) {
        AABB box = player.getBoundingBox();
        return new Vec3(
                HumanStatus.clamp(point.x, box.minX, box.maxX),
                HumanStatus.clamp(point.y, box.minY, box.maxY),
                HumanStatus.clamp(point.z, box.minZ, box.maxZ));
    }

    private static BodyPart alternating(ServerPlayer player, BodyPart first, BodyPart second) {
        return (player.tickCount & 1) == 0 ? first : second;
    }

    private static void markGroundState(ServerPlayer player) {
        player.getPersistentData().putInt(LAST_ON_GROUND, player.onGround() ? 1 : 0);
    }

    private static boolean handleJumpTakeoff(ServerPlayer player, HumanBodyStatus body) {
        CompoundTag data = player.getPersistentData();
        boolean onGround = player.onGround();
        boolean wasOnGround = data.getIntOr(LAST_ON_GROUND, onGround ? 1 : 0) != 0;
        boolean jumped = wasOnGround
                && !onGround
                && player.getDeltaMovement().y > 0.18D
                && player.level().getGameTime() - data.getLongOr(LAST_JUMP_TIME, -1_000_000L) > 6L;
        data.putInt(LAST_ON_GROUND, onGround ? 1 : 0);
        if (!jumped) {
            return false;
        }

        data.putLong(LAST_JUMP_TIME, player.level().getGameTime());
        addExertion(player, player.isSprinting() ? 1.35D : 0.85D);
        HumanExercise.applyPassiveUse(player, BodyPart.LEFT_LEG, 0.018D, 0.030D);
        HumanExercise.applyPassiveUse(player, BodyPart.RIGHT_LEG, 0.018D, 0.030D);
        double base = player.isSprinting() ? 0.030D : 0.018D;
        boolean changed = damageLegFromJump(player, body, BodyPart.LEFT_LEG, base);
        changed |= damageLegFromJump(player, body, BodyPart.RIGHT_LEG, base);
        changed |= body.spreadFromUse(player, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG, 0.16D);
        return changed;
    }

    private static boolean damageLegFromJump(ServerPlayer player, HumanBodyStatus body, BodyPart leg, double base) {
        double healthPercent = body.percent(leg);
        double amount = base
                * (1.0D + (1.0D - healthPercent) * 2.0D)
                * EarthHumanConfig.bodyDamageMultiplier();
        double before = body.health(leg);
        boolean changed = body.damage(leg, amount) > 0.001D;
        if (changed) {
            body.warnDirectDamage(player, leg, before);
        }
        return changed;
    }

    private static void applyFallDamageToLegs(ServerPlayer player, HumanBodyStatus body, double vanillaDamage) {
        double total = Math.max(0.0D, vanillaDamage)
                * FALL_LEG_DAMAGE_MULTIPLIER
                * EarthHumanConfig.bodyDamageMultiplier();
        double leftAmount = total * 0.50D;
        double rightAmount = total - leftAmount;
        applyLegImpact(player, body, BodyPart.LEFT_LEG, leftAmount);
        applyLegImpact(player, body, BodyPart.RIGHT_LEG, rightAmount);
    }

    private static void applyLegImpact(ServerPlayer player, HumanBodyStatus body, BodyPart leg, double amount) {
        double before = body.health(leg);
        double applied = body.damage(leg, amount);
        if (applied <= 0.01D && before > 0.01D) {
            return;
        }
        body.spreadFromImpact(player, leg, amount, before);
        if (applied > 0.01D) {
            body.warnDirectDamage(player, leg, before);
        }
    }

    private static void applyCompatibleFallDamageToLegs(ServerPlayer player, HumanBodyStatus body, double vanillaDamage) {
        double strengthened = Math.max(0.0D, vanillaDamage) * FALL_LEG_DAMAGE_MULTIPLIER;
        applyCompatiblePartDamage(player, body, BodyPart.LEFT_LEG, strengthened);
        applyCompatiblePartDamage(player, body, BodyPart.RIGHT_LEG, strengthened);
    }

    private static void applyCompatibleFullBodyDamage(ServerPlayer player, HumanBodyStatus body, double vanillaDamage) {
        for (BodyPart part : BodyPart.values()) {
            if (part != BodyPart.HEAD) {
                applyCompatiblePartDamage(player, body, part, vanillaDamage);
            }
        }
    }

    private static void applyHeadBarFullBodyDamage(ServerPlayer player, HumanBodyStatus body, double vanillaDamage) {
        for (BodyPart part : BodyPart.values()) {
            double routedDamage = BodyHealthMath.compatiblePartDamage(
                    vanillaDamage,
                    body.maxHealth(part),
                    player.getMaxHealth(),
                    EarthHumanConfig.bodyDamageMultiplier());
            if (routedDamage <= 0.001D) {
                continue;
            }
            double before = body.health(part);
            body.damage(part, routedDamage);
            body.warnDirectDamage(player, part, before);
        }
        body.syncVanillaFromHead(player);
    }

    private static void applyCompatiblePartDamage(ServerPlayer player, HumanBodyStatus body,
                                                  BodyPart part, double vanillaDamage) {
        double routedDamage = BodyHealthMath.compatiblePartDamage(
                vanillaDamage,
                body.maxHealth(part),
                player.getMaxHealth(),
                EarthHumanConfig.bodyDamageMultiplier());
        if (routedDamage <= 0.001D) {
            return;
        }
        double before = body.health(part);
        body.damage(part, routedDamage);
        body.spreadFromImpact(player, part, routedDamage, before);
        body.warnDirectDamage(player, part, before);
    }

    private static void handleInteractionUse(Player player, double baseAmount) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.isCreative() || serverPlayer.isSpectator()) {
            return;
        }
        HumanBodyStatus body = HumanBodyStatus.get(serverPlayer);
        if (EarthHumanConfig.bodyHealthEnabled()) {
            body.spreadFromUse(serverPlayer, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, baseAmount);
        }
        HumanExercise.applyPassiveUse(serverPlayer, BodyPart.LEFT_ARM, 0.010D, 0.018D);
        HumanExercise.applyPassiveUse(serverPlayer, BodyPart.RIGHT_ARM, 0.010D, 0.018D);
        body.save(serverPlayer);
    }

    private static double updateExertionLoad(ServerPlayer player, boolean moving, boolean sprinting, boolean sleeping) {
        CompoundTag data = player.getPersistentData();
        double load = HumanStatus.clamp(data.getDoubleOr(EXERTION_LOAD, 0.0D), 0.0D, 100.0D);
        if (sleeping) {
            load = Math.max(0.0D, load - 1.60D);
        } else if (sprinting) {
            load = Math.min(100.0D, load + 0.46D);
            data.putLong(LAST_ACTIVE_TIME, player.level().getGameTime());
        } else if (moving) {
            load = Math.min(100.0D, load + 0.18D);
            data.putLong(LAST_ACTIVE_TIME, player.level().getGameTime());
        } else if (player.onGround()) {
            boolean recent = player.level().getGameTime() - data.getLongOr(LAST_ACTIVE_TIME, -1_000_000L) < 20L * 90L;
            load = Math.max(0.0D, load - (recent || load > 35.0D ? 0.035D : 0.12D));
        }
        data.putDouble(EXERTION_LOAD, load);
        return load;
    }

    private static void addExertion(ServerPlayer player, double amount) {
        CompoundTag data = player.getPersistentData();
        double load = HumanStatus.clamp(data.getDoubleOr(EXERTION_LOAD, 0.0D) + Math.max(0.0D, amount), 0.0D, 100.0D);
        data.putDouble(EXERTION_LOAD, load);
        data.putLong(LAST_ACTIVE_TIME, player.level().getGameTime());
    }

    private static boolean isRecentExertion(ServerPlayer player, double load) {
        CompoundTag data = player.getPersistentData();
        return load > 18.0D || player.level().getGameTime() - data.getLongOr(LAST_ACTIVE_TIME, -1_000_000L) < 20L * 45L;
    }

    private static float armUseSpeedMultiplier(HumanBodyStatus body) {
        double left = body.percent(BodyPart.LEFT_ARM);
        double right = body.percent(BodyPart.RIGHT_ARM);
        boolean leftFailed = body.health(BodyPart.LEFT_ARM) <= 0.01D;
        boolean rightFailed = body.health(BodyPart.RIGHT_ARM) <= 0.01D;
        if (leftFailed && rightFailed) {
            return 0.20F;
        }
        if (leftFailed || rightFailed) {
            return 0.45F;
        }
        if (left < 0.30D && right < 0.30D) {
            return 0.55F;
        }
        if (left < 0.30D || right < 0.30D) {
            return 0.75F;
        }
        return 1.0F;
    }
}
