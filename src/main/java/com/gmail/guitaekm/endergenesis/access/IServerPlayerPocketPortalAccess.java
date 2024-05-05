package com.gmail.guitaekm.endergenesis.access;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.math.ChunkPos;

public interface IServerPlayerPocketPortalAccess {
    void endergenesis$setLastUsedPocketPortal(@Nullable BlockPos position);
    @Nullable BlockPos endergenesis$getLastUsedPocketPortal();
    void endergenesis$setPocketDimensionPlace(@Nullable ChunkPos position);
    @Nullable ChunkPos endergenesis$getPocketDimensionPlace();
    void endergenesis$PocketPortal$writeNbt(NbtCompound nbt);
    void endergenesis$PocketPortal$readNbt(NbtCompound nbt);
}
