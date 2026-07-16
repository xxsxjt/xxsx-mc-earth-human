package com.xxsx.earthhuman;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public final class HumanDamageTags {
    public static final TagKey<DamageType> BYPASSES_BODY = tag("bypasses_body");
    public static final TagKey<DamageType> HEAD_PRIORITY = tag("head_priority");
    public static final TagKey<DamageType> FULL_BODY = tag("full_body");

    private HumanDamageTags() {
    }

    private static TagKey<DamageType> tag(String path) {
        return TagKey.create(Registries.DAMAGE_TYPE, EarthHuman.id(path));
    }
}
