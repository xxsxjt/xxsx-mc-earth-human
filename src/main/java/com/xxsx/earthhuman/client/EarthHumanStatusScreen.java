package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.BodyPart;
import com.xxsx.earthhuman.CareAction;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import com.xxsx.earthhuman.HumanCareStatePayload;
import com.xxsx.earthhuman.HumanStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class EarthHumanStatusScreen extends Screen {
    public static final int TAB_OVERVIEW = 0;
    public static final int TAB_BODY = 1;
    public static final int TAB_CARE = 2;
    public static final int TAB_SETTINGS = 3;

    private static final int BACKDROP = 0xC80B0F12;
    private static final int PANEL = 0xF0171D21;
    private static final int PANEL_ALT = 0xFF20282D;
    private static final int EDGE = 0xFF52616A;
    private static final int ACCENT = 0xFF63D6C5;
    private static final int INK = 0xFFF1F5F3;
    private static final int MUTED = 0xFFAAB7B8;
    private static final int DANGER = 0xFFFF6B6B;
    private static final int WARNING = 0xFFFFC857;

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
        int left = panelLeft();
        int top = panelTop();
        int navX = left + 10;
        int navY = top + 44;
        int navW = navWidth();
        addTabButton(navX, navY, navW, TAB_OVERVIEW, "screen.earth_human.status.tab.overview");
        addTabButton(navX, navY + 25, navW, TAB_BODY, "screen.earth_human.status.tab.body");
        addTabButton(navX, navY + 50, navW, TAB_CARE, "screen.earth_human.status.tab.care");
        addTabButton(navX, navY + 75, navW, TAB_SETTINGS, "screen.earth_human.status.tab.settings");

        if (tab == TAB_OVERVIEW) {
            addOverviewButtons();
        } else if (tab == TAB_CARE) {
            addCareButtons();
        } else if (tab == TAB_SETTINGS) {
            addSettingsButtons();
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.status.close"), b -> onClose())
                .bounds(left + panelWidth() - 72, top + panelHeight() - 28, 60, 20)
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

        g.fill(left, top, left + w, top + h, PANEL);
        g.outline(left, top, w, h, EDGE);
        g.fill(left + 1, top + 1, left + w - 1, top + 36, PANEL_ALT);
        int dividerX = left + navWidth() + 14;
        g.fill(dividerX, top + 37, dividerX + 1, top + h - 36, EDGE);
        g.fill(left + 1, top + 35, left + w - 1, top + 37, ACCENT);
        g.text(font, Component.translatable("screen.earth_human.status.title"), left + 14, top + 11, INK, false);
        g.text(font, fit(Component.translatable("screen.earth_human.status.subtitle"), w - 146), left + 132, top + 12, MUTED, false);

        switch (tab) {
            case TAB_BODY -> drawBody(g, left, top, w, h);
            case TAB_CARE -> drawCare(g, left, top, w, h);
            case TAB_SETTINGS -> drawSettings(g, left, top, w, h);
            default -> drawOverview(g, left, top, w, h);
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
        int left = contentLeft();
        int top = panelTop();
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.status.open_body"), b -> switchTab(TAB_BODY))
                .bounds(left, top + panelHeight() - 56, 98, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.status.open_care"), b -> switchTab(TAB_CARE))
                .bounds(left + 104, top + panelHeight() - 56, 98, 20)
                .build());
    }

    private void addCareButtons() {
        int x = panelLeft() + panelWidth() - 108;
        int y = panelTop() + 49;
        for (CareAction action : CareAction.values()) {
            Button button = addRenderableWidget(Button.builder(Component.translatable(action.titleKey()), b -> requestCare(action))
                    .bounds(x, y, 92, 20)
                    .build());
            button.setTooltip(Tooltip.create(Component.translatable(action.descriptionKey())));
            careButtons.put(action, button);
            y += 26;
        }
        cancelCareButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.earth_human.status.care.cancel"),
                        b -> EarthHumanClient.cancelCareAction())
                .bounds(x, y, 92, 16)
                .build());
        updateCareButtons();
    }

    private void requestCare(CareAction action) {
        EarthHumanClient.requestCareAction(action);
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

    private void addSettingsButtons() {
        int x = contentLeft();
        int y = panelTop() + 52;
        int gap = 6;
        int buttonW = Math.max(80, (contentWidth() - gap) / 2);

        addRenderableWidget(Button.builder(vitalsLabel(), b -> {
            HumanHudClientConfig.setShowVitalsHud(!HumanHudClientConfig.showVitalsHud());
            b.setMessage(vitalsLabel());
        }).bounds(x, y, buttonW, 20).build());
        addRenderableWidget(Button.builder(bodyHudLabel(), b -> {
            HumanHudClientConfig.setShowBodyHud(!HumanHudClientConfig.showBodyHud());
            b.setMessage(bodyHudLabel());
        }).bounds(x + buttonW + gap, y, buttonW, 20).build());

        addRenderableWidget(Button.builder(numbersLabel(), b -> {
            HumanHudClientConfig.setShowNumericValues(!HumanHudClientConfig.showNumericValues());
            b.setMessage(numbersLabel());
        }).bounds(x, y + 28, buttonW, 20).build());
        addRenderableWidget(Button.builder(themeLabel(), b -> {
            HumanHudClientConfig.cycleTheme();
            b.setMessage(themeLabel());
        }).bounds(x + buttonW + gap, y + 28, buttonW, 20).build());

        addRenderableWidget(Button.builder(cornerLabel(), b -> {
            HumanHudClientConfig.cycleCorner();
            b.setMessage(cornerLabel());
        }).bounds(x, y + 56, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.settings.size.minus"), b -> HumanHudClientConfig.changeScale(-0.1D))
                .bounds(x + buttonW + gap, y + 56, (buttonW - gap) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.settings.size.plus"), b -> HumanHudClientConfig.changeScale(0.1D))
                .bounds(x + buttonW + gap + (buttonW + gap) / 2, y + 56, (buttonW - gap) / 2, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.settings.opacity.minus"), b -> HumanHudClientConfig.changeOpacity(-0.1D))
                .bounds(x, y + 84, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_human.settings.opacity.plus"), b -> HumanHudClientConfig.changeOpacity(0.1D))
                .bounds(x + buttonW + gap, y + 84, buttonW, 20).build());
    }

    private void drawOverview(GuiGraphicsExtractor g, int left, int top, int w, int h) {
        int x = contentLeft();
        int y = top + 49;
        HumanStatus status = EarthHumanClient.syncedStatus();
        HumanBodySyncPayload body = EarthHumanClient.syncedBody();
        int bodyW = 64;
        int meterW = Math.max(110, contentWidth() - bodyW - 16);

        g.text(font, Component.translatable("screen.earth_human.status.overview.heading"), x, y, INK, false);
        drawMeter(g, x, y + 18, meterW, "screen.earth_human.status.hydration", status.hydration / 100.0D,
                Math.round(status.hydration) + "%", vitalColor(status.hydration / 100.0D, false));
        drawMeter(g, x, y + 47, meterW, "screen.earth_human.status.temperature", temperatureRatio(status.bodyTemperature),
                String.format(java.util.Locale.ROOT, "%.1f C", status.bodyTemperature), temperatureColor(status.bodyTemperature));
        drawMeter(g, x, y + 76, meterW, "screen.earth_human.status.fatigue", status.fatigue / 100.0D,
                Math.round(status.fatigue) + "%", vitalColor(status.fatigue / 100.0D, true));

        int bodyX = x + meterW + 17;
        g.text(font, Component.translatable("screen.earth_human.status.body_summary"), bodyX - 4, y, MUTED, false);
        BodyHudRenderer.draw(g, body, bodyX + 7, y + 17, 1.05D, 0.9D);

        int hintY = top + h - 91;
        g.fill(x, hintY, left + w - 14, hintY + 31, PANEL_ALT);
        g.text(font, Component.translatable("screen.earth_human.status.recommendation"), x + 7, hintY + 5, ACCENT, false);
        g.text(font, fit(recommendation(status, body), contentWidth() - 14), x + 7, hintY + 17, INK, false);
    }

    private void drawBody(GuiGraphicsExtractor g, int left, int top, int w, int h) {
        int x = contentLeft();
        int y = top + 49;
        HumanBodySyncPayload body = EarthHumanClient.syncedBody();
        g.text(font, fit(Component.translatable("screen.earth_human.status.body.heading"), contentWidth()), x, y, INK, false);
        boolean compact = contentWidth() < 250;
        double bodyScale = compact ? 1.45D : 2.15D;
        BodyHudRenderer.draw(g, body, x + (compact ? 6 : 18), y + 24, bodyScale, 1.0D);

        int barsX = x + (compact ? 64 : 104);
        int barsW = Math.max(72, left + w - 16 - barsX);
        int rowY = y + 15;
        for (BodyPart part : BodyPart.values()) {
            double ratio = body.value(part);
            drawMeter(g, barsX, rowY, barsW, part.translationKey(), ratio,
                    Math.round(ratio * 100.0D) + "%", bodyColor(ratio));
            rowY += 22;
        }
    }

    private void drawCare(GuiGraphicsExtractor g, int left, int top, int w, int h) {
        int x = contentLeft();
        int y = top + 49;
        HumanCareStatePayload careState = EarthHumanClient.syncedCareState();
        g.text(font, fit(Component.translatable("screen.earth_human.status.care.heading"), contentWidth()), x, y, INK, false);
        if (careState.active()) {
            CareAction activeAction = CareAction.byId(careState.actionId());
            Component actionName = activeAction == null
                    ? Component.translatable("screen.earth_human.status.care.running")
                    : Component.translatable(activeAction.titleKey());
            g.text(font, fit(Component.translatable(
                    "screen.earth_human.status.care.active",
                    actionName,
                    String.format(java.util.Locale.ROOT, "%.1f", careState.remainingTicks() / 20.0D)), contentWidth()),
                    x, y + 11, ACCENT, false);
        } else if (!careState.feedbackKey().isEmpty()) {
            g.text(font, fit(Component.translatable(careState.feedbackKey()), contentWidth()), x, y + 11, INK, false);
        }
        int rowY = y + 23;
        for (CareAction action : CareAction.values()) {
            ItemStack careItem = findCareItem(action);
            boolean hasItem = !careItem.isEmpty();
            int color = hasItem ? ACCENT : MUTED;
            g.fill(x, rowY - 4, left + w - 116, rowY + 18, PANEL_ALT);
            g.text(font, Component.translatable(action.titleKey()), x + 6, rowY, color, false);
            Component availability = hasItem
                    ? Component.translatable("screen.earth_human.status.care.ready", careItem.getHoverName())
                    : Component.translatable("screen.earth_human.status.care.missing");
            if (careState.active() && action.id.equals(careState.actionId())) {
                availability = Component.translatable(
                        "screen.earth_human.status.care.progress",
                        Math.round(100.0D * (1.0D - careState.remainingTicks() / (double) Math.max(1, careState.totalTicks()))));
            }
            g.text(font, fit(availability, Math.max(40, left + w - 122 - x)), x + 6, rowY + 10, hasItem ? INK : WARNING, false);
            rowY += 26;
        }
    }

    private void drawSettings(GuiGraphicsExtractor g, int left, int top, int w, int h) {
        int x = contentLeft();
        g.text(font, fit(Component.translatable("screen.earth_human.status.settings.heading"), contentWidth()), x, top + 43, INK, false);
        int previewY = top + 162;
        if (h >= 245) {
            g.text(font, Component.translatable("screen.earth_human.settings.preview"), x, previewY, MUTED, false);
            BodyHudRenderer.draw(g, EarthHumanClient.syncedBody(), x + 48, previewY - 8, 0.85D, HumanHudClientConfig.bodyHudOpacity());
            g.text(font, Component.translatable("screen.earth_human.settings.current_values",
                    Math.round(HumanHudClientConfig.bodyHudScale() * 100.0D),
                    Math.round(HumanHudClientConfig.bodyHudOpacity() * 100.0D)), x + 92, previewY + 8, INK, false);
        } else {
            g.text(font, Component.translatable("screen.earth_human.settings.current_values",
                    Math.round(HumanHudClientConfig.bodyHudScale() * 100.0D),
                    Math.round(HumanHudClientConfig.bodyHudOpacity() * 100.0D)), x, previewY, INK, false);
        }
        g.text(font, Component.translatable("screen.earth_human.settings.color_hint"), x, top + h - 48, MUTED, false);
    }

    private void drawMeter(GuiGraphicsExtractor g, int x, int y, int width, String labelKey,
                           double ratio, String value, int color) {
        double clamped = Math.max(0.0D, Math.min(1.0D, ratio));
        g.text(font, Component.translatable(labelKey), x, y, MUTED, false);
        int valueWidth = font.width(value);
        g.text(font, value, x + width - valueWidth, y, INK, false);
        g.fill(x, y + 11, x + width, y + 17, 0xFF30393E);
        g.fill(x + 1, y + 12, x + 1 + (int) Math.round((width - 2) * clamped), y + 16, color);
    }

    private Component recommendation(HumanStatus status, HumanBodySyncPayload body) {
        double worstBody = 1.0D;
        for (BodyPart part : BodyPart.values()) {
            worstBody = Math.min(worstBody, body.value(part));
        }
        if (worstBody < 0.35D) return Component.translatable("screen.earth_human.status.recommend.body");
        if (status.hydration < 35.0D) return Component.translatable("screen.earth_human.status.recommend.hydration");
        if (status.fatigue > 70.0D) return Component.translatable("screen.earth_human.status.recommend.fatigue");
        if (status.bodyTemperature < 35.8D) return Component.translatable("screen.earth_human.status.recommend.cold");
        if (status.bodyTemperature > 38.0D) return Component.translatable("screen.earth_human.status.recommend.hot");
        return Component.translatable("screen.earth_human.status.recommend.ok");
    }

    private Component vitalsLabel() {
        return Component.translatable(HumanHudClientConfig.showVitalsHud()
                ? "screen.earth_human.settings.vitals.on"
                : "screen.earth_human.settings.vitals.off");
    }

    private Component bodyHudLabel() {
        return Component.translatable(HumanHudClientConfig.showBodyHud()
                ? "screen.earth_human.settings.body.on"
                : "screen.earth_human.settings.body.off");
    }

    private Component numbersLabel() {
        return Component.translatable(HumanHudClientConfig.showNumericValues()
                ? "screen.earth_human.settings.numbers.on"
                : "screen.earth_human.settings.numbers.off");
    }

    private Component themeLabel() {
        return Component.translatable("screen.earth_human.settings.theme",
                Component.translatable(HumanHudClientConfig.hudTheme().translationKey()));
    }

    private Component cornerLabel() {
        return Component.translatable("screen.earth_human.settings.corner",
                Component.translatable(HumanHudClientConfig.bodyHudCorner().translationKey()));
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

    private Component fit(Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int contentWidth = Math.max(1, maxWidth - font.width(suffix));
        return Component.literal(font.plainSubstrByWidth(text.getString(), contentWidth) + suffix);
    }

    private int panelWidth() {
        return Math.min(440, Math.max(300, width - 12));
    }

    private int panelHeight() {
        return Math.min(260, Math.max(224, height - 24));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    private int contentLeft() {
        return panelLeft() + navWidth() + 26;
    }

    private int contentWidth() {
        return panelWidth() - navWidth() - 40;
    }

    private int navWidth() {
        return panelWidth() < 380 ? 68 : 82;
    }

    private static double temperatureRatio(double temperature) {
        return (temperature - 32.0D) / 10.5D;
    }

    private static int temperatureColor(double temperature) {
        if (temperature < 35.5D) return 0xFF5DADE2;
        if (temperature > 38.2D) return 0xFFFF9F43;
        return 0xFF72D6A0;
    }

    private static int vitalColor(double ratio, boolean reverse) {
        double health = reverse ? 1.0D - ratio : ratio;
        if (health < 0.25D) return DANGER;
        if (health < 0.55D) return WARNING;
        return ACCENT;
    }

    private static int bodyColor(double ratio) {
        if (ratio <= 0.01D) return 0xFF111111;
        if (ratio < 0.30D) return DANGER;
        if (ratio < 0.65D) return WARNING;
        return 0xFF72D6A0;
    }
}
