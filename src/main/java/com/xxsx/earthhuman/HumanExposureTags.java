package com.xxsx.earthhuman;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class HumanExposureTags {
    public static final TagKey<Block> DUST_SOURCES = block("dust_sources");

    private HumanExposureTags() {
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, EarthHuman.id(path));
    }
}
