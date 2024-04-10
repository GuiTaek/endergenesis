package com.gmail.guitaekm.endergenesis.keybinds.use_block_long;

import com.gmail.guitaekm.endergenesis.networking.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;


public class SendPacketToServer implements CallbackClient {
    public static final int MAX_AGE = 40;
    public static final Set<BlockPos> dirtyBlocks = new HashSet<>();

    @Override
    public void onStartUse(MinecraftClient client, World world, PlayerEntity player, BlockPos pos) {
        if (player.isUsingItem()) {
            dirtyBlocks.add(pos);
        }
    }

    @Override
    public void onUseTick(MinecraftClient client, World world, PlayerEntity player, BlockPos pos, int age) {

    }

    @Override
    public void onEndUse(MinecraftClient client, World world, PlayerEntity player, BlockPos pos, int age) {
        boolean isDirty = player.getMainHandStack().getMaxUseTime() != 0;
        isDirty |= player.getOffHandStack().getMaxUseTime() != 0;
        isDirty |= dirtyBlocks.contains(pos);
        dirtyBlocks.remove(pos);
        if (isDirty || age < MAX_AGE) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ClientPlayNetworking.send(ModNetworking.LONG_USE_BLOCK, buf);
    }
}
