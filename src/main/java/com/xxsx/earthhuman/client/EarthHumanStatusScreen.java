package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.CareAction;
import com.xxsx.earthhuman.EarthHumanItems;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import com.xxsx.earthhuman.HumanCareStatePayload;
import com.xxsx.earthhuman.HumanStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleConsumer;

public class EarthHumanStatusScreen extends Screen {
    public static final int TAB_OVERVIEW = 0;
    public static final int TAB_BODY = 1;
    public static final int TAB_CARE = 2;
    public static final int TAB_SETTINGS = 3;

    private static final int BACKDROP = 0xC0101314;
    private static final int FRAME = 0xFF111719;
    private static final int SURFACE = 0xFF1B2325;
    private static final int SURFACE_ALT = 0xFF242E30;
    private static final int LEDGER = 0x253E5552;
    private static final int EDGE = 0xFF667673;
    private static final int ACCENT = 0xFF7BB69F;
    private static final int ACCENT_DARK = 0xFF385B50;
    private static final int MEDICAL_WHITE = 0xFFF0F2EC;
    private static final int MUTED = 0xFFB3BFBA;
    private static final int DANGER = 0xFFD85A56;
    private static final int WARNING = 0xFFD5A44F;

    private final Screen parent;
    private final Map<CareAction, Button> careButtons = new EnumMap<>(CareAction.class);
    private int tab;
    private Button cancelCareButton;

    public EarthHumanStatusScreen(Screen parent, int initialTab) {
        super(Component.translatable("screen.earth_human.status.title"));
        this.parent = parent;
        this.tab = Math.max(TAB_OVERVIEW, Math.min(TAB_SETTINGS, initialTab));
        HumanHudClientConfig.load();
    }

    @Override
    protected void init() {
        careButtons.clear();
        cancelCareButton = null;
        addTabButtons();

        if (tab == TAB_OVERVIEW) {
            addOverviewButtons();
        } else if (tab == TAB_CARE) {
            addCareButtons();
        } else if (tab == TAB_SETTINGS) {
            addSettingsWidgets();
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.status.close"), b -> onClose())
                .bounds(panelLeft() + panelWidth() - 72, panelTop() + panelHeight() - 27, 60, 18)
                .build());
    }

