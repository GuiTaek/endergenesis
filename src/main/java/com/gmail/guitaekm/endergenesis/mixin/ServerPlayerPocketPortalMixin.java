package com.gmail.guitaekm.endergenesis.mixin;

import com.gmail.guitaekm.endergenesis.access.IServerPlayerPocketPortalAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerPocketPortalMixin implements IServerPlayerPocketPortalAccess {
    @Unique
    private @Nullable BlockPos lastUsedPocketPortal = null;

    @Unique
    private @Nullable ChunkPos pocketDimensionPlace = null;

    @Override
    public void endergenesis$setLastUsedPocketPortal(@Nullable BlockPos position) {
        this.lastUsedPocketPortal = position;
    }

    @Override
    public @Nullable BlockPos endergenesis$getLastUsedPocketPortal() {
        return this.lastUsedPocketPortal;
    }

    @Override
    public void endergenesis$setPocketDimensionPlace(@Nullable ChunkPos pos) {
        this.pocketDimensionPlace = pos;
    }

    @Override
    public @Nullable ChunkPos endergenesis$getPocketDimensionPlace() {
        return this.pocketDimensionPlace;
    }

    @Unique
    public void writeLastUsedPocketPortal(NbtCompound nbt) {
        if (this.lastUsedPocketPortal == null) {
            return;
        }
        NbtCompound nbtCompound = new NbtCompound();

        nbtCompound.putInt("x", this.lastUsedPocketPortal.getX());
        nbtCompound.putInt("y", this.lastUsedPocketPortal.getY());
        nbtCompound.putInt("z", this.lastUsedPocketPortal.getZ());
        nbt.put("lastUsedPocketPortal", nbtCompound);
    }
    @Unique
    public void writePocketDimensionPlace(NbtCompound nbt) {
        if (this.pocketDimensionPlace == null) {
            return;
        }
        NbtCompound place = new NbtCompound();
        place.putInt("x", this.pocketDimensionPlace.x);
        place.putInt("z", this.pocketDimensionPlace.z);
        nbt.put("pocketDimensionPlace", place);
    }
    @Inject(method = "writeCustomDataToNbt", at=@At("TAIL"))
    public void writeCustomDataToNbtTail(NbtCompound nbt, CallbackInfo ci) {
        writeLastUsedPocketPortal(nbt);
        writePocketDimensionPlace(nbt);
    }

    @Unique
    public void readLastUsedPocketPortal(NbtCompound nbt) {
        if (nbt.contains("lastUsedPocketPortal", NbtElement.COMPOUND_TYPE)) {
            NbtCompound nbtCompound = nbt.getCompound("lastUsedPocketPortal");
            this.lastUsedPocketPortal = new BlockPos(nbtCompound.getInt("x"), nbtCompound.getInt("y"), nbtCompound.getInt("z"));
        }
    }
    @Unique
    public void readPocketDimensionPlace(NbtCompound nbt) {
        if (nbt.contains("pocketDimensionPlace", NbtElement.COMPOUND_TYPE)) {
            NbtCompound nbtCompound = nbt.getCompound("pocketDimensionPlace");
            this.pocketDimensionPlace = new ChunkPos(nbtCompound.getInt("x"), nbtCompound.getInt("z"));
        }
    }
    @Inject(method = "readCustomDataFromNbt", at=@At("TAIL"))
    public void readCustomDataFromNbtTail(NbtCompound nbt, CallbackInfo ci) {
        readLastUsedPocketPortal(nbt);
        readPocketDimensionPlace(nbt);
    }
}
