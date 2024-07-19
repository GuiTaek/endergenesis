package com.gmail.guitaekm.endergenesis.gui;

import com.gmail.guitaekm.endergenesis.access.IServerPlayerNetherEnderworldPortal;
import com.gmail.guitaekm.endergenesis.blocks.EnderworldPortalBlock;
import com.gmail.guitaekm.endergenesis.networking.AnswerRenamingRequest;
import com.gmail.guitaekm.endergenesis.networking.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class RenamingScreenHandler extends ScreenHandler implements ServerPlayNetworking.PlayChannelHandler {
    public String currName;
    public BlockPos pos;

    // server
    public RenamingScreenHandler(int syncId, String name, BlockPos pos, ServerPlayerEntity player) {
        super(RegisterGui.RENAMING_SCREEN_SCREEN_HANDLER_TYPE, syncId);
        this.currName = name;
        this.pos = pos;
        ServerPlayNetworking.registerReceiver(
                player.networkHandler,
                ModNetworking.ANSWER_RENAMING_REQUEST,
                this
        );
    }

    // client
    public RenamingScreenHandler(
            int syncId, PlayerInventory inventory, PacketByteBuf buf
    ) {
        super(RegisterGui.RENAMING_SCREEN_SCREEN_HANDLER_TYPE, syncId);
        this.currName = buf.readString();
        this.pos = buf.readBlockPos();
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.squaredDistanceTo(
                (double) pos.getX() + 0.5,
                (double) pos.getY() + 0.5,
                (double) pos.getZ() + 0.5
        ) <= 64;
    }

    // client
    public void sendAnswer(String name, AnswerRenamingRequest.ButtonPressed button) {
        AnswerRenamingRequest packet = new AnswerRenamingRequest(name, button);
        PacketByteBuf buf = PacketByteBufs.create();
        packet.writePacket(buf);
        ClientPlayNetworking.send(ModNetworking.ANSWER_RENAMING_REQUEST, buf);
    }

    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        player.closeHandledScreen();
        IServerPlayerNetherEnderworldPortal playerEnderworldPortal = (IServerPlayerNetherEnderworldPortal)player;
        AnswerRenamingRequest packet = new AnswerRenamingRequest(buf);
        if (packet.button == AnswerRenamingRequest.ButtonPressed.OK) {
            playerEnderworldPortal.endergenesis$setName(this.currName, packet.newName);
            EnderworldPortalBlock.NetherInstance instance = playerEnderworldPortal
                    .endergenesis$getDestinations()
                    .stream()
                    .filter(checkInstance ->
                            checkInstance.pos().equals(this.pos)).findFirst().orElse(null);
            playerEnderworldPortal.endergenesis$setSource(instance);
        }
        if (packet.button == AnswerRenamingRequest.ButtonPressed.FORGET) {
            playerEnderworldPortal.endergenesis$removeWithName(this.currName);
        }
        if (packet.button == AnswerRenamingRequest.ButtonPressed.CANCEL) {
            EnderworldPortalBlock.NetherInstance instance = playerEnderworldPortal
                    .endergenesis$getDestinations()
                    .stream()
                    .filter(checkInstance ->
                            checkInstance.pos().equals(this.pos)).findFirst().orElse(null);
            playerEnderworldPortal.endergenesis$setSource(instance);
        }
        player.openHandledScreen(new TeleportScreenFactory());
        ServerPlayNetworking.unregisterReceiver(player.networkHandler, ModNetworking.ANSWER_RENAMING_REQUEST);
    }
}
