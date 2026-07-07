package com.xxsx.earthhuman;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
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
                                        .executes(ctx -> setTemperature(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "value"))))))
                .executes(ctx -> status(ctx.getSource()));
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

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.title"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.status"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.thirst"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.fatigue"), false);
        source.sendSuccess(() -> Component.translatable("message.earth_human.help.temperature"), false);
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
        HumanNetwork.sync(player, status);
        source.sendSuccess(() -> Component.translatable("message.earth_human.reset.done"), false);
        return 1;
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

    private static Component temperatureText(double value) {
        if (value < 35.5D) return Component.translatable("message.earth_human.state.temperature.cold");
        if (value > 38.2D) return Component.translatable("message.earth_human.state.temperature.hot");
        return Component.translatable("message.earth_human.state.ok");
    }
}
