package com.xxsx.earthhuman.client;

import com.xxsx.earthhuman.EarthHuman;
import com.xxsx.earthhuman.EarthHumanTooltipProvider;
import com.xxsx.earthhuman.CareAction;
import com.xxsx.earthhuman.HumanCareActionPayload;
import com.xxsx.earthhuman.HumanCareCancelPayload;
import com.xxsx.earthhuman.HumanCareStatePayload;
import com.xxsx.earthhuman.HumanExerciseSessionPayload;
import com.xxsx.earthhuman.ExerciseAction;
import com.xxsx.earthhuman.HumanExerciseActionPayload;
import com.xxsx.earthhuman.HumanTrainingSyncPayload;
import com.xxsx.earthhuman.HumanBodySyncPayload;
import com.xxsx.earthhuman.HumanActivityType;
import com.xxsx.earthhuman.HumanOpenSettingsPayload;
import com.xxsx.earthhuman.HumanOpenStatusPayload;
import com.xxsx.earthhuman.HumanOpenGuidePayload;
import com.xxsx.earthhuman.HumanStatus;
import com.xxsx.earthhuman.HumanStatusSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class EarthHumanClient {
    private static HumanStatus syncedStatus = new HumanStatus(100.0D, 37.0D, 0.0D, 0);
    private static HumanBodySyncPayload syncedBody = new HumanBodySyncPayload(
            1.0D, 1.0D, 1.0D, 0.0D, 0.0D);
    private static HumanCareStatePayload syncedCareState = HumanCareStatePayload.IDLE;
    private static HumanExerciseSessionPayload syncedExercise = HumanExerciseSessionPayload.IDLE;
    private static HumanTrainingSyncPayload syncedTraining = HumanTrainingSyncPayload.EMPTY;
    private static CameraType cameraBeforeExercise;
    private static boolean exerciseCameraForced;
    private static int careAnimationTicker;
    private static int exerciseSwingTicker;

    private EarthHumanClient() {
    }

    public static void register(IEventBus modBus, ModContainer modContainer) {
        HumanHudClientConfig.load();
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new EarthHumanSettingsScreen(parent));
        modBus.addListener(EarthHumanClient::registerGuiLayers);
        modBus.addListener(EarthHumanClient::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(EarthHumanClient::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(EarthHumanClient::onContainerForeground);
        NeoForge.EVENT_BUS.addListener(EarthHumanClient::onScreenMousePressed);
        NeoForge.EVENT_BUS.addListener(EarthHumanClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(EarthHumanClient::onRenderPlayer);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, EarthHuman.id("body_status"), HumanHudOverlay::render);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(HumanStatusSyncPayload.TYPE, EarthHumanClient::handleStatusSync);
        event.register(HumanBodySyncPayload.TYPE, EarthHumanClient::handleBodySync);
        event.register(HumanTrainingSyncPayload.TYPE, EarthHumanClient::handleTrainingSync);
        event.register(HumanOpenSettingsPayload.TYPE, EarthHumanClient::handleOpenSettings);
        event.register(HumanOpenStatusPayload.TYPE, EarthHumanClient::handleOpenStatus);
        event.register(HumanOpenGuidePayload.TYPE, EarthHumanClient::handleOpenGuide);
        event.register(HumanCareStatePayload.TYPE, EarthHumanClient::handleCareState);
        event.register(HumanExerciseSessionPayload.TYPE, EarthHumanClient::handleExerciseSession);
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof EarthHumanTooltipProvider provider) {
            provider.appendEarthHumanTooltip(event.getToolTip()::add);
        }
    }

    private static void onContainerForeground(ScreenEvent.Render.Foreground event) {
        InventoryBodySummary.render(event, syncedBody);
    }

    private static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0) {
            return;
        }
        if (InventoryBodySummary.hitTestCultivationHub(event.getScreen(), event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
            openCultivationHub(event.getScreen());
            return;
        }
        if (!InventoryBodySummary.hitTest(event.getScreen(), event.getMouseX(), event.getMouseY())) {
            return;
        }
        event.setCanceled(true);
        Minecraft.getInstance().setScreen(new BodyPartScreen(event.getScreen()));
    }

    private static void handleStatusSync(HumanStatusSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> syncedStatus = new HumanStatus(
                payload.hydration(), payload.bodyTemperature(), payload.fatigue(), 0,
                payload.exertionLoad(), HumanActivityType.byNetworkId(payload.currentActivityId()), 0,
                payload.airQuality()));
    }

    private static void handleBodySync(HumanBodySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> syncedBody = payload);
    }

    private static void handleTrainingSync(HumanTrainingSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> syncedTraining = payload);
    }

    private static void handleOpenSettings(HumanOpenSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openStatusScreen(3));
    }

    private static void handleOpenStatus(HumanOpenStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openStatusScreen(payload.initialTab()));
    }

    private static void handleOpenGuide(HumanOpenGuidePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new EarthHumanGuideScreen(null)));
    }

    private static void handleCareState(HumanCareStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> syncedCareState = payload);
    }

    /**
     * A station exercise switches to third person while it runs, so the player can see the action,
     * and restores whatever view was in use before it started.
     */
    private static void handleExerciseSession(HumanExerciseSessionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            syncedExercise = payload;
            Minecraft minecraft = Minecraft.getInstance();
            if (payload.active()) {
                if (!exerciseCameraForced) {
                    cameraBeforeExercise = minecraft.options.getCameraType();
                    exerciseCameraForced = true;
                }
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            } else if (exerciseCameraForced) {
                restoreExerciseCamera(minecraft);
            }
        });
    }

    private static void restoreExerciseCamera(Minecraft minecraft) {
        exerciseCameraForced = false;
        minecraft.options.setCameraType(
                cameraBeforeExercise == null ? CameraType.FIRST_PERSON : cameraBeforeExercise);
        cameraBeforeExercise = null;
    }

    /**
     * A care action is owned by the server and has no item use behind it, so the client supplies
     * the visible motion: one arm swing every ten ticks while the action runs.
     */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            careAnimationTicker = 0;
            exerciseSwingTicker = 0;
            if (exerciseCameraForced) {
                restoreExerciseCamera(minecraft);
            }
            return;
        }
        tickExerciseAnimation(minecraft, player);
        tickCareAnimation(minecraft, player);
    }

    private static void tickExerciseAnimation(Minecraft minecraft, LocalPlayer player) {
        if (!syncedExercise.active()) {
            exerciseSwingTicker = 0;
            return;
        }
        if (minecraft.screen != null || player.isUsingItem()) {
            return;
        }
        // Never lock or modify camera pitch/yaw (setXRot/setYRot) or force jumpFromGround().
        // Instead, alternate smooth arm swings and spawn sweat/breath particles while RenderPlayerEvent animates the pose.
        exerciseSwingTicker++;
        String act = syncedExercise.actionId();
        int swingInterval = "stretch".equals(act) ? 8 : 6;
        if (exerciseSwingTicker % swingInterval == 0) {
            InteractionHand hand = ((exerciseSwingTicker / swingInterval) % 2 == 0)
                    ? InteractionHand.MAIN_HAND
                    : InteractionHand.OFF_HAND;
            player.swing(hand);
        }
        if (minecraft.level != null && exerciseSwingTicker % 8 == 0) {
            double ox = (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.45D;
            double oz = (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.45D;
            minecraft.level.addParticle(ParticleTypes.SPLASH,
                    player.getX() + ox, player.getY() + 1.15D, player.getZ() + oz,
                    0.0D, 0.02D, 0.0D);
            if (exerciseSwingTicker % 16 == 0) {
                minecraft.level.addParticle(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 1.50D, player.getZ(),
                        0.0D, 0.01D, 0.0D);
                minecraft.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 0.25F, 1.08F, false);
            }
        }
    }

    private static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        if (!syncedExercise.active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || event.getRenderState().id != minecraft.player.getId()) {
            return;
        }
        PlayerModel model = event.getRenderer().getModel();
        float time = exerciseSwingTicker + event.getPartialTick();
        float wave = Mth.sin(time * 0.45F);
        float rep = (wave + 1.0F) * 0.5F; // 0..1 smooth rep cycle
        String act = syncedExercise.actionId();

        switch (act) {
            case "push_up" -> {
                model.body.xRot = 0.55F + 0.28F * rep;
                model.rightArm.xRot = -1.35F + 0.55F * rep;
                model.leftArm.xRot = -1.35F + 0.55F * rep;
                model.rightArm.zRot = 0.18F * rep;
                model.leftArm.zRot = -0.18F * rep;
                model.rightLeg.xRot = 0.15F * rep;
                model.leftLeg.xRot = 0.15F * rep;
            }
            case "squat" -> {
                model.body.xRot = 0.36F * rep;
                model.rightLeg.xRot = -0.72F * rep;
                model.leftLeg.xRot = -0.72F * rep;
                model.rightLeg.zRot = -0.08F * rep;
                model.leftLeg.zRot = 0.08F * rep;
                model.rightArm.xRot = -1.15F * rep;
                model.leftArm.xRot = -1.15F * rep;
            }
            case "sit_up", "core" -> {
                model.body.xRot = -0.20F + 0.65F * rep;
                model.rightLeg.xRot = -0.85F;
                model.leftLeg.xRot = -0.85F;
                model.rightArm.xRot = -1.65F + 0.35F * rep;
                model.leftArm.xRot = -1.65F + 0.35F * rep;
                model.rightArm.yRot = -0.25F;
                model.leftArm.yRot = 0.25F;
            }
            case "pull_up" -> {
                model.rightArm.xRot = -2.75F + 0.55F * rep;
                model.leftArm.xRot = -2.75F + 0.55F * rep;
                model.rightArm.zRot = -0.15F;
                model.leftArm.zRot = 0.15F;
                model.body.xRot = -0.10F * rep;
                model.rightLeg.xRot = 0.22F * wave;
                model.leftLeg.xRot = -0.22F * wave;
            }
            case "stretch" -> {
                model.rightArm.xRot = -0.85F - 0.50F * rep;
                model.leftArm.xRot = -0.85F - 0.50F * rep;
                model.rightArm.zRot = 0.60F * rep;
                model.leftArm.zRot = -0.60F * rep;
                model.body.xRot = -0.12F * rep;
            }
            default -> {
                model.rightArm.xRot = -0.90F - 0.75F * rep;
                model.leftArm.xRot = -0.90F - 0.75F * (1.0F - rep);
                model.body.xRot = 0.12F * rep;
            }
        }
        copyPose(model.rightArm, model.rightSleeve);
        copyPose(model.leftArm, model.leftSleeve);
        copyPose(model.rightLeg, model.rightPants);
        copyPose(model.leftLeg, model.leftPants);
        copyPose(model.body, model.jacket);
    }

    private static void copyPose(ModelPart source, ModelPart target) {
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
        target.xScale = source.xScale;
        target.yScale = source.yScale;
        target.zScale = source.zScale;
    }

    /**
     * A care action is owned by the server and has no item use behind it, so the client supplies
     * the visible motion: one arm swing every ten ticks while the action runs.
     */
    private static void tickCareAnimation(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.screen != null || !syncedCareState.active()) {
            careAnimationTicker = 0;
            return;
        }
        if (player.isUsingItem()) {
            return;
        }
        if (++careAnimationTicker % 10 == 0) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static void renderBodyStatus(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        HumanHudOverlay.render(graphics, deltaTracker);
    }

    public static void openSettingsScreen() {
        openStatusScreen(3);
    }

    public static void openStatusScreen(int initialTab) {
        Minecraft minecraft = Minecraft.getInstance();
        if (initialTab == EarthHumanStatusScreen.TAB_BODY) {
            minecraft.setScreen(new BodyPartScreen(null));
            return;
        }
        minecraft.setScreen(new EarthHumanStatusScreen(null, initialTab));
    }

    public static void openCultivationHub(net.minecraft.client.gui.screens.Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new CultivationHubScreen(parent));
    }

    public static HumanBodySyncPayload currentBodyPayload() {
        return syncedBody;
    }

    static HumanStatus syncedStatus() {
        return syncedStatus;
    }

    static HumanBodySyncPayload syncedBody() {
        return syncedBody;
    }

    static HumanTrainingSyncPayload syncedTraining() {
        return syncedTraining;
    }

    static HumanCareStatePayload syncedCareState() {
        return syncedCareState;
    }

    static HumanExerciseSessionPayload syncedExercise() {
        return syncedExercise;
    }

    static void requestCareAction(CareAction action) {
        ClientPacketDistributor.sendToServer(new HumanCareActionPayload(action.id));
    }

    static void requestExerciseAction(ExerciseAction action) {
        ClientPacketDistributor.sendToServer(new HumanExerciseActionPayload(action.id));
    }

    static void stopExerciseAction() {
        ClientPacketDistributor.sendToServer(new HumanExerciseActionPayload("stop"));
    }

    static void cancelCareAction() {
        ClientPacketDistributor.sendToServer(HumanCareCancelPayload.INSTANCE);
    }
}