    @Override
    public void tick() {
        if (tab == TAB_CARE) {
            updateCareButtons();
        }
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
        g.fill(left + 3, top + 3, left + w - 3, top + 33, SURFACE_ALT);
        g.fill(left + 3, top + 33, left + 7, top + h - 4, ACCENT_DARK);
        g.fill(left + 7, top + 60, left + w - 3, top + h - 34, SURFACE);
        drawLedger(g, left + 8, top + 61, left + w - 4, top + h - 35);
        drawMedicalMark(g, left + 13, top + 10);
        g.text(font, Component.translatable("screen.earth_human.status.title"), left + 36, top + 9, MEDICAL_WHITE, false);
        g.text(font, fit(Component.translatable("screen.earth_human.status.subtitle"), Math.max(80, w - 250)),
                left + 192, top + 11, MUTED, false);
        g.fill(left + 7, top + 58, left + w - 3, top + 60, ACCENT);

        switch (tab) {
            case TAB_BODY -> drawBody(g);
            case TAB_CARE -> drawCare(g);
            case TAB_SETTINGS -> drawSettings(g);
            default -> drawOverview(g);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        HumanHudClientConfig.save();
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private void addTabButtons() {
        int gap = 4;
        int available = panelWidth() - 28;
        int tabWidth = (available - gap * 3) / 4;
        int x = panelLeft() + 14;
        int y = panelTop() + 36;
        addTabButton(x, y, tabWidth, TAB_OVERVIEW, "screen.earth_human.status.tab.overview");
        addTabButton(x + tabWidth + gap, y, tabWidth, TAB_BODY, "screen.earth_human.status.tab.body");
        addTabButton(x + (tabWidth + gap) * 2, y, tabWidth, TAB_CARE, "screen.earth_human.status.tab.care");
        addTabButton(x + (tabWidth + gap) * 3, y, tabWidth, TAB_SETTINGS, "screen.earth_human.status.tab.settings");
    }

    private void addTabButton(int x, int y, int width, int targetTab, String key) {
        Button button = addRenderableWidget(Button.builder(Component.translatable(key), b -> switchTab(targetTab))
                .bounds(x, y, width, 20)
                .build());
        button.active = tab != targetTab;
    }

    private void switchTab(int targetTab) {
        if (tab == targetTab) {
            return;
        }
        tab = targetTab;
        clearWidgets();
        init();
    }

    private void addOverviewButtons() {
        int y = panelTop() + panelHeight() - 27;
        int x = contentLeft();
        int width = compactWidth() ? 78 : 94;
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.status.open_body"), b -> switchTab(TAB_BODY))
                .bounds(x, y, width, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.status.open_care"), b -> switchTab(TAB_CARE))
                .bounds(x + width + 6, y, width, 18)
                .build());
    }

    private void addCareButtons() {
        int buttonWidth = compactWidth() ? 62 : 76;
        int buttonHeight = compactHeight() ? 16 : 18;
        for (CareAction action : CareAction.values()) {
            int rowY = careRowY(action);
            Button button = addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.status.care.start"),
                            b -> EarthHumanClient.requestCareAction(action))
                    .bounds(contentRight() - buttonWidth - 5, rowY + (compactHeight() ? 2 : 5), buttonWidth, buttonHeight)
                    .tooltip(Tooltip.create(Component.translatable(action.descriptionKey())))
                    .build());
            careButtons.put(action, button);
        }
        cancelCareButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.earth_human.status.care.cancel"),
                        b -> EarthHumanClient.cancelCareAction())
                .bounds(panelLeft() + panelWidth() - 158, panelTop() + panelHeight() - 27, 80, 18)
                .build());
        updateCareButtons();
    }

    private void updateCareButtons() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean active = EarthHumanClient.syncedCareState().active();
        for (Map.Entry<CareAction, Button> entry : careButtons.entrySet()) {
            boolean hasItem = minecraft.player != null && minecraft.player.getInventory().contains(entry.getKey().itemTag);
            entry.getValue().active = !active && hasItem;
        }
        if (cancelCareButton != null) {
            cancelCareButton.active = active;
        }
    }

    private void addSettingsWidgets() {
        int x = contentLeft();
        int y = contentTop() + 19;
        int columnGap = 10;
        int columnWidth = (contentWidth() - columnGap) / 2;

        addRenderableWidget(Checkbox.builder(Component.translatable("screen.earth_human.settings.vitals"), font)
                .pos(x, y)
                .maxWidth(columnWidth)
                .selected(HumanHudClientConfig.showVitalsHud())
                .onValueChange((box, value) -> HumanHudClientConfig.setShowVitalsHud(value))
                .tooltip(Tooltip.create(Component.translatable("screen.earth_human.settings.tooltip.vitals")))
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("screen.earth_human.settings.body"), font)
                .pos(x + columnWidth + columnGap, y)
                .maxWidth(columnWidth)
                .selected(HumanHudClientConfig.showBodyHud())
                .onValueChange((box, value) -> HumanHudClientConfig.setShowBodyHud(value))
                .tooltip(Tooltip.create(Component.translatable("screen.earth_human.settings.tooltip.enabled")))
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("screen.earth_human.settings.numbers"), font)
                .pos(x, y + 25)
                .maxWidth(columnWidth)
                .selected(HumanHudClientConfig.showNumericValues())
                .onValueChange((box, value) -> HumanHudClientConfig.setShowNumericValues(value))
                .tooltip(Tooltip.create(Component.translatable("screen.earth_human.settings.tooltip.numbers")))
                .build());

        addRenderableWidget(CycleButton.builder(
                        theme -> Component.translatable(theme.translationKey()),
                        HumanHudClientConfig.hudTheme())
                .withValues(List.of(HumanHudClientConfig.HudTheme.values()))
                .withTooltip(theme -> Tooltip.create(Component.translatable("screen.earth_human.settings.tooltip.theme")))
                .create(x + columnWidth + columnGap, y + 22, columnWidth, 20,
                        Component.translatable("screen.earth_human.settings.theme.label"),
                        (button, value) -> HumanHudClientConfig.setHudTheme(value)));

        addRenderableWidget(CycleButton.builder(
                        corner -> Component.translatable(corner.translationKey()),
                        HumanHudClientConfig.bodyHudCorner())
                .withValues(List.of(HumanHudClientConfig.HudCorner.values()))
                .withTooltip(corner -> Tooltip.create(Component.translatable("screen.earth_human.settings.tooltip.corner")))
                .create(x, y + 51, contentWidth(), 20,
                        Component.translatable("screen.earth_human.settings.corner.label"),
                        (button, value) -> HumanHudClientConfig.setBodyHudCorner(value)));

        addRenderableWidget(new ConfigSlider(x, y + 78, columnWidth, "screen.earth_human.settings.size.slider",
                HumanHudClientConfig.bodyHudScale(), 0.6D, 2.0D, HumanHudClientConfig::setBodyHudScale));
        addRenderableWidget(new ConfigSlider(x + columnWidth + columnGap, y + 78, columnWidth,
                "screen.earth_human.settings.opacity.slider", HumanHudClientConfig.bodyHudOpacity(),
                0.1D, 1.0D, HumanHudClientConfig::setBodyHudOpacity));
    }

    private void drawOverview(GuiGraphicsExtractor g) {
        int x = contentLeft();
        int y = contentTop();
        HumanStatus status = EarthHumanClient.syncedStatus();
        HumanBodySyncPayload body = EarthHumanClient.syncedBody();
        int bodyPlateWidth = compactWidth() ? 58 : 78;
        int meterWidth = contentWidth() - bodyPlateWidth - 12;
        int meterStep = compactHeight() ? 19 : 24;

        drawSectionHeading(g, x, y, "screen.earth_human.status.overview.heading");
        drawMeter(g, x, y + 17, meterWidth, "screen.earth_human.status.hydration",
                status.hydration / 100.0D, Math.round(status.hydration) + "%",
                vitalColor(status.hydration / 100.0D, false));
        drawMeter(g, x, y + 17 + meterStep, meterWidth, "screen.earth_human.status.temperature",
                temperatureRatio(status.bodyTemperature), String.format(Locale.ROOT, "%.1f C", status.bodyTemperature),
                temperatureColor(status.bodyTemperature));
        drawMeter(g, x, y + 17 + meterStep * 2, meterWidth, "screen.earth_human.status.fatigue",
                status.fatigue / 100.0D, Math.round(status.fatigue) + "%",
                vitalColor(status.fatigue / 100.0D, true));
        drawMeter(g, x, y + 17 + meterStep * 3, meterWidth, "screen.earth_human.status.air_quality",
                status.airQuality / 100.0D, Math.round(status.airQuality) + "%",
                airQualityColor(status.airQuality));

        int bodyX = x + meterWidth + 12;
        int plateHeight = compactHeight() ? 84 : 96;
        g.fill(bodyX, y + 15, bodyX + bodyPlateWidth, y + 15 + plateHeight, 0xB0141A1C);
        g.outline(bodyX, y + 15, bodyPlateWidth, plateHeight, ACCENT_DARK);
        g.centeredText(font, Component.translatable("screen.earth_human.status.body_summary"),
                bodyX + bodyPlateWidth / 2, y + 20, MUTED);
        double scale = compactHeight() ? 0.82D : 0.98D;
        BodyHudRenderer.draw(g, body, bodyX + (bodyPlateWidth - BodyHudRenderer.width(scale)) / 2,
                y + 33, scale, 0.95D);

        int activityY = y + 18 + meterStep * 4;
        g.text(font, fit(Component.translatable("screen.earth_human.status.activity_line",
                Component.translatable(status.currentActivity.translationKey()), Math.round(status.exertionLoad)), meterWidth),
                x, activityY, MEDICAL_WHITE, false);
        drawThinBar(g, x, activityY + 12, meterWidth, status.exertionLoad / 100.0D,
                vitalColor(status.exertionLoad / 100.0D, true));

        Component recommendation = recommendation(status, body);
        if (compactHeight()) {
            drawCompactRecommendation(g, x, activityY + 18, contentWidth(), recommendation);
        } else {
            int noteY = Math.max(activityY + 23, panelTop() + panelHeight() - 62);
            drawRecommendation(g, x, noteY, contentWidth(), recommendation);
        }
    }

    private void drawBody(GuiGraphicsExtractor g) {
        int x = contentLeft();
        int y = contentTop();
        HumanBodySyncPayload body = EarthHumanClient.syncedBody();
        drawSectionHeading(g, x, y, "screen.earth_human.status.body.heading");

        int figureWidth = compactWidth() ? 74 : 104;
        int figureTop = y + (compactHeight() ? 20 : 16);
        int figureHeight = compactHeight() ? 114 : panelHeight() - 106;
        g.fill(x, figureTop, x + figureWidth, figureTop + figureHeight, 0xB0141A1C);
        g.outline(x, figureTop, figureWidth, figureHeight, ACCENT_DARK);
        drawDiagnosticCorners(g, x, figureTop, figureWidth, figureHeight);
        double bodyScale = compactHeight() ? 1.35D : 1.75D;
        BodyHudRenderer.draw(g, body, x + (figureWidth - BodyHudRenderer.width(bodyScale)) / 2,
                y + (compactHeight() ? 26 : 24), bodyScale, 1.0D);

        int barsX = x + figureWidth + 12;
        int barsWidth = contentRight() - barsX;
        int rowY = y + (compactHeight() ? 18 : 15);
        int rowStep = compactHeight() ? 18 : Math.max(21, (figureHeight - 1) / BodyPart.values().length);
        for (BodyPart part : BodyPart.values()) {
            double ratio = body.value(part);
            drawMeter(g, barsX, rowY, barsWidth, part.translationKey(), ratio,
                    Math.round(ratio * 100.0D) + "%", bodyColor(ratio));
            rowY += rowStep;
        }
    }

    private void drawCare(GuiGraphicsExtractor g) {
        int x = contentLeft();
        int y = contentTop();
        HumanCareStatePayload careState = EarthHumanClient.syncedCareState();
        drawSectionHeading(g, x, y, "screen.earth_human.status.care.heading");

        if (careState.active()) {
            CareAction activeAction = CareAction.byId(careState.actionId());
            Component actionName = activeAction == null
                    ? Component.translatable("screen.earth_human.status.care.running")
                    : Component.translatable(activeAction.titleKey());
            g.text(font, fit(Component.translatable("screen.earth_human.status.care.active", actionName,
                            String.format(Locale.ROOT, "%.1f", careState.remainingTicks() / 20.0D)), contentWidth()),
                    x, y + 17, ACCENT, false);
        } else if (!careState.feedbackKey().isEmpty()) {
            g.text(font, fit(Component.translatable(careState.feedbackKey()), contentWidth()),
                    x, y + 17, MEDICAL_WHITE, false);
        } else if (!compactHeight()) {
            g.text(font, Component.translatable("screen.earth_human.status.care.idle"), x, y + 17, MUTED, false);
        }

        int buttonWidth = compactWidth() ? 62 : 76;
        for (CareAction action : CareAction.values()) {
            int rowY = careRowY(action);
            int rowHeight = careRowStep() - 2;
            int rowRight = contentRight() - buttonWidth - 9;
            ItemStack inventoryItem = findCareItem(action);
            ItemStack displayItem = inventoryItem.isEmpty() ? representativeCareItem(action) : inventoryItem;
            boolean hasItem = !inventoryItem.isEmpty();
            boolean isActive = careState.active() && action.id.equals(careState.actionId());

            g.fill(x, rowY, contentRight(), rowY + rowHeight, isActive ? 0xD02A3B37 : 0xB01B2426);
            g.fill(x, rowY, x + 3, rowY + rowHeight, isActive ? ACCENT : (hasItem ? ACCENT_DARK : 0xFF555D5C));
            int slotY = rowY + Math.max(1, (rowHeight - 18) / 2);
            g.fill(x + 6, slotY, x + 24, slotY + 18, 0xFF111719);
            g.outline(x + 6, slotY, 18, 18, hasItem ? EDGE : 0xFF4B5352);
            g.item(displayItem, x + 7, slotY + 1);
            if (!hasItem) {
                g.fill(x + 7, slotY + 1, x + 23, slotY + 17, 0x78000000);
            }

            int textX = x + 29;
            g.text(font, fit(Component.translatable(action.titleKey()), Math.max(35, rowRight - textX)),
                    textX, rowY + 3, hasItem ? MEDICAL_WHITE : MUTED, false);
            Component detail = hasItem
                    ? Component.translatable("screen.earth_human.status.care.ready_timed", inventoryItem.getHoverName(),
                    String.format(Locale.ROOT, "%.1f", action.durationTicks / 20.0D))
                    : Component.translatable("screen.earth_human.status.care.missing");
            g.text(font, fit(detail, Math.max(35, rowRight - textX)), textX, rowY + 13,
                    hasItem ? ACCENT : WARNING, false);

            if (isActive) {
                double progress = 1.0D - careState.remainingTicks() / (double) Math.max(1, careState.totalTicks());
                drawThinBar(g, textX, rowY + rowHeight - 4, Math.max(20, rowRight - textX), progress, ACCENT);
            }
        }
    }

    private void drawSettings(GuiGraphicsExtractor g) {
        int x = contentLeft();
        int y = contentTop();
        drawSectionHeading(g, x, y, "screen.earth_human.status.settings.heading");
        if (!compactHeight()) {
            int previewY = y + 126;
            g.fill(x, previewY, contentRight(), previewY + 40, 0xB0141A1C);
            g.outline(x, previewY, contentWidth(), 40, ACCENT_DARK);
            g.text(font, Component.translatable("screen.earth_human.settings.preview"), x + 7, previewY + 6, MUTED, false);
            double previewScale = 0.55D;
            BodyHudRenderer.draw(g, EarthHumanClient.syncedBody(), x + 58, previewY + 3,
                    previewScale, HumanHudClientConfig.bodyHudOpacity());
            g.text(font, Component.translatable("screen.earth_human.settings.current_values",
                            Math.round(HumanHudClientConfig.bodyHudScale() * 100.0D),
                            Math.round(HumanHudClientConfig.bodyHudOpacity() * 100.0D)),
                    x + 92, previewY + 7, MEDICAL_WHITE, false);
            g.text(font, fit(Component.translatable("screen.earth_human.settings.color_hint"), contentWidth() - 99),
                    x + 92, previewY + 20, MUTED, false);
        }
    }

    private void drawSectionHeading(GuiGraphicsExtractor g, int x, int y, String key) {
        g.fill(x, y + 10, x + 10, y + 12, ACCENT);
        g.text(font, fit(Component.translatable(key), contentWidth() - 16), x + 15, y + 6, MEDICAL_WHITE, false);
    }

    private void drawMeter(GuiGraphicsExtractor g, int x, int y, int width, String labelKey,
                           double ratio, String value, int color) {
        double clamped = Math.max(0.0D, Math.min(1.0D, ratio));
        g.text(font, Component.translatable(labelKey), x, y, MUTED, false);
        g.text(font, value, x + width - font.width(value), y, MEDICAL_WHITE, false);
        g.fill(x, y + 11, x + width, y + 18, 0xFF111719);
        g.outline(x, y + 11, width, 7, 0xFF46514F);
        int filled = (int) Math.round((width - 2) * clamped);
        if (filled > 0) {
            g.fill(x + 1, y + 12, x + 1 + filled, y + 17, color);
        }
        for (int marker = 1; marker < 4; marker++) {
            int markerX = x + width * marker / 4;
            g.fill(markerX, y + 12, markerX + 1, y + 17, 0x66323B3A);
        }
    }

    private void drawThinBar(GuiGraphicsExtractor g, int x, int y, int width, double ratio, int color) {
        double clamped = Math.max(0.0D, Math.min(1.0D, ratio));
        g.fill(x, y, x + width, y + 4, 0xFF111719);
        int filled = (int) Math.round((width - 2) * clamped);
        if (filled > 0) {
            g.fill(x + 1, y + 1, x + 1 + filled, y + 3, color);
        }
    }

    private void drawRecommendation(GuiGraphicsExtractor g, int x, int y, int width, Component recommendation) {
        g.fill(x, y, x + width, y + 29, 0xD0212B2C);
        g.fill(x, y, x + 4, y + 29, ACCENT);
        g.text(font, Component.translatable("screen.earth_human.status.recommendation"), x + 9, y + 4, ACCENT, false);
        g.text(font, fit(recommendation, width - 18), x + 9, y + 16, MEDICAL_WHITE, false);
    }

    private void drawCompactRecommendation(GuiGraphicsExtractor g, int x, int y, int width,
                                            Component recommendation) {
        g.fill(x, y, x + width, y + 18, 0xD0212B2C);
        g.fill(x, y, x + 4, y + 18, ACCENT);
        g.text(font, Component.translatable("screen.earth_human.status.recommendation"), x + 9, y + 5, ACCENT, false);
        g.text(font, fit(recommendation, width - 92), x + 82, y + 5, MEDICAL_WHITE, false);
    }

    private void drawLedger(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
        for (int y = top + 10; y < bottom; y += 12) {
            g.fill(left, y, right, y + 1, LEDGER);
        }
    }

    private void drawMedicalMark(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 17, y + 17, ACCENT_DARK);
        g.outline(x, y, 17, 17, ACCENT);
        g.fill(x + 7, y + 3, x + 10, y + 14, MEDICAL_WHITE);
        g.fill(x + 3, y + 7, x + 14, y + 10, MEDICAL_WHITE);
    }

    private void drawDiagnosticCorners(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        int length = 7;
        g.fill(x + 3, y + 3, x + 3 + length, y + 4, ACCENT);
        g.fill(x + 3, y + 3, x + 4, y + 3 + length, ACCENT);
        g.fill(x + width - 3 - length, y + 3, x + width - 3, y + 4, ACCENT);
        g.fill(x + width - 4, y + 3, x + width - 3, y + 3 + length, ACCENT);
        g.fill(x + 3, y + height - 4, x + 3 + length, y + height - 3, ACCENT);
        g.fill(x + 3, y + height - 3 - length, x + 4, y + height - 3, ACCENT);
        g.fill(x + width - 3 - length, y + height - 4, x + width - 3, y + height - 3, ACCENT);
        g.fill(x + width - 4, y + height - 3 - length, x + width - 3, y + height - 3, ACCENT);
    }

    private Component recommendation(HumanStatus status, HumanBodySyncPayload body) {
        double worstBody = 1.0D;
        for (BodyPart part : BodyPart.values()) {
            worstBody = Math.min(worstBody, body.value(part));
        }
        if (worstBody < 0.35D) return Component.translatable("screen.earth_human.status.recommend.body");
        if (status.hydration < 35.0D) return Component.translatable("screen.earth_human.status.recommend.hydration");
        if (status.fatigue > 70.0D) return Component.translatable("screen.earth_human.status.recommend.fatigue");
        if (status.exertionLoad > 70.0D) return Component.translatable("screen.earth_human.status.recommend.exertion");
        if (status.airQuality < 35.0D) return Component.translatable("screen.earth_human.status.recommend.air_quality");
        if (status.bodyTemperature < 35.8D) return Component.translatable("screen.earth_human.status.recommend.cold");
        if (status.bodyTemperature > 38.0D) return Component.translatable("screen.earth_human.status.recommend.hot");
        return Component.translatable("screen.earth_human.status.recommend.ok");
    }

    private ItemStack findCareItem(CareAction action) {
        if (Minecraft.getInstance().player == null) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < Minecraft.getInstance().player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slot);
            if (stack.is(action.itemTag)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack representativeCareItem(CareAction action) {
        return switch (action) {
            case HYDRATE -> new ItemStack(EarthHumanItems.FILLED_CANTEEN.get());
            case BANDAGE -> new ItemStack(EarthHumanItems.STERILE_BANDAGE.get());
            case FIRST_AID -> new ItemStack(EarthHumanItems.BASIC_FIRST_AID_KIT.get());
            case WARM -> new ItemStack(EarthHumanItems.HEAT_PACK.get());
            case COOL -> new ItemStack(EarthHumanItems.COOLING_PACK.get());
        };
    }

    private Component fit(Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int allowed = Math.max(1, maxWidth - font.width(suffix));
        return Component.literal(font.plainSubstrByWidth(text.getString(), allowed) + suffix);
    }

    private int careRowY(CareAction action) {
        return contentTop() + (compactHeight() ? 27 : 32) + action.ordinal() * careRowStep();
    }

    private int careRowStep() {
        return compactHeight() ? 20 : 29;
    }

    private int panelWidth() {
        return Math.min(468, Math.max(310, width - 12));
    }

    private int panelHeight() {
        return Math.min(286, Math.max(228, height - 20));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    private int contentLeft() {
        return panelLeft() + 16;
    }

    private int contentRight() {
        return panelLeft() + panelWidth() - 14;
    }

    private int contentTop() {
        return panelTop() + 65;
    }

    private int contentWidth() {
        return contentRight() - contentLeft();
    }

    private boolean compactWidth() {
        return panelWidth() < 390;
    }

    private boolean compactHeight() {
        return panelHeight() < 260;
    }

    private static double temperatureRatio(double temperature) {
        return (temperature - 32.0D) / 10.5D;
    }

    private static int temperatureColor(double temperature) {
        if (temperature < 35.5D) return 0xFF5C91B7;
        if (temperature > 38.2D) return 0xFFD8844F;
        return ACCENT;
    }

    private static int vitalColor(double ratio, boolean reverse) {
        double health = reverse ? 1.0D - ratio : ratio;
        if (health < 0.25D) return DANGER;
        if (health < 0.55D) return WARNING;
        return ACCENT;
    }

    private static int bodyColor(double ratio) {
        if (ratio <= 0.01D) return 0xFF080A0A;
        if (ratio < 0.30D) return DANGER;
        if (ratio < 0.65D) return WARNING;
        return ACCENT;
    }

    private static int airQualityColor(double value) {
        if (value < 15.0D) return DANGER;
        if (value < 35.0D) return WARNING;
        return ACCENT;
    }

    private static final class ConfigSlider extends AbstractSliderButton {
        private final String messageKey;
        private final double min;
        private final double max;
        private final DoubleConsumer setter;

        private ConfigSlider(int x, int y, int width, String messageKey, double current,
                             double min, double max, DoubleConsumer setter) {
            super(x, y, width, 20, sliderMessage(messageKey, current), (current - min) / (max - min));
            this.messageKey = messageKey;
            this.min = min;
            this.max = max;
            this.setter = setter;
        }

        @Override
        protected void updateMessage() {
            setMessage(sliderMessage(messageKey, currentValue()));
        }

        @Override
        protected void applyValue() {
            setter.accept(currentValue());
        }

        private double currentValue() {
            return min + value * (max - min);
        }

        private static Component sliderMessage(String key, double value) {
            return Component.translatable(key, Math.round(value * 100.0D));
        }
    }
}
