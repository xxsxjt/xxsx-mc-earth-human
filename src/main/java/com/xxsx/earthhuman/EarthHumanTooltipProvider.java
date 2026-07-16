package com.xxsx.earthhuman;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public interface EarthHumanTooltipProvider {
    void appendEarthHumanTooltip(Consumer<Component> tooltip);
}
