package com.gmail.guitaekm.endergenesis.event;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Pair;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AllPlayerNbt {
    protected static Map<UUID, NbtCompound> savedPlayers(MinecraftServer server) {
        List<Pair<UUID, NbtCompound>> players = Arrays.stream(
                        Objects.requireNonNull(
                                server.getSavePath(WorldSavePath.PLAYERDATA).toFile().listFiles()
                        )
                ).map(file -> new Pair<>(file.getName(), file))
                .filter(pair -> !pair.getLeft().endsWith("dat_old"))
                .map(pair -> {
                    try {
                        return new Pair<>(pair.getLeft(), NbtIo.readCompressed(pair.getRight()));
                    } catch (IOException e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .map(pair -> new Pair<>(
                        UUID.fromString(pair.getLeft().substring(0, pair.getLeft().length() - 4)),
                        pair.getRight())
                        )
                .toList();
        return players.stream().collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
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
