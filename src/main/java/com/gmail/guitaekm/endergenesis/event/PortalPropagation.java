package com.gmail.guitaekm.endergenesis.event;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import com.gmail.guitaekm.endergenesis.access.IChunkTicketManagerAccess;
import com.gmail.guitaekm.endergenesis.blocks.EnderworldPortalBlock;
import com.gmail.guitaekm.endergenesis.worldgen.ModWorlds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class PortalPropagation implements ServerTickEvents.EndTick {
    public PortalPropagation() {
        ServerTickEvents.END_SERVER_TICK.register(this);
    }
    // configure
    public static int TICKET_EXPIRY = 5;
    @Override
    public void onEndTick(MinecraftServer server) {
        propagatedTickets.keySet().forEach(
                ticket -> propagatedTickets.put(ticket, propagatedTickets.get(ticket) + 1)
        );
        Set<PropagetedTicket> toRemove = new HashSet<>();
        propagatedTickets.keySet().forEach(
                ticket -> {
                    if (propagatedTickets.get(ticket) > TICKET_EXPIRY) {
                        toRemove.add(ticket);
                        Objects.requireNonNull(server.getWorld(ticket.world)).setChunkForced(ticket.pos.x, ticket.pos.z, false);
                    }
                }
        );
        toRemove.forEach(propagatedTickets::remove);
    }

    // for me the usual minecraft tickets don't work, and I wasn't able to get them work, therefore, I have to force-load the tickets
    public record PropagetedTicket(RegistryKey<World> world, ChunkPos pos) { }
    private final static Map<PropagetedTicket, Integer> propagatedTickets = new HashMap<>();
    public static final Set<Identifier> PROPAGATION_WHITELIST = Set.of(
            new Identifier("minecraft:overworld"),
            new Identifier(EnderGenesis.MOD_ID, "enderworld")
    );
    public static ServerWorld switchDimension(ServerWorld world) {
        if (world.getRegistryKey().equals(ModWorlds.getInfo(world.getServer()).enderworldKey())) {
            return world.getServer().getOverworld();
        }
        if (world.getRegistryKey().equals(world.getServer().getOverworld().getRegistryKey())) {
            return ModWorlds.getInfo(world.getServer()).enderworld();
        }
        // this function shall never be called when the dimension is false
        assert false;
        return Objects.requireNonNull(null);
    }
    public static void propagateEnderworldPortalTicket(
            ServerWorld world,
            WorldChunk chunk,
            BlockPos portalPos
    ) {
        if (!checkEnderworldTicketPropagationValid(
                world, chunk, portalPos
        )) {
            return;
        }
        PortalPropagation.loadChunk(
                PortalPropagation.switchDimension(world),
                toLoadChunkBlockPos(world.getServer(), world.getRegistryKey(), portalPos)
        );
    }
    public static void propagateOneWayPortalTicket(MinecraftServer server, ServerWorld world, BlockPos portalPos) {
        if (!world.getRegistryKey().equals(ModWorlds.getInfo(server).enderworldKey())) {
            return;
        }
        PortalPropagation.loadChunk(
                server.getOverworld(),
                toLoadChunkBlockPos(server, world.getRegistryKey(), portalPos)
        );
    }
    public static boolean checkEnderworldTicketPropagationValid(
            ServerWorld world,
            WorldChunk chunk,
            BlockPos pos
    ) {
        if (!PortalPropagation.PROPAGATION_WHITELIST.contains(world.getRegistryKey().getValue())) {
            return false;
        }

        if (switchDimension(world) == null) {
            // As the server first loads the overworld fully before even creating the other dimensions,
            // this has to be checked
            return false;
        }
        if (!PROPAGATION_WHITELIST.contains(world.getRegistryKey().getValue())) {
            return false;
        }
        if (chunk.getBlockState(pos).get(EnderworldPortalBlock.GENERATED)) {
            return false;
        }
        List<? extends ChunkTicketType<?>> ticketList = ((IChunkTicketManagerAccess) world.getChunkManager().threadedAnvilChunkStorage.getTicketManager())
                .endergenesis$getTicketSetPublic(new ChunkPos(pos).toLong())
                .stream()
                .map(ChunkTicket::getType)
                .toList();
        // make the set mutable
        Set<? extends ChunkTicketType<?>> ticketSet = new HashSet<>(ticketList);
        if (ticketSet.isEmpty()) {
            // I don't understand this case, but it happens
            return false;
        }
        // they are not really removed, they are just ignored by this function
        ticketSet.remove(ChunkTicketType.START);
        ticketSet.remove(ChunkTicketType.UNKNOWN);
        if (world.getRegistryKey().equals(ModWorlds.getInfo(world.getServer()).enderworldKey())) {
            // check if loaded through ticket
            if (propagatedTickets.containsKey(
                    new PropagetedTicket(
                            ModWorlds.getInfo(world.getServer()).enderworldKey(),
                            new ChunkPos(pos)
                    )
            )) {
                ticketSet.remove(ChunkTicketType.FORCED);
            }
        }
        return !ticketSet.isEmpty();
    }
    public static BlockPos toLoadChunkBlockPos(MinecraftServer server, RegistryKey<World> world, BlockPos portalPos) {
        BlockPos toLoad;
        if (world.equals(ModWorlds.getInfo(server).enderworldKey())) {
            toLoad = EnderworldPortalBlock.enderworldToOverworld(server, portalPos);
        } else {
            toLoad = EnderworldPortalBlock.overworldToEnderworldMiddle(server, portalPos);
        }
        return toLoad;
    }
    public static void loadChunk(ServerWorld world, BlockPos portalPos) {
        ChunkPos pos = new ChunkPos(portalPos);
        world.setChunkForced(pos.x, pos.z, true);
        PortalPropagation.propagatedTickets.put(
                new PropagetedTicket(world.getRegistryKey(), pos),
                0
        );
    }
}
