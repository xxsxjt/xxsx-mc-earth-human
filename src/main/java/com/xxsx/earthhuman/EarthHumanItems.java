package com.xxsx.earthhuman;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class EarthHumanItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EarthHuman.MODID);
    private static final List<DeferredItem<? extends Item>> TAB_ITEMS = new ArrayList<>();

    public static final DeferredItem<Item> CANTEEN = guided("canteen", props -> props.stacksTo(1));
    public static final DeferredItem<Item> FILLED_CANTEEN = consumable("filled_canteen", props -> props.stacksTo(1),
            ItemUseAnimation.DRINK, 45.0D, 0.0D, 0.0D, () -> CANTEEN.get());
    public static final DeferredItem<Item> WATER_PURIFICATION_TABLET = consumable("water_purification_tablet", props -> props.stacksTo(32),
            ItemUseAnimation.EAT, 12.0D, 0.0D, 0.0D, null);
    public static final DeferredItem<Item> ORAL_REHYDRATION_SALTS = consumable("oral_rehydration_salts", props -> props.stacksTo(16),
            ItemUseAnimation.EAT, 24.0D, 0.0D, -3.0D, null);
    public static final DeferredItem<Item> ELECTROLYTE_TABLET = consumable("electrolyte_tablet", props -> props.stacksTo(32),
            ItemUseAnimation.EAT, 16.0D, 0.0D, -4.0D, null);
    public static final DeferredItem<Item> REHYDRATION_DRINK = consumable("rehydration_drink", props -> props.stacksTo(16),
            ItemUseAnimation.DRINK, 55.0D, -0.1D, -8.0D, null);
    public static final DeferredItem<Item> RESTORATIVE_TEA = consumable("restorative_tea", props -> props.stacksTo(16),
            ItemUseAnimation.DRINK, 22.0D, 0.2D, -12.0D, null);
    public static final DeferredItem<Item> ENERGY_BAR = consumable("energy_bar", props -> props.stacksTo(32),
            ItemUseAnimation.EAT, 3.0D, 0.0D, -16.0D, null);
    public static final DeferredItem<Item> GLUCOSE_GEL = consumable("glucose_gel", props -> props.stacksTo(16),
            ItemUseAnimation.EAT, 4.0D, 0.0D, -12.0D, null);
    public static final DeferredItem<Item> COOLING_PACK = consumable("cooling_pack", props -> props.stacksTo(16),
            ItemUseAnimation.EAT, 0.0D, -0.6D, -2.0D, null);
    public static final DeferredItem<Item> HEAT_PACK = consumable("heat_pack", props -> props.stacksTo(16),
            ItemUseAnimation.EAT, 0.0D, 0.6D, -2.0D, null);
    public static final DeferredItem<Item> EMERGENCY_BLANKET = consumable("emergency_blanket", props -> props.stacksTo(8),
            ItemUseAnimation.EAT, 0.0D, 0.8D, -4.0D, null);
    public static final DeferredItem<Item> CLEAN_CLOTH = guided("clean_cloth", props -> props.stacksTo(64));
    public static final DeferredItem<Item> SIMPLE_BANDAGE = consumable("simple_bandage", props -> props.stacksTo(32),
            ItemUseAnimation.EAT, 0.0D, 0.0D, -4.0D, null,
            new StatusConsumableItem.EffectSpec(MobEffects.REGENERATION, 80, 0));
    public static final DeferredItem<Item> STERILE_BANDAGE = consumable("sterile_bandage", props -> props.stacksTo(32),
            ItemUseAnimation.EAT, 0.0D, 0.0D, -8.0D, null,
            new StatusConsumableItem.EffectSpec(MobEffects.REGENERATION, 140, 0));
    public static final DeferredItem<Item> DISINFECTANT_WIPE = guided("disinfectant_wipe", props -> props.stacksTo(32));
    public static final DeferredItem<Item> ANTISEPTIC_BOTTLE = guided("antiseptic_bottle", props -> props.stacksTo(16));
    public static final DeferredItem<Item> BASIC_FIRST_AID_KIT = consumable("basic_first_aid_kit", props -> props.stacksTo(8),
            ItemUseAnimation.EAT, 0.0D, 0.0D, -18.0D, null,
            new StatusConsumableItem.EffectSpec(MobEffects.REGENERATION, 200, 0),
            new StatusConsumableItem.EffectSpec(MobEffects.ABSORPTION, 300, 0));
    public static final DeferredItem<Item> CLOTH_MASK = guided("cloth_mask", props -> props.stacksTo(16));
    public static final DeferredItem<Item> DUST_MASK = guided("dust_mask", props -> props.stacksTo(16));
    public static final DeferredItem<Item> SIMPLE_THERMOMETER = guided("simple_thermometer", props -> props.stacksTo(1));
    public static final DeferredItem<Item> FIELD_HEALTH_NOTE = guided("field_health_note", props -> props.stacksTo(1));

    private EarthHumanItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(EarthHumanItems::registerCreativeTab);
    }

    private static DeferredItem<Item> guided(String id, UnaryOperator<Item.Properties> properties) {
        DeferredItem<Item> item = ITEMS.registerItem(id,
                props -> new GuidedItem(props, "tooltip.earth_human." + id),
                properties);
        TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<Item> consumable(
            String id,
            UnaryOperator<Item.Properties> properties,
            ItemUseAnimation animation,
            double hydrationDelta,
            double temperatureDelta,
            double fatigueDelta,
            java.util.function.Supplier<? extends Item> remainder,
            StatusConsumableItem.EffectSpec... effects) {
        DeferredItem<Item> item = ITEMS.registerItem(id,
                props -> new StatusConsumableItem(props, "tooltip.earth_human." + id, animation,
                        hydrationDelta, temperatureDelta, fatigueDelta, remainder, effects),
                properties);
        TAB_ITEMS.add(item);
        return item;
    }

    private static void registerCreativeTab(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(EarthHuman.id("earth_human"),
                CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.earth_human"))
                        .icon(() -> new ItemStack(FILLED_CANTEEN.get()))
                        .displayItems((params, output) -> TAB_ITEMS.forEach(output::accept))
                        .build()));
    }
}
