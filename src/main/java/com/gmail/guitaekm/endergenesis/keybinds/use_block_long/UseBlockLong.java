package com.gmail.guitaekm.endergenesis.keybinds.use_block_long;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class UseBlockLong {

    static public class SavedUsage {
        public BlockPos pos;
        public int age;

        public SavedUsage(BlockPos pos, int age) {
            this.pos = pos;
            this.age = age;
        }
    }

    final private static List<ListenerInstance> listeners = new ArrayList<>();
    private static @Nullable SavedUsage current = null;
    public static void registerClass() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.options.useKey.isPressed()) {
                if (UseBlockLong.hasNewTarget(client)) {
                    UseBlockLong.callAllOnStartUse(client);
                    UseBlockLong.callAllOnUseTick(client);
                    return;
                }
                UseBlockLong.callAllOnUseTick(client);
                UseBlockLong.callAllOnEndUse(client);
                if (UseBlockLong.hasSameTarget(client)) {
                    if (UseBlockLong.current == null) {
                        throw new IllegalArgumentException("The method before should eliminate current being null. However, that's not the case");
                    }
                    UseBlockLong.current.age++;
                }
                return;
            }
            UseBlockLong.callAllOnEndUse(client);
            UseBlockLong.refreshListenerInstances();
            UseBlockLong.current = null;
        });
    }
    public static ListenerInstance registerListener(int maxAge, CallbackClient callback) {
        ListenerInstance listener = new ListenerInstance(maxAge, callback, false);
        UseBlockLong.listeners.add(listener);
        return listener;
    }

    public static boolean deregisterListener(ListenerInstance listener) {
        return UseBlockLong.listeners.remove(listener);
    }
    protected static Optional<BlockPos> getTargetBlockPos(MinecraftClient client) {
        if (client.crosshairTarget == null) {
            return Optional.empty();
        }
        if (!(client.crosshairTarget instanceof BlockHitResult result)) {
            return Optional.empty();
        }
        if (client.world == null) {
            // don't ask me why the main menu is already ticking
            return Optional.empty();
        }
        return Optional.of(result.getBlockPos());
    }
    protected static void refreshListenerInstances() {
        UseBlockLong.listeners.forEach((ListenerInstance listener) -> listener.dead = false);
    }
    protected static List<ListenerInstance> iterateOverCallbacks() {
        if (current == null) {
            return new ArrayList<>();
        }
        List<ListenerInstance> listeners = new ArrayList<>();
        for(ListenerInstance entry : UseBlockLong.listeners) {
            if (current.age <= entry.maxAge) {
                if (!entry.dead) {
                    listeners.add(entry);
                }
            }
        }
        return listeners;
    }
    protected static boolean hasNewTarget(MinecraftClient client) {
        if (!client.options.useKey.isPressed()) {
            return false;
        }
        Optional<BlockPos> targetBlockOptional = UseBlockLong.getTargetBlockPos(client);
        if (targetBlockOptional.isEmpty()) {
            return false;
        }
        return current == null;
    }
    protected static boolean hasSameTarget(MinecraftClient client) {
        if (!client.options.useKey.isPressed()) {
            return false;
        }
        Optional<BlockPos> targetBlockOptional = UseBlockLong.getTargetBlockPos(client);
        if (targetBlockOptional.isEmpty()) {
            return false;
        }
        if (current == null) {
            return false;
        }
        return targetBlockOptional.get().equals(current.pos);
    }
    protected static void callAllOnStartUse(MinecraftClient client) {
        if (UseBlockLong.hasSameTarget(client)) {
            return;
        }
        Optional<BlockPos> targetBlockOptional = UseBlockLong.getTargetBlockPos(client);
        if (targetBlockOptional.isEmpty()) {
            return;
        }
        current = new SavedUsage(targetBlockOptional.get(), 0);
        for(ListenerInstance listener : UseBlockLong.iterateOverCallbacks()) {
            listener.callback.onStartUse(client, client.world, client.player, UseBlockLong.getTargetBlockPos(client).get());
        }
    }
    protected static void callAllOnUseTick(MinecraftClient client) {
        if (!UseBlockLong.hasSameTarget(client)) {
            return;
        }
        if (current == null) {
            throw new IllegalArgumentException("The method before should eliminate current being null. However, that's not the case");
        }
        for(ListenerInstance listener : UseBlockLong.iterateOverCallbacks()) {
            listener.callback.onUseTick(client, client.world, client.player, UseBlockLong.getTargetBlockPos(client).get(), current.age);
        }
    }
    protected static void callAllOnEndUse(MinecraftClient client) {
        if (UseBlockLong.current == null) {
            return;
        }
        if (UseBlockLong.hasSameTarget(client)) {
            for(ListenerInstance listener : UseBlockLong.iterateOverCallbacks()) {
                if (current.age == listener.maxAge) {
                    listener.callback.onEndUse(client, client.world, client.player, UseBlockLong.getTargetBlockPos(client).get(), current.age);
                    listener.dead = true;
                }
            }
            return;
        }
        for(ListenerInstance listener : UseBlockLong.iterateOverCallbacks()) {
            listener.callback.onEndUse(client, client.world, client.player, UseBlockLong.current.pos, current.age);
            listener.dead = true;
        }
    }
}
