package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.ExerciseAction;
import com.xxsx.earthhuman.HumanTrainingSyncPayload;
import com.xxsx.earthhuman.SomaticsLoreBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Unified Cultivation Hub Screen ([✦ 修行]) accessible directly from the player's inventory.
 * Integrates:
 * - 5x Peak Human Somatic Training (无器械随时锻体 + 五倍常人体魄进度)
 * - Xuanhuan Qi Cultivation (玄学练气)
 * - Fantasy Arcane Mage (初阶法师)
 * - Theology Preacher (初阶传教士)
 * - Earth Lore Knowledge Trees (学识树/锻体学 SOM)
 */
public final class CultivationHubScreen extends Screen {
    private static final int BACKDROP = 0xC80D1113;
    private static final int FRAME = 0xFF12181B;
    private static final int SURFACE = 0xFF1A2226;
    private static final int CARD_BG = 0xD0212B30;
    private static final int EDGE = 0xFF5A6B6E;
    private static final int GOLD = 0xFFE5B85B;
    private static final int SOM_ORANGE = 0xFFD4883A;
    private static final int XUA_JADE = 0xFF3FA688;
    private static final int MAG_VIOLET = 0xFFB274E8;
    private static final int THE_GOLD = 0xFFD4B254;
    private static final int TEXT_MAIN = 0xFFF2F4F0;
    private static final int TEXT_MUTED = 0xFFAEB8B6;

    private final Screen parent;

