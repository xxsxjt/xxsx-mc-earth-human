package com.xxsx.earthhuman;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/** Stable classification used by the body router and addon documentation. */
public enum DamageProfile {
    BYPASS,
    FALL,
    HEAD,
    FULL_BODY,
    EXPLOSION,
    ENVIRONMENT,
    LOW_CONTACT,
    PROJECTILE,
    MELEE,
    GENERIC;

    public static DamageProfile classify(DamageSource source) {
        if (source.is(HumanDamageTags.BYPASSES_BODY)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.GENERIC_KILL)) {
            return BYPASS;
        }
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.FLY_INTO_WALL)) {
            return FALL;
        }
        if (source.is(HumanDamageTags.FULL_BODY)) {
            return FULL_BODY;
        }
        if (source.is(HumanDamageTags.HEAD_PRIORITY)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.DRAGON_BREATH)) {
            return HEAD;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return EXPLOSION;
        }
        if (source.is(HumanDamageTags.LEG_PRIORITY)
                || source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypes.DRY_OUT)) {
            return ENVIRONMENT;
        }
        if (source.is(HumanDamageTags.LOW_CONTACT)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.STING)
                || source.is(DamageTypes.SWEET_BERRY_BUSH)) {
            return LOW_CONTACT;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return PROJECTILE;
        }
        return source.getEntity() != null ? MELEE : GENERIC;
    }
}
