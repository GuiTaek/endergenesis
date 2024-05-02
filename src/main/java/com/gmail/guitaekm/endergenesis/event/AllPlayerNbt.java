package com.gmail.guitaekm.endergenesis.event;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Pair;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AllPlayerNbt {
    protected static Map<UUID, NbtCompound> savedPlayers(MinecraftServer server) {
        List<Pair<UUID, NbtCompound>> players = Arrays.stream(
                        Objects.requireNonNull(
                                server.getSavePath(WorldSavePath.PLAYERDATA).toFile().listFiles()
                        )
                ).map(file -> new Pair<>(file.getName(), file))
                .filter(pair -> pair.getLeft().endsWith("dat"))
                .map(pair -> {
                    String uuidString = pair.getLeft().substring(0, pair.getLeft().length() - 4);
                    // sanitize string if this is a file that otherwise crashes the game according
                    // to #10
                    Pattern regex = Pattern.compile("(([a-fA-F0-9]+-){4})[a-fA-F0-9]+(-[0-9]+)?");
                    Matcher matcher = regex.matcher(uuidString);
                    // weirdly, without the method .matches(), the group function doesn't work
                    // probably good to have this check anyway
                    if (!matcher.matches()) {
                        return null;
                    }
                    if(matcher.group(3) != null) {
                        return null;
                    }
                    return new Pair<>(
                                    UUID.fromString(uuidString),
                                    pair.getRight()
                            );
                }).filter(Objects::nonNull)
                .map(pair -> {
                    try {
                        return new Pair<>(pair.getLeft(), NbtIo.readCompressed(pair.getRight()));
                    } catch (IOException e) {
                        return null;
                    }
                }).filter(Objects::nonNull).toList();
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
