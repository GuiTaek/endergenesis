package com.gmail.guitaekm.endergenesis.event;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.core.jmx.Server;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class AllPlayerNbt {
    protected static Map<UUID, NbtCompound> savedPlayers(MinecraftServer server) {
        List<UUID> players = Arrays.stream(
                        Objects.requireNonNull(
                                server.getSavePath(WorldSavePath.PLAYERDATA).toFile().listFiles()
                        )
                ).map(File::getName)
                .filter(filename -> !filename.endsWith("dat_old"))
                .map(filename -> filename.substring(0, filename.length() - 4))
                .map(UUID::fromString).toList();
        return players.stream().map((UUID uuid) -> new Pair<>(uuid, new GameProfile(uuid, null)))
                .map(pair ->
                        new Pair<>(
                                pair.getLeft(),
                                server.getPlayerManager().loadPlayerData(new ServerPlayerEntity(server, server.getOverworld(), pair.getRight()))
                        )
                ).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }
    protected static Map<UUID, NbtCompound> onlinePlayers(MinecraftServer server) {
        return server
                .getPlayerManager()
                .getPlayerList()
                .stream().map(player -> new Pair<>(player.getUuid(), player.writeNbt(new NbtCompound())))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }
    public static Map<UUID, NbtCompound> getPlayerNbts(MinecraftServer server) {
        Map<UUID, NbtCompound> result = savedPlayers(server);
        result.putAll(onlinePlayers(server));
        return result;
    }
}
