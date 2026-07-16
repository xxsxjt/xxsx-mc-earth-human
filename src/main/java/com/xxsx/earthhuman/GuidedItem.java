package com.xxsx.earthhuman;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

public class GuidedItem extends Item implements EarthHumanTooltipProvider {
    private final String tooltipKey;

    public GuidedItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendEarthHumanTooltip(Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
    }
}
