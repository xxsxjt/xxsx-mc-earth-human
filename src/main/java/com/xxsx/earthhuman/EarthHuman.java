package com.xxsx.earthhuman;

import com.xxsx.earthhuman.client.EarthHumanClient;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(EarthHuman.MODID)
public class EarthHuman {
    public static final String MODID = "earth_human";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EarthHuman(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, EarthHumanConfig.SPEC);
        EarthHumanEffects.register(modBus);
        EarthHumanItems.register(modBus);
        modBus.addListener(HumanNetwork::registerPayloads);
        NeoForge.EVENT_BUS.addListener(EarthHumanCommands::register);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, HumanEvents::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, HumanEvents::onLivingHeal);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onUseItemFinish);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onCanPlayerSleep);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onCanContinueSleeping);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onPlayerWakeUp);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(HumanEvents::onPlayerLogin);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            EarthHumanClient.register(modBus, modContainer);
        }
        LOGGER.info("[Earth Human] NeoForge 26.2 module loaded");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
