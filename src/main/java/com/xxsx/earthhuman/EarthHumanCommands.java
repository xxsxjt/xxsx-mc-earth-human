package com.xxsx.earthhuman;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class EarthHumanCommands {
    private EarthHumanCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("earthhuman"));
        dispatcher.register(root("eh"));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
                .then(Commands.literal("status")
                        .executes(ctx -> statusScreen(ctx.getSource(), 0))
                        .then(Commands.literal("text").executes(ctx -> status(ctx.getSource()))))
                .then(Commands.literal("panel").executes(ctx -> statusScreen(ctx.getSource(), 0)))
                .then(Commands.literal("body").executes(ctx -> body(ctx.getSource())))
                .then(Commands.literal("training").executes(ctx -> training(ctx.getSource())))
                .then(Commands.literal("exercise")
                        .executes(ctx -> exerciseHelp(ctx.getSource()))
                        .then(Commands.literal("push_up").executes(ctx -> exercise(ctx.getSource(), ExerciseAction.PUSH_UP)))
                        .then(Commands.literal("squat").executes(ctx -> exercise(ctx.getSource(), ExerciseAction.SQUAT)))
                        .then(Commands.literal("core").executes(ctx -> exercise(ctx.getSource(), ExerciseAction.CORE_TRAINING)))
                        .then(Commands.literal("stretch").executes(ctx -> exercise(ctx.getSource(), ExerciseAction.STRETCH)))
                        .then(Commands.literal("dumbbell").executes(ctx -> exercise(ctx.getSource(), ExerciseAction.DUMBBELL_CURL)))
                        .then(Commands.literal("band").executes(ctx -> exercise(ctx.getSource(), ExerciseAction.RESISTANCE_BAND_ROW))))
                .then(Commands.literal("settings").executes(ctx -> settings(ctx.getSource())))
                .then(Commands.literal("care")
                        .executes(ctx -> statusScreen(ctx.getSource(), 2))
                        .then(Commands.literal("hydrate").executes(ctx -> care(ctx.getSource(), CareAction.HYDRATE)))
                        .then(Commands.literal("bandage").executes(ctx -> care(ctx.getSource(), CareAction.BANDAGE)))
                        .then(Commands.literal("first_aid").executes(ctx -> care(ctx.getSource(), CareAction.FIRST_AID)))
                        .then(Commands.literal("warm").executes(ctx -> care(ctx.getSource(), CareAction.WARM)))
                        .then(Commands.literal("cool").executes(ctx -> care(ctx.getSource(), CareAction.COOL))))
                .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
                .then(Commands.literal("drink").executes(ctx -> drink(ctx.getSource())))
                .then(Commands.literal("reset")
                        .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(ctx -> reset(ctx.getSource())))
                .then(Commands.literal("set")
                        .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("hydration")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                        .executes(ctx -> setHydration(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "value")))))
                        .then(Commands.literal("fatigue")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                        .executes(ctx -> setFatigue(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "value")))))
                        .then(Commands.literal("temperature")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(32.0D, 42.5D))
                                        .executes(ctx -> setTemperature(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "value")))))
                        .then(Commands.literal("air_quality")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                        .executes(ctx -> setAirQuality(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "value")))))
                        .then(Commands.literal("body")
                                .then(Commands.argument("part", StringArgumentType.word())
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D))
                                                .executes(ctx -> setBodyPart(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "part"),
                                                        DoubleArgumentType.getDouble(ctx, "value")))))))
                .executes(ctx -> statusScreen(ctx.getSource(), 0));
    }

    private static int statusScreen(CommandSourceStack source, int initialTab) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        HumanNetwork.openStatus(player, initialTab);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        HumanStatus status = HumanStatus.get(player);
        source.sendSuccess(() -> Component.translatable("message.earth_human.status.title"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.status.hydration",
                Math.round(status.hydration), hydrationText(status.hydration)), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.status.temperature",
                String.format(java.util.Locale.ROOT, "%.1f", status.bodyTemperature), temperatureText(status.bodyTemperature)), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.status.fatigue",
                Math.round(status.fatigue), fatigueText(status.fatigue)), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.status.air_quality",
                Math.round(status.airQuality), airQualityText(status.airQuality)), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.status.activity",
                Component.translatable(status.currentActivity.translationKey()),
                Math.round(status.exertionLoad),
                exertionText(status.exertionLoad)), false);
        source.sendSuccess(() -> Component.translatable(
                "message.earth_human.status.rules",
                Component.translatable("config.earth_human.preset." + EarthHumanConfig.preset().id),
                Component.translatable("config.earth_human.bridge." + EarthHumanConfig.healthBridgeMode().id)), false);
        HumanBodyStatus body = HumanBodyStatus.get(player);
        BodyPart worst = body.worstPart(false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.status.body",
                Component.translatable(worst.translationKey()),
                String.format(java.util.Locale.ROOT, "%.0f", body.percent(worst) * 100.0D)), false);
        ArcanaHumanBridge arcana = ArcanaHumanBridge.read(player);
        if (arcana.hasAny()) {
            source.sendSuccess(() -> Component.translatable("message.earth_human.status.arcana",
                    arcana.cultivationLevel,
                    arcana.magicResearchLevel,
                    arcana.biguLevel,
                    arcana.extraAirSupply()), false);
        }
        source.sendSuccess(() -> Component.translatable("message.earth_human.status.hint"), false);
        return 1;
    }

    private static int settings(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        HumanNetwork.openStatus(player, 3);
        source.sendSuccess(() -> Component.translatable("message.earth_human.settings.opened"), false);
        return 1;
    }

    private static int care(CommandSourceStack source, CareAction action) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        return HumanCareActions.request(player, action.id) ? 1 : 0;
    }

    private static int body(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        HumanBodyStatus body = HumanBodyStatus.get(player);
        source.sendSuccess(() -> Component.translatable("message.earth_human.body.title"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.body.vanilla_head"), false);
        for (BodyPart part : BodyPart.values()) {
            source.sendSuccess(() -> body.describeLine(part), false);
        }
        source.sendSuccess(() -> Component.translatable("message.earth_human.body.hint"), false);
        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.title"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.status"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.body"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.training"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.settings"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.thirst"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.fatigue"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.temperature"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.air_quality"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.arcana"), false);
        return 1;
    }

    private static int drink(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        HumanStatus status = HumanStatus.get(player);
        status.hydration = Math.min(100.0D, status.hydration + 35.0D);
        status.save(player);
        HumanNetwork.sync(player, status);
        source.sendSuccess(() -> Component.translatable("message.earth_human.drink.done", Math.round(status.hydration)), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        HumanStatus status = HumanStatus.get(player);
        status.reset();
        status.save(player);
        HumanBodyStatus body = HumanBodyStatus.get(player);
        body.reset();
        body.ensureHeadHealthLimit(player);
        body.syncVanillaFromHead(player);
        body.save(player);
        HumanTrainingStatus training = HumanTrainingStatus.get(player);
        training.reset();
        training.save(player);
        HumanNetwork.sync(player, status);
        source.sendSuccess(() -> Component.translatable("message.earth_human.reset.done"), false);
        return 1;
    }

    private static int training(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        HumanTrainingStatus training = HumanTrainingStatus.get(player);
        source.sendSuccess(() -> Component.translatable("message.earth_human.training.title"), false);
        for (BodyPart part : BodyPart.values()) {
            if (part == BodyPart.HEAD) {
                continue;
            }
            source.sendSuccess(() -> training.describeLine(part), false);
        }
        source.sendSuccess(() -> Component.translatable("message.earth_human.training.hint", HumanExercise.availableActionsText()), false);
        return 1;
    }

    private static int exerciseHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("message.earth_human.training.exercise_help", HumanExercise.availableActionsText()), false);
        return 1;
    }

    private static int exercise(CommandSourceStack source, ExerciseAction action) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        return HumanExercise.apply(player, action, false) ? 1 : 0;
    }

    private static int setHydration(CommandSourceStack source, double value) {
        return set(source, value, "hydration");
    }

    private static int setFatigue(CommandSourceStack source, double value) {
        return set(source, value, "fatigue");
    }

    private static int setTemperature(CommandSourceStack source, double value) {
        return set(source, value, "temperature");
    }

    private static int setAirQuality(CommandSourceStack source, double value) {
        return set(source, value, "air_quality");
    }

    private static int setBodyPart(CommandSourceStack source, String partId, double value) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        BodyPart part = BodyPart.byId(partId);
        if (part == null) {
            source.sendFailure(Component.translatable("message.earth_human.body.unknown_part", partId));
            return 0;
        }
        HumanBodyStatus body = HumanBodyStatus.get(player);
        body.ensureHeadHealthLimit(player);
        body.setHealth(part, value);
        if (part == BodyPart.HEAD) {
            body.ensureHeadHealthLimit(player);
            body.syncVanillaFromHead(player);
        }
        body.save(player);
        HumanNetwork.syncBody(player, body);
        source.sendSuccess(() -> Component.translatable("message.earth_human.body.set.done",
                Component.translatable(part.translationKey()),
                String.format(java.util.Locale.ROOT, "%.1f", body.health(part))), false);
        return 1;
    }

    private static int set(CommandSourceStack source, double value, String field) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.earth_human.players_only"));
            return 0;
        }
        HumanStatus status = HumanStatus.get(player);
        switch (field) {
            case "hydration" -> status.hydration = value;
            case "fatigue" -> status.fatigue = value;
            case "temperature" -> status.bodyTemperature = value;
            case "air_quality" -> status.airQuality = value;
            default -> {
                source.sendFailure(Component.literal("Unknown field: " + field));
                return 0;
            }
        }
        status.save(player);
        HumanNetwork.sync(player, status);
        source.sendSuccess(() -> Component.translatable("message.earth_human.set.done", field, value), false);
        return 1;
    }

    private static Component hydrationText(double value) {
        if (value < 20.0D) return Component.translatable("message.earth_human.state.thirst.danger");
        if (value < 45.0D) return Component.translatable("message.earth_human.state.thirst.low");
        return Component.translatable("message.earth_human.state.ok");
    }

    private static Component fatigueText(double value) {
        if (value > 80.0D) return Component.translatable("message.earth_human.state.fatigue.danger");
        if (value > 55.0D) return Component.translatable("message.earth_human.state.fatigue.high");
        return Component.translatable("message.earth_human.state.ok");
    }

    private static Component exertionText(double value) {
        if (value > 70.0D) return Component.translatable("message.earth_human.state.exertion.high");
        if (value > 30.0D) return Component.translatable("message.earth_human.state.exertion.medium");
        return Component.translatable("message.earth_human.state.exertion.low");
    }

    private static Component temperatureText(double value) {
        if (value < 35.5D) return Component.translatable("message.earth_human.state.temperature.cold");
        if (value > 38.2D) return Component.translatable("message.earth_human.state.temperature.hot");
        return Component.translatable("message.earth_human.state.ok");
    }

    private static Component airQualityText(double value) {
        return Component.translatable("message.earth_human.state.air_quality." + AirQualityModel.stateId(value));
    }
}
