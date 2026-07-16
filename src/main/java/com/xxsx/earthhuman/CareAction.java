package com.xxsx.earthhuman;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum CareAction {
    HYDRATE("hydrate", HumanCareTags.HYDRATION, 32),
    BANDAGE("bandage", HumanCareTags.BANDAGES, 40),
    FIRST_AID("first_aid", HumanCareTags.FIRST_AID, 60),
    WARM("warm", HumanCareTags.WARMING, 36),
    COOL("cool", HumanCareTags.COOLING, 36);

    public final String id;
    public final TagKey<Item> itemTag;
    public final int durationTicks;

    CareAction(String id, TagKey<Item> itemTag, int durationTicks) {
        this.id = id;
        this.itemTag = itemTag;
        this.durationTicks = durationTicks;
    }

    public String titleKey() {
        return "care.earth_human." + id;
    }

    public String descriptionKey() {
        return titleKey() + ".description";
    }

    public static CareAction byId(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (CareAction action : values()) {
            if (action.id.equals(normalized)) {
                return action;
            }
        }
        return null;
    }
}
