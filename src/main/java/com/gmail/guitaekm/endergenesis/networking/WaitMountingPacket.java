package com.gmail.guitaekm.endergenesis.networking;

import com.gmail.guitaekm.endergenesis.blocks.TreeTraverser;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.*;

public class WaitMountingPacket {
    List<Integer> entitiesToWaitFor;
    Identifier destination;

    /**
     * creates the packet server-side
     * @param destination the world where the client should wait for
     * @param tree the riding stack
     */
    public WaitMountingPacket(ServerWorld destination, TreeTraverser<Entity> tree) {
        Set<Integer> entitiesToWaitForSet = new HashSet<>();
        tree.depthFirstSearch((Entity parent, Entity child) -> {
            // every in-between entity is added twice, but not the root and the leaves
            entitiesToWaitForSet.add(parent.getId());
            entitiesToWaitForSet.add(child.getId());
        });
        this.entitiesToWaitFor = entitiesToWaitForSet.stream().toList();
        this.destination = destination.getRegistryKey().getValue();
    }
    public WaitMountingPacket(PacketByteBuf buf) {
        this.destination = buf.readIdentifier();
        this.entitiesToWaitFor = new ArrayList<>();
        this.entitiesToWaitFor.addAll(Arrays.stream(buf.readIntArray()).boxed().toList());
    }
    public void writeToBuf(PacketByteBuf buf) {
        buf.writeIdentifier(this.destination);
        buf.writeIntArray(this.entitiesToWaitFor.stream().mapToInt(i->i).toArray());
    }
    public boolean checkReady(ClientWorld world) {
        if (!world.getRegistryKey().getValue().equals(this.destination)) {
            return false;
        }
        for (int id : this.entitiesToWaitFor) {
            if (world.getEntityById(id) == null) {
                return false;
            }
            if (world.getEntityById(id).hasVehicle()) {
                world.getEntityById(id).stopRiding();
                return false;
            }
        }
        return true;
    }
}
