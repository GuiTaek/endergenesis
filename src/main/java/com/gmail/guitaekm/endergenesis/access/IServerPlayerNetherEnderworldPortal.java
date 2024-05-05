package com.gmail.guitaekm.endergenesis.access;

import com.gmail.guitaekm.endergenesis.blocks.EnderworldPortalBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IServerPlayerNetherEnderworldPortal {
    void endergenesis$remove(BlockPos pos);
    void endergenesis$remove(EnderworldPortalBlock.NetherInstance portal);
    void endergenesis$add(EnderworldPortalBlock.NetherInstance portal);
    List<EnderworldPortalBlock.NetherInstance> endergenesis$getDestinations();
    void endergenesis$setSource(@Nullable EnderworldPortalBlock.NetherInstance source);
    EnderworldPortalBlock.NetherInstance endergenesis$addIfNotPresent(BlockPos pos);
    @Nullable EnderworldPortalBlock.NetherInstance endergenesis$getSource();
    void endergenesis$setName(String currName, String newName);
    void endergenesis$removeWithName(String name);
    void endergenesis$NetherEnderworldPortal$readNbt(NbtCompound nbt);
    void endergenesis$NetherEnderworldPortal$writeNbt(NbtCompound nbt);
}