    public CultivationHubScreen(Screen parent) {
        super(Component.translatable("screen.earth_human.cultivation_hub.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int w = panelWidth();
        int h = panelHeight();

        int contentX = left + 14;
        int contentW = w - 28;

        // 1. No-equipment Calisthenics Action Buttons (俯卧撑 / 深蹲 / 核心训练 / 调息拉伸)
        int exY = top + 96;
        int gap = 6;
        int btnW = (contentW - gap * 3) / 4;
        ExerciseAction[] calisthenics = {
                ExerciseAction.PUSH_UP,
                ExerciseAction.SQUAT,
                ExerciseAction.CORE_TRAINING,
                ExerciseAction.STRETCH
        };
        for (int i = 0; i < calisthenics.length; i++) {
            ExerciseAction action = calisthenics[i];
            int bx = contentX + i * (btnW + gap);
            addRenderableWidget(Button.builder(
                            Component.translatable("screen.earth_human.cultivation_hub.do_exercise", action.displayName()),
                            b -> {
                                Minecraft.getInstance().setScreen(null);
                                EarthHumanClient.requestExerciseAction(action);
                            })
                    .bounds(bx, exY, btnW, 20)
                    .tooltip(Tooltip.create(Component.translatable("screen.earth_human.cultivation_hub.exercise_tip", action.displayName())))
                    .build());
        }

        // 2. Cross-Mod Path Entry Buttons (玄学练气 / 初阶法师 / 神学传教 / 锻体学识树 / 身体护理)
        int realmBtnY = top + 186;
        int colGap = 6;
        int colW = (contentW - colGap * 3) / 4;

        Button xuaBtn = addRenderableWidget(Button.builder(
                        Component.translatable("screen.earth_human.cultivation_hub.open_xuanhuan"),
                        b -> openXuanhuanCultivation())
                .bounds(contentX, realmBtnY, colW, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.earth_human.cultivation_hub.open_xuanhuan.tip")))
                .build());
        xuaBtn.active = SomaticsLoreBridge.isXuanhuanLoaded();

        Button magBtn = addRenderableWidget(Button.builder(
                        Component.translatable("screen.earth_human.cultivation_hub.open_fantasy"),
                        b -> openFantasyAttunement())
                .bounds(contentX + colW + colGap, realmBtnY, colW, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.earth_human.cultivation_hub.open_fantasy.tip")))
                .build());
        magBtn.active = SomaticsLoreBridge.isFantasyLoaded();

        Button somLoreBtn = addRenderableWidget(Button.builder(
                        Component.translatable("screen.earth_human.cultivation_hub.open_somatics_lore"),
                        b -> openLoreTree("earth_lore", "som"))
                .bounds(contentX + (colW + colGap) * 2, realmBtnY, colW, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.earth_human.cultivation_hub.open_somatics_lore.tip")))
                .build());
        somLoreBtn.active = SomaticsLoreBridge.isLoreLoaded();

        Button theoLoreBtn = addRenderableWidget(Button.builder(
                        Component.translatable("screen.earth_human.cultivation_hub.open_theology_lore"),
                        b -> openLoreTree("earth_lore", "the"))
                .bounds(contentX + (colW + colGap) * 3, realmBtnY, colW, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.earth_human.cultivation_hub.open_theology_lore.tip")))
                .build());
        theoLoreBtn.active = SomaticsLoreBridge.isLoreLoaded();

        // Footer buttons
        int footerY = top + h - 26;
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.earth_human.cultivation_hub.open_body_status"),
                        b -> Minecraft.getInstance().setScreen(new EarthHumanStatusScreen(this, EarthHumanStatusScreen.TAB_BODY)))
                .bounds(contentX, footerY, 108, 18)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.earth_human.cultivation_hub.open_quick_care"),
                        b -> Minecraft.getInstance().setScreen(new EarthHumanStatusScreen(this, EarthHumanStatusScreen.TAB_CARE)))
                .bounds(contentX + 114, footerY, 96, 18)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.earth_human.status.close"),
                        b -> onClose())
                .bounds(left + w - 82, footerY, 68, 18)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BACKDROP);
        int left = panelLeft();
        int top = panelTop();
        int w = panelWidth();
        int h = panelHeight();

        g.fill(left, top, left + w, top + h, FRAME);
        g.outline(left, top, w, h, EDGE);
        g.fill(left + 3, top + 3, left + w - 3, top + 29, 0xFF1F2A2E);
        g.fill(left + 3, top + 29, left + w - 3, top + 31, GOLD);
        g.fill(left + 3, top + 31, left + w - 3, top + h - 32, SURFACE);

        g.text(font, Component.translatable("screen.earth_human.cultivation_hub.title"), left + 14, top + 11, GOLD, false);
        Component sub = Component.translatable("screen.earth_human.cultivation_hub.subtitle");
        g.text(font, sub, left + w - 14 - font.width(sub), top + 11, TEXT_MUTED, false);

        int contentX = left + 14;
        int contentW = w - 28;

        // Section 1: Somatic 5x Peak Human Progress
        HumanTrainingSyncPayload training = EarthHumanClient.syncedTraining();
        double mult = training.strengthFactor();
        int totalLvl = training.totalLevel();
        String stageKey = somaticStageKey(totalLvl);

        int somTop = top + 36;
        g.fill(contentX, somTop, contentX + contentW, somTop + 56, CARD_BG);
        g.outline(contentX, somTop, contentW, 56, SOM_ORANGE);

        Component stageHeader = Component.translatable(
                "screen.earth_human.cultivation_hub.somatics_header",
                Component.translatable(stageKey),
                String.format(Locale.ROOT, "%.2fx", mult),
                totalLvl);
        g.text(font, stageHeader, contentX + 8, somTop + 6, TEXT_MAIN, false);

        int bonusHp = (int) Math.min(80, Math.round((mult - 1.0D) * 10.0D) * 2);
        Component bonusLine = Component.translatable(
                "screen.earth_human.cultivation_hub.somatics_bonuses",
                20 + bonusHp,
                String.format(Locale.ROOT, "+%.1f", (mult - 1.0D) / 4.0D * 7.0D),
                String.format(Locale.ROOT, "%.1fx", mult),
                Math.round(3.0D + (mult - 1.0D) * 3.0D));
        g.text(font, bonusLine, contentX + 8, somTop + 19, 0xFFEAD2AC, false);

        // 3 body part training bars
        int barGap = 8;
        int barW = (contentW - 16 - barGap * 2) / 3;
        drawPartTrainingBar(g, contentX + 8, somTop + 33, barW, BodyPart.HEAD, training);
        drawPartTrainingBar(g, contentX + 8 + barW + barGap, somTop + 33, barW, BodyPart.TORSO, training);
        drawPartTrainingBar(g, contentX + 8 + (barW + barGap) * 2, somTop + 33, barW, BodyPart.LIMBS, training);

        // Section 2: Realm & Discipline Overview Cards
        int cardsTop = top + 122;
        int colGap = 6;
        int colW = (contentW - colGap * 3) / 4;
        drawPathCard(g, contentX, cardsTop, colW, 60, XUA_JADE,
                "screen.earth_human.cultivation_hub.card.xua.title",
                "screen.earth_human.cultivation_hub.card.xua.desc",
                SomaticsLoreBridge.isXuanhuanLoaded());
        drawPathCard(g, contentX + colW + colGap, cardsTop, colW, 60, MAG_VIOLET,
                "screen.earth_human.cultivation_hub.card.mag.title",
                "screen.earth_human.cultivation_hub.card.mag.desc",
                SomaticsLoreBridge.isFantasyLoaded());
        drawPathCard(g, contentX + (colW + colGap) * 2, cardsTop, colW, 60, SOM_ORANGE,
                "screen.earth_human.cultivation_hub.card.som.title",
                "screen.earth_human.cultivation_hub.card.som.desc",
                true);
        drawPathCard(g, contentX + (colW + colGap) * 3, cardsTop, colW, 60, THE_GOLD,
                "screen.earth_human.cultivation_hub.card.the.title",
                "screen.earth_human.cultivation_hub.card.the.desc",
                SomaticsLoreBridge.isTheologyLoaded());

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawPartTrainingBar(GuiGraphicsExtractor g, int x, int y, int w, BodyPart part, HumanTrainingSyncPayload training) {
        int lvl = training.level(part);
        int load = (int) Math.round(training.load(part));
        String text = Component.translatable(part.translationKey()).getString() + " Lv." + lvl + "/50 (" + load + "%)";
        g.text(font, text, x, y, TEXT_MUTED, false);
        g.fill(x, y + 11, x + w, y + 16, 0xFF101517);
        double ratio = Math.min(1.0D, lvl / 50.0D);
        int filled = (int) Math.round((w - 2) * ratio);
        if (filled > 0) {
            g.fill(x + 1, y + 12, x + 1 + filled, y + 15, SOM_ORANGE);
        }
    }

    private void drawPathCard(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent,
                              String titleKey, String descKey, boolean active) {
        g.fill(x, y, x + w, y + h, CARD_BG);
        g.outline(x, y, w, h, active ? accent : 0xFF444E50);
        g.fill(x, y, x + w, y + 3, active ? accent : 0xFF444E50);
        g.text(font, Component.translatable(titleKey), x + 5, y + 7, active ? TEXT_MAIN : TEXT_MUTED, false);
        String desc = Component.translatable(descKey).getString();
        int lineY = y + 20;
        String remaining = desc;
        for (int line = 0; line < 3 && !remaining.isEmpty(); line++) {
            String sub = font.plainSubstrByWidth(remaining, w - 10);
            if (sub.isEmpty()) {
                break;
            }
            g.text(font, sub, x + 5, lineY + line * 11, TEXT_MUTED, false);
            remaining = remaining.substring(sub.length()).stripLeading();
        }
    }

    private static String somaticStageKey(int totalLvl) {
        if (totalLvl >= 100) return "screen.earth_human.cultivation_hub.stage.fivefold";
        if (totalLvl >= 50) return "screen.earth_human.cultivation_hub.stage.furnace";
        if (totalLvl >= 25) return "screen.earth_human.cultivation_hub.stage.tendon";
        if (totalLvl >= 10) return "screen.earth_human.cultivation_hub.stage.calisthenics";
        return "screen.earth_human.cultivation_hub.stage.mortal";
    }

    private void openXuanhuanCultivation() {
        try {
            Class<?> cls = Class.forName("com.xxsx.xuanhuanearth.client.XuanhuanEarthClient");
            Method m = cls.getMethod("requestOpenCultivation");
            m.invoke(null);
        } catch (Throwable ignored) {
        }
    }

    private void openFantasyAttunement() {
        try {
            Class<?> cls = Class.forName("com.xxsx.fantasyearth.client.FantasyEarthClient");
            Method m = cls.getMethod("requestOpenAttunement");
            m.invoke(null);
        } catch (Throwable ignored) {
        }
    }

    private void openLoreTree(String ns, String path) {
        try {
            Class<?> cls = Class.forName("com.xxsx.earthlore.client.LoreTreeScreen");
            Constructor<?> ctor = cls.getConstructor(Identifier.class);
            Screen screen = (Screen) ctor.newInstance(Identifier.fromNamespaceAndPath(ns, path));
            Minecraft.getInstance().setScreen(screen);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private int panelWidth() {
        return Math.min(460, Math.max(320, width - 16));
    }

    private int panelHeight() {
        return Math.min(242, Math.max(218, height - 20));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }
}
