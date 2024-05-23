package com.gmail.guitaekm.endergenesis.networking;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public class WaitMounting {
    protected static WaitMountingPacket packet;
    public static void register() {
        ClientTickEvents.END_WORLD_TICK.register(world -> {
            if (WaitMounting.packet == null) {
                return;
            }
            if (WaitMounting.packet.checkReady(world)) {
                ClientPlayNetworking.send(ModNetworking.MOUNTING_READY, PacketByteBufs.create());
                WaitMounting.packet = null;
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(
                ModNetworking.ASK_WAITING_MOUNTING, (client, handler, buf, responseSender) -> {
                    WaitMounting.packet = new WaitMountingPacket(buf);
                });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> WaitMounting.packet = null);
    }
}
