package com.xxsx.earthhuman;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class HumanEvents {
    private HumanEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }

        HumanStatus status = HumanStatus.get(player);
        if (player.isCreative() || player.isSpectator()) {
            status.hydration = Math.min(100.0D, status.hydration + 0.05D);
            status.fatigue = Math.max(0.0D, status.fatigue - 0.15D);
            status.bodyTemperature += (37.0D - status.bodyTemperature) * 0.03D;
            status.save(player);
            HumanNetwork.sync(player, status);
            return;
        }

        double movement = horizontalSpeedSqr(player.getDeltaMovement());
        boolean moving = movement > 0.0025D;
        boolean sprinting = player.isSprinting();
        boolean resting = !moving && player.onGround();
        ArcanaHumanBridge arcana = ArcanaHumanBridge.read(player);
        arcana.applyPassiveBodySupport(player);

        double hydrationLoss = 0.010D;
        if (sprinting) {
            hydrationLoss += 0.035D;
        } else if (moving) {
            hydrationLoss += 0.010D;
        }
        if (player.level().dimension() == Level.NETHER) {
            hydrationLoss += 0.030D;
        }
        status.hydration -= hydrationLoss * arcana.hydrationLossMultiplier();

        if (player.isSleeping()) {
            status.fatigue -= 0.60D * arcana.recoveryMultiplier();
        } else if (sprinting) {
            status.fatigue += 0.070D * arcana.fatigueGainMultiplier();
        } else if (moving) {
            status.fatigue += 0.018D * arcana.fatigueGainMultiplier();
        } else if (resting) {
            status.fatigue -= 0.055D * arcana.recoveryMultiplier();
        }
        if (status.hydration < 25.0D) {
            status.fatigue += 0.020D * arcana.fatigueGainMultiplier();
        }

        double targetTemperature = 37.0D;
        if (player.level().dimension() == Level.NETHER) {
            targetTemperature += 1.10D;
        }
        if (player.isInWaterOrRain() || player.level().isRainingAt(player.blockPosition())) {
            targetTemperature -= 0.55D;
        }
        if (status.hydration < 15.0D && targetTemperature > 37.0D) {
            targetTemperature += 0.35D;
        }
        double temperatureDrift = Math.abs(targetTemperature - 37.0D) > 0.05D
                ? 0.018D * arcana.temperatureStressMultiplier()
                : 0.018D * arcana.recoveryMultiplier();
        status.bodyTemperature += (targetTemperature - status.bodyTemperature) * temperatureDrift;

        warnIfNeeded(player, status);
        applyGentleConsequences(player, status, arcana);
        status.save(player);
        HumanNetwork.sync(player, status);
    }

    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) {
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

    public static void onPlayerClone(PlayerEvent.Clone event) {
        HumanStatus.get(event.getOriginal()).copyTo(event.getEntity());
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HumanStatus.get(player).save(player);
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
        int now = player.tickCount;
        if (now - status.lastWarningTick < 600) {
            return;
        }

        if (status.hydration < 20.0D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.warning.thirst"));
            status.lastWarningTick = now;
        } else if (status.fatigue > 80.0D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.warning.fatigue"));
            status.lastWarningTick = now;
        } else if (status.bodyTemperature < 35.5D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.warning.cold"));
            status.lastWarningTick = now;
        } else if (status.bodyTemperature > 38.2D) {
            player.sendSystemMessage(Component.translatable("message.earth_human.warning.heat"));
            status.lastWarningTick = now;
        }
    }

    private static void applyGentleConsequences(ServerPlayer player, HumanStatus status, ArcanaHumanBridge arcana) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (status.hydration < 18.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, status.hydration < 8.0D ? 1 : 0, true, false, true));
            player.causeFoodExhaustion((float) (0.08F * arcana.foodExhaustionMultiplier()));
            if (status.hydration < 5.0D && player.tickCount % 100 == 0) {
                player.hurtServer(level, level.damageSources().dryOut(), 1.0F);
            }
        }
        if (status.fatigue > 75.0D) {
            double bufferedFatigue = status.fatigue - arcana.adaptationScore() * 2.0D;
            int amplifier = bufferedFatigue > 92.0D ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, amplifier, true, false, true));
            if (bufferedFatigue > 88.0D) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, true, false, true));
            }
        }
        if (status.bodyTemperature < 34.8D) {
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
}
