package com.xxsx.earthhuman.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** A short, discoverable explanation layer kept separate from the live status screen. */
public final class EarthHumanGuideScreen extends Screen {
    private static final int PAGE_COUNT = 6;
    private static final int BACKDROP = 0xC0101314;
    private static final int FRAME = 0xFF111719;
    private static final int SURFACE = 0xFF1B2325;
    private static final int SURFACE_ALT = 0xFF242E30;
    private static final int EDGE = 0xFF667673;
    private static final int ACCENT = 0xFF7BB69F;
    private static final int TEXT = 0xFFF0F2EC;
    private static final int MUTED = 0xFFB3BFBA;

    private final Screen parent;
    private int page;

    public EarthHumanGuideScreen(Screen parent) {
        super(Component.translatable("screen.earth_human.guide.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int left = panelLeft();
        int top = panelTop();
        int width = panelWidth();
        int bottom = top + panelHeight();

        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.guide.previous"), button -> changePage(-1))
                .bounds(left + 12, bottom - 27, 60, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.guide.open_status"), button ->
                        Minecraft.getInstance().setScreen(new EarthHumanStatusScreen(this, EarthHumanStatusScreen.TAB_OVERVIEW)))
                .bounds(left + (width - 112) / 2, bottom - 27, 112, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.guide.next"), button -> changePage(1))
                .bounds(left + width - 72, bottom - 27, 60, 18)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, BACKDROP);
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();

        graphics.fill(left, top, left + panelWidth, top + panelHeight, FRAME);
        graphics.outline(left, top, panelWidth, panelHeight, EDGE);
        graphics.fill(left + 3, top + 3, left + panelWidth - 3, top + 34, SURFACE_ALT);
        graphics.fill(left + 3, top + 37, left + panelWidth - 3, top + panelHeight - 34, SURFACE);
        graphics.fill(left + 7, top + 37, left + 10, top + panelHeight - 34, ACCENT);
        graphics.text(font, Component.translatable("screen.earth_human.guide.title"),
                left + 14, top + 10, TEXT, false);
        graphics.text(font, Component.translatable("screen.earth_human.guide.page_counter", page + 1, PAGE_COUNT),
                left + panelWidth - 60, top + 11, MUTED, false);

        Page content = Page.values()[page];
        graphics.text(font, Component.translatable(content.titleKey), left + 18, top + 48, ACCENT, false);
        List<FormattedCharSequence> lines = font.split(Component.translatable(content.bodyKey), panelWidth - 38);
        int lineY = top + 68;
        for (FormattedCharSequence line : lines) {
            if (lineY > top + panelHeight - 48) {
                break;
            }
            graphics.text(font, line, left + 18, lineY, TEXT, false);
            lineY += 12;
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void changePage(int delta) {
        page = Math.floorMod(page + delta, PAGE_COUNT);
        init();
    }

    private int panelWidth() {
        return Math.min(456, Math.max(300, width - 16));
    }

    private int panelHeight() {
        return Math.min(224, Math.max(190, height - 16));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    private enum Page {
        BODY("screen.earth_human.guide.body.title", "screen.earth_human.guide.body.body"),
        VITALS("screen.earth_human.guide.vitals.title", "screen.earth_human.guide.vitals.body"),
        CARE("screen.earth_human.guide.care.title", "screen.earth_human.guide.care.body"),
        ACTIVITY("screen.earth_human.guide.activity.title", "screen.earth_human.guide.activity.body"),
        ENVIRONMENT("screen.earth_human.guide.environment.title", "screen.earth_human.guide.environment.body"),
        COMPATIBILITY("screen.earth_human.guide.compatibility.title", "screen.earth_human.guide.compatibility.body");

        private final String titleKey;
        private final String bodyKey;

        Page(String titleKey, String bodyKey) {
            this.titleKey = titleKey;
            this.bodyKey = bodyKey;
        }
    }
}
