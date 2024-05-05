package com.gmail.guitaekm.endergenesis.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.BiConsumer;

public class DeathPersistantPlayer implements ServerPlayerEvents.AfterRespawn {
    final BiConsumer<ServerPlayerEntity, NbtCompound> readNbt;
    final BiConsumer<ServerPlayerEntity, NbtCompound> writeNbt;

    public DeathPersistantPlayer(
            BiConsumer<ServerPlayerEntity, NbtCompound> readNbt,
            BiConsumer<ServerPlayerEntity, NbtCompound> writeNbt
    ) {
        this.readNbt = readNbt;
        this.writeNbt = writeNbt;
        ServerPlayerEvents.AFTER_RESPAWN.register(this);
    }

    @Override
    public void afterRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        assert oldPlayer != newPlayer;
        NbtCompound nbt = new NbtCompound();
        this.writeNbt.accept(oldPlayer, nbt);
        this.readNbt.accept(newPlayer, nbt);
    }
}
