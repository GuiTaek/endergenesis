package com.gmail.guitaekm.endergenesis.gui;

import com.gmail.guitaekm.endergenesis.blocks.EnderworldPortalBlock;
import com.gmail.guitaekm.endergenesis.networking.TeleportDestinations;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TeleportScreenFactory implements ExtendedScreenHandlerFactory {
    @Override
    public Text getDisplayName() {
        return TranslateString.translate("teleport_title");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new TeleportScreenHandler((ServerPlayerEntity) player, syncId);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        Pair<EnderworldPortalBlock.NetherInstance, List<EnderworldPortalBlock.NetherInstance>>
                result = TeleportScreenHandler.getPlayerStoredPortals(player);
        new TeleportDestinations(result.getLeft(), result.getRight()).writeToPacket(buf);
    }
}
