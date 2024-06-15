package com.gmail.guitaekm.endergenesis.networking;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class HandleLongUseServer {
    public interface Listener {
        void onLongUse(MinecraftServer server, ServerPlayerEntity player, BlockPos pos);
    }
    protected static List<Listener> listeners = new ArrayList<>();
    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ModNetworking.LONG_USE_BLOCK, new ServerPlayNetworking.PlayChannelHandler() {
            @Override
            public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
                BlockPos pos = buf.readBlockPos();
                for (Listener listener : HandleLongUseServer.listeners) {
                    listener.onLongUse(server, player, pos);
                }
            }
        });
    }
    public static void register(Listener listener) {
        HandleLongUseServer.listeners.add(listener);
    }
    public static void deregister(Listener listener) {
        HandleLongUseServer.listeners.remove(listener);
    }
    public static void clear(Listener listener) {
        HandleLongUseServer.listeners.clear();
    }
}
