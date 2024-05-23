package com.gmail.guitaekm.endergenesis.networking;

import com.gmail.guitaekm.endergenesis.blocks.TreeTraverser;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.*;

public class WaitMountingPacket {
    TreeTraverser<Integer> tree;
    Identifier destination;

    /**
     * creates the packet server-side
     * @param destination the world where the client should wait for
     * @param tree the riding stack
     */
    public WaitMountingPacket(ServerWorld destination, TreeTraverser<Entity> tree) {
        this.tree = tree.mapValue(Entity::getId);
        this.destination = destination.getRegistryKey().getValue();
    }
    public WaitMountingPacket(PacketByteBuf buf) {
        this.destination = buf.readIdentifier();
        List<Integer> entitiesToWaitFor = new ArrayList<>(Arrays.stream(buf.readIntArray()).boxed().toList());
        List<Integer> entityChildren = new ArrayList<>(Arrays.stream(buf.readIntArray()).boxed().toList());
        this.tree = TreeTraverser.createFromList(entityChildren, entitiesToWaitFor);
    }
    public void writeToBuf(PacketByteBuf buf) {
        buf.writeIdentifier(this.destination);
        buf.writeIntArray(this.tree.toList().stream().mapToInt(i -> i).toArray());
        buf.writeIntArray(this.tree.toChildList().stream().mapToInt(i -> i).toArray());
    }
    public boolean checkReady(ClientWorld world) {
        if (!world.getRegistryKey().getValue().equals(this.destination)) {
            return false;
        }
        for (int id : this.tree.toList()) {
            Entity entity = world.getEntityById(id);
            if (entity == null) {
                return false;
            }
        }
        return true;
    }
}
