// scraped from
// https://github.com/Ladysnake/Requiem/blob/1.19/src/main/java/ladysnake/requiem/common/screen/RiftScreenHandler.java
package com.gmail.guitaekm.endergenesis.gui;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import com.gmail.guitaekm.endergenesis.access.IServerPlayerNetherEnderworldPortal;
import com.gmail.guitaekm.endergenesis.blocks.EnderworldPortalBlock;
import com.gmail.guitaekm.endergenesis.enderling_structure.EnderlingStructureInitializer;
import com.gmail.guitaekm.endergenesis.networking.ModNetworking;
import com.gmail.guitaekm.endergenesis.networking.RequestNetherTeleport;
import com.gmail.guitaekm.endergenesis.networking.TeleportDestinations;
import com.gmail.guitaekm.endergenesis.teleport.TeleportParams;
import com.gmail.guitaekm.endergenesis.teleport.VehicleTeleport;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class TeleportScreenHandler extends ScreenHandler implements ServerPlayNetworking.PlayChannelHandler {
    // server
    public TeleportScreenHandler(
            ServerPlayerEntity player,
            int syncId
    ) {
        super(RegisterGui.TELEPORT_SCREEN_HANDLER_TYPE, syncId);
        Pair<EnderworldPortalBlock.NetherInstance, List<EnderworldPortalBlock.NetherInstance>>
                result = TeleportScreenHandler.getPlayerStoredPortals(player);
        this.source = result.getLeft();
        this.registeredEnderworldPortalPositions = result.getRight();
        ServerPlayNetworking.unregisterReceiver(player.networkHandler, ModNetworking.REQUEST_NETHER_TELEPORT);
        ServerPlayNetworking.registerReceiver(player.networkHandler, ModNetworking.REQUEST_NETHER_TELEPORT, this);
        this.vehicle = player.getVehicle();
        player.dismountVehicle();
    }
    // client
    public TeleportScreenHandler(
            int syncId, PlayerInventory inventory, PacketByteBuf buf
    ) {
        super(RegisterGui.TELEPORT_SCREEN_HANDLER_TYPE, syncId);
        TeleportDestinations packet = new TeleportDestinations(buf);
        this.source = packet.source;
        this.registeredEnderworldPortalPositions = packet.destinations;
    }

    public void requestTeleportClient(int id) {
        PacketByteBuf packet = PacketByteBufs.create();
        new RequestNetherTeleport(id).writeToPacket(packet);
        ClientPlayNetworking.send(ModNetworking.REQUEST_NETHER_TELEPORT, packet);
    }
    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        RequestNetherTeleport packet = new RequestNetherTeleport(buf);
        if (this.source != null && this.source.equals(this.registeredEnderworldPortalPositions.get(packet.destinationId))) {
            player.openHandledScreen(new RenamingScreenFactory(this.source.name(), this.source.pos()));
            return;
        }
        RegistryKey<World> netherKey = RegistryKey.of(
                Registry.WORLD_KEY,
                new Identifier("minecraft:the_nether")
        );
        this.requestTeleportServer(
                player,
                server.getWorld(netherKey),
                this.registeredEnderworldPortalPositions.get(packet.destinationId).pos()
        );
    }
    public void requestTeleportServer(ServerPlayerEntity player, ServerWorld world, BlockPos destination) {
        player.startRiding(this.vehicle);
        Optional<BlockPos> enderworldPortalOptional = EnderlingStructureInitializer.arbitraryStructureRegistry.get(
                new Identifier(EnderGenesis.MOD_ID, "enderworld_portal_lit")
        ).check(world, destination);
        if (enderworldPortalOptional.isEmpty()) {
            EnderGenesis.LOGGER.warn("bad nether teleport request, removing the destination");
            EnderworldPortalBlock.removeNetherDestination(world.getServer(), world, destination);
            return;
        }
        Optional<Vec3d> destPosOptional = VehicleTeleport.findWakeUpPosition(
                player,
                world,
                enderworldPortalOptional.get(),
                EnderworldPortalBlock.getOffsets(
                        world.getSeed(),
                        enderworldPortalOptional.get().getX(),
                        enderworldPortalOptional.get().getZ(),
                        player.hasVehicle()
                ),
                false
        );
        if (destPosOptional.isEmpty()) {
            return;
        }
        if (!payTeleportPrice(player.server, player)) {
            return;
        }
        Vec3d destPos = destPosOptional.get();
        TeleportParams params = new TeleportParams(
                player,
                world,
                enderworldPortalOptional.get(),
                destPos.getX(),
                destPos.getY(),
                destPos.getZ()
        );
        VehicleTeleport.teleportWithVehicle(params);
    }

    public boolean payTeleportPrice(MinecraftServer server, ServerPlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem().equals(Items.ENDER_PEARL)) {
                stack.split(1);
                server.getPlayerManager().sendPlayerStatus(player);
                return true;
            }
        }
        return false;
    }
    public EnderworldPortalBlock.NetherInstance source;
    public List<EnderworldPortalBlock.NetherInstance> registeredEnderworldPortalPositions;
    public Entity vehicle;

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (this.source == null) {
            return true;
        }
        BlockPos pos = this.source.pos();
        return player.squaredDistanceTo(
                (double)pos.getX() + 0.5,
                (double)pos.getY() + 0.5,
                (double)pos.getZ() + 0.5
        ) <= 64;
    }

    // it's weird that you don't get an instance of TeleportScreenHandler in ExtendedScreenFactory.writeScreenOpeningData
    // or even that this is inside ScreenHandler
    public static Pair<EnderworldPortalBlock.NetherInstance, List<EnderworldPortalBlock.NetherInstance>>
    getPlayerStoredPortals(ServerPlayerEntity player) {
        EnderworldPortalBlock.NetherInstance source = ((IServerPlayerNetherEnderworldPortal)player).endergenesis$getSource();
        List<EnderworldPortalBlock.NetherInstance> destinations = ((IServerPlayerNetherEnderworldPortal)player).endergenesis$getDestinations();
        return new Pair<>(source, destinations);
    }
}
