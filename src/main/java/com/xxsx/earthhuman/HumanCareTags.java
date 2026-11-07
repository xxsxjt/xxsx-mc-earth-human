package com.xxsx.earthhuman;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class HumanCareTags {
    public static final TagKey<Item> HYDRATION = item("care/hydration");
    public static final TagKey<Item> BANDAGES = item("care/bandages");
    public static final TagKey<Item> SPLINTS = item("care/splints");
    public static final TagKey<Item> FIRST_AID = item("care/first_aid");
    public static final TagKey<Item> WARMING = item("care/warming");
    public static final TagKey<Item> COOLING = item("care/cooling");
    public static final TagKey<Item> MASKS = item("care/masks");
    public static final TagKey<Item> DUST_MASKS = item("care/dust_masks");

    private HumanCareTags() {
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, EarthHuman.id(path));
    }
}
