package com.xxsx.earthhuman;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class HumanCareActions {
    private static final String PREFIX = EarthHuman.MODID + ".care.";
    private static final String ACTIVE_ID = PREFIX + "active_id";
    private static final String ACTIVE_SLOT = PREFIX + "active_slot";
    private static final String ACTIVE_ITEM_ID = PREFIX + "active_item_id";
    private static final String ACTIVE_COMPONENTS_HASH = PREFIX + "active_components_hash";
    private static final String FINISH_TICK = PREFIX + "finish_tick";

    private HumanCareActions() {
    }

    public static void handlePayload(HumanCareActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                request(player, payload.actionId());
            }
        });
    }

    public static void handleCancelPayload(HumanCareCancelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                cancel(player);
            }
        });
    }

    public static boolean request(ServerPlayer player, String actionId) {
        CareAction action = CareAction.byId(actionId);
        if (action == null) {
            player.sendSystemMessage(Component.translatable("message.earth_human.care.unknown"));
            syncFeedback(player, "screen.earth_human.status.care.feedback.unknown");
            return false;
        }
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }

        CompoundTag data = player.getPersistentData();
        if (data.contains(ACTIVE_ID)) {
            player.sendSystemMessage(Component.translatable("message.earth_human.care.busy"));
            syncState(player, "");
            return false;
        }

        int slot = findItemSlot(player, action);
        if (slot < 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.earth_human.care.missing",
                    Component.translatable(action.titleKey())));
            syncFeedback(player, "screen.earth_human.status.care.feedback.missing");
            return false;
        }

        ItemStack selectedStack = player.getInventory().getItem(slot);
        if (!canApply(player, action, selectedStack)) {
            player.sendSystemMessage(Component.translatable("message.earth_human.care.no_effect"));
            syncFeedback(player, "screen.earth_human.status.care.feedback.no_effect");
            return false;
        }

        long now = player.level().getGameTime();
        data.putString(ACTIVE_ID, action.id);
        data.putInt(ACTIVE_SLOT, slot);
        data.putString(ACTIVE_ITEM_ID, itemId(selectedStack));
        data.putInt(ACTIVE_COMPONENTS_HASH, selectedStack.getComponents().hashCode());
        data.putLong(FINISH_TICK, now + action.durationTicks);
        player.sendSystemMessage(Component.translatable(
                "message.earth_human.care.started",
                Component.translatable(action.titleKey()),
                selectedStack.getHoverName(),
                String.format(java.util.Locale.ROOT, "%.1f", action.durationTicks / 20.0D)));
        syncState(player, "");
        return true;
    }

    public static void tick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ACTIVE_ID)) {
            return;
        }
        if (!player.isAlive()) {
            clear(data);
            syncFeedback(player, "screen.earth_human.status.care.feedback.interrupted");
            return;
        }
        long now = player.level().getGameTime();
        if (now < data.getLong(FINISH_TICK).orElse(0L)) {
            if (now % 10L == 0L) {
                syncState(player, "");
            }
            return;
        }

        CareAction action = CareAction.byId(data.getString(ACTIVE_ID).orElse(""));
        int slot = data.getIntOr(ACTIVE_SLOT, -1);
        if (action == null || slot < 0 || slot >= player.getInventory().getContainerSize()) {
            clear(data);
            syncFeedback(player, "screen.earth_human.status.care.feedback.interrupted");
            return;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (!matchesOriginalItem(data, stack, action)) {
            clear(data);
            player.sendSystemMessage(Component.translatable("message.earth_human.care.item_changed"));
            syncFeedback(player, "screen.earth_human.status.care.feedback.item_changed");
            return;
        }

        boolean applied = apply(player, action, stack);
        if (applied) {
            consumeOne(player, stack);
            player.getInventory().setChanged();
            player.sendSystemMessage(Component.translatable(
                    "message.earth_human.care.completed",
                    Component.translatable(action.titleKey())));
            clear(data);
            syncFeedback(player, "screen.earth_human.status.care.feedback.completed");
            return;
        }
        clear(data);
        player.sendSystemMessage(Component.translatable("message.earth_human.care.no_effect"));
        syncFeedback(player, "screen.earth_human.status.care.feedback.no_effect");
    }

    public static void syncState(ServerPlayer player, String feedbackKey) {
        CompoundTag data = player.getPersistentData();
        CareAction action = CareAction.byId(data.getString(ACTIVE_ID).orElse(""));
        if (action == null) {
            syncFeedback(player, feedbackKey);
            return;
        }
        long now = player.level().getGameTime();
        long finish = data.getLong(FINISH_TICK).orElse(now);
        int remaining = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, finish - now));
        HumanNetwork.syncCare(player, new HumanCareStatePayload(
                action.id,
                remaining,
                action.durationTicks,
                feedbackKey == null ? "" : feedbackKey));
    }

    public static boolean cancel(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ACTIVE_ID)) {
            syncFeedback(player, "");
            return false;
        }
        clear(data);
        player.sendSystemMessage(Component.translatable("message.earth_human.care.cancelled"));
        syncFeedback(player, "screen.earth_human.status.care.feedback.cancelled");
        return true;
    }

    public static boolean hasItem(ServerPlayer player, CareAction action) {
        return findItemSlot(player, action) >= 0;
    }

    private static int findItemSlot(ServerPlayer player, CareAction action) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(action.itemTag)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean canApply(ServerPlayer player, CareAction action, ItemStack stack) {
        if (stack.getItem() instanceof StatusConsumableItem consumable) {
            return consumable.wouldHaveEffect(player, stack);
        }
        HumanStatus status = HumanStatus.get(player);
        HumanBodyStatus body = HumanBodyStatus.get(player);
        return switch (action) {
            case HYDRATE -> status.hydration < 99.9D;
            case WARM -> status.bodyTemperature < 42.49D || status.fatigue > 0.01D;
            case COOL -> status.bodyTemperature > 32.01D || status.fatigue > 0.01D;
            case BANDAGE -> hasMissingBodyHealth(body, false);
            case FIRST_AID -> hasMissingBodyHealth(body, true);
        };
    }

    private static boolean hasMissingBodyHealth(HumanBodyStatus body, boolean includeHead) {
        for (BodyPart part : BodyPart.values()) {
            if ((includeHead || part != BodyPart.HEAD) && body.health(part) < body.maxHealth(part) - 0.01D) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesOriginalItem(CompoundTag data, ItemStack stack, CareAction action) {
        return !stack.isEmpty()
                && stack.is(action.itemTag)
                && itemId(stack).equals(data.getString(ACTIVE_ITEM_ID).orElse(""))
                && stack.getComponents().hashCode() == data.getIntOr(ACTIVE_COMPONENTS_HASH, Integer.MIN_VALUE);
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static void syncFeedback(ServerPlayer player, String feedbackKey) {
        HumanNetwork.syncCare(player, new HumanCareStatePayload(
                "",
                0,
                0,
                feedbackKey == null ? "" : feedbackKey));
    }

    private static boolean apply(ServerPlayer player, CareAction action, ItemStack stack) {
        if (stack.getItem() instanceof StatusConsumableItem consumable) {
            consumable.applyStatusEffects(player, stack);
            return true;
        }

        HumanStatus status = HumanStatus.get(player);
        HumanBodyStatus body = HumanBodyStatus.get(player);
        boolean changed = switch (action) {
            case HYDRATE -> changeStatus(status, 25.0D, 0.0D, 0.0D);
            case WARM -> changeStatus(status, 0.0D, 0.45D, -1.0D);
            case COOL -> changeStatus(status, 0.0D, -0.45D, -1.0D);
            case BANDAGE -> body.healWorst(player, 1.5D * EarthHumanConfig.healingMultiplier(), false);
            case FIRST_AID -> body.healWorst(player, 4.0D * EarthHumanConfig.healingMultiplier(), true);
        };
        status.save(player);
        body.save(player);
        HumanNetwork.sync(player, status);
        return changed;
    }

    private static boolean changeStatus(HumanStatus status, double hydration, double temperature, double fatigue) {
        double oldHydration = status.hydration;
        double oldTemperature = status.bodyTemperature;
        double oldFatigue = status.fatigue;
        status.hydration = HumanStatus.clamp(status.hydration + hydration, 0.0D, 100.0D);
        status.bodyTemperature = HumanStatus.clamp(status.bodyTemperature + temperature, 32.0D, 42.5D);
        status.fatigue = HumanStatus.clamp(status.fatigue + fatigue, 0.0D, 100.0D);
        return Math.abs(status.hydration - oldHydration) > 0.01D
                || Math.abs(status.bodyTemperature - oldTemperature) > 0.01D
                || Math.abs(status.fatigue - oldFatigue) > 0.01D;
    }

    private static void consumeOne(ServerPlayer player, ItemStack stack) {
        ItemStack remainder;
        if (stack.getItem() instanceof StatusConsumableItem consumable) {
            remainder = consumable.createRemainderStack();
        } else {
            var template = stack.getCraftingRemainder();
            remainder = template == null ? ItemStack.EMPTY : template.create();
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (!remainder.isEmpty() && !player.getAbilities().instabuild) {
            if (!player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
        }
    }

    private static void clear(CompoundTag data) {
        data.remove(ACTIVE_ID);
        data.remove(ACTIVE_SLOT);
        data.remove(ACTIVE_ITEM_ID);
        data.remove(ACTIVE_COMPONENTS_HASH);
        data.remove(FINISH_TICK);
    }
}
