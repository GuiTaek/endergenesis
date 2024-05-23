package com.gmail.guitaekm.endergenesis.networking;

import com.gmail.guitaekm.endergenesis.teleport.VehicleTeleport;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class AcceptFinishWaitingMount {
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ModNetworking.MOUNTING_READY, new ServerPlayNetworking.PlayChannelHandler() {
            @Override
            public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
                VehicleTeleport.mountPlayer(player, player.getId());
            }
        });
    }
}
