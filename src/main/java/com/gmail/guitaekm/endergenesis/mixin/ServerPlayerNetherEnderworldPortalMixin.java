package com.gmail.guitaekm.endergenesis.mixin;

import com.gmail.guitaekm.endergenesis.access.IServerPlayerNetherEnderworldPortal;
import com.gmail.guitaekm.endergenesis.blocks.EnderworldPortalBlock;
import com.gmail.guitaekm.endergenesis.event.DeathPersistantPlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerNetherEnderworldPortalMixin implements IServerPlayerNetherEnderworldPortal {
    // configure
    @Unique
    private static final String DEFAULT_NAME = "portal-";
    // configure
    @Unique
    private static final String FALLBACK_NAME = "no-new-name-found";
    //configure
    @Unique
    private static final int BATCH_SIZE = 1000;
    @Unique
    private final List<EnderworldPortalBlock.NetherInstance> savedDestinations = new ArrayList<>();
    @Unique
    private EnderworldPortalBlock.NetherInstance source = null;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructorTail(MinecraftServer server, ServerWorld world, GameProfile profile, @Nullable PlayerPublicKey publicKey, CallbackInfo info) {
        new DeathPersistantPlayer(
                ServerPlayerNetherEnderworldPortalMixin::readNbt,
                ServerPlayerNetherEnderworldPortalMixin::writeNbt
        );
    }

    @Override
    public void endergenesis$remove(BlockPos pos) {
        this.savedDestinations.removeIf(instance -> instance.pos().equals(pos));
    }

    @Override
    public void endergenesis$remove(EnderworldPortalBlock.NetherInstance portal) {
        this.savedDestinations.remove(portal);
    }

    @Override
    public void endergenesis$add(EnderworldPortalBlock.NetherInstance portal) {
        this.savedDestinations.add(portal);
    }

    @Override
    public List<EnderworldPortalBlock.NetherInstance> endergenesis$getDestinations() {
        return this.savedDestinations;
    }

    @Override
    public void endergenesis$setSource(EnderworldPortalBlock.@Nullable NetherInstance source) {
        this.source = source;
    }

    @Override
    public EnderworldPortalBlock.NetherInstance endergenesis$addIfNotPresent(BlockPos pos) {
        List<EnderworldPortalBlock.NetherInstance> possibleInstances = this.savedDestinations
                .stream()
                .filter(instance -> pos.equals(instance.pos()))
                .toList();
        if (possibleInstances.isEmpty()) {
            // average complexity: theta(n)
            Set<String> savedNamesSet = new HashSet<>(
                    this.savedDestinations.stream().map(EnderworldPortalBlock.NetherInstance::name).toList()
            );
            String resultName = null;
            for (int i = 0; i < 10; ++i) {
                Set<String> possibleNames = IntStream.range(BATCH_SIZE * i, BATCH_SIZE * i + BATCH_SIZE)
                        .boxed()
                        .map(id ->DEFAULT_NAME + (id + 1))
                        .collect(Collectors.toSet());
                // average complexity: theta(n)
                // source: https://www.baeldung.com/java-hashset-removeall-performance
                possibleNames.removeAll(savedNamesSet);
                if (!possibleNames.isEmpty()) {
                    resultName =  possibleNames
                            .stream().min(new Comparator<String>() {
                                public int extractNumber(String val) {
                                    return Integer.parseInt(val.substring(DEFAULT_NAME.length()));
                                }
                                @Override
                                public int compare(String o1, String o2) {
                                    return Integer.compare(extractNumber(o1), extractNumber(o2));
                                }
                            }).get();
                    break;
                }
            }
            if (resultName == null) {
                resultName = FALLBACK_NAME;
            }
            EnderworldPortalBlock.NetherInstance result = new EnderworldPortalBlock.NetherInstance(
                    this.savedDestinations.size(),
                    resultName,
                    pos
            );
            this.savedDestinations.add(result);
            return result;
        }
        assert (possibleInstances.size() == 1);
        return possibleInstances.get(0);
    }

    @Override
    public @Nullable EnderworldPortalBlock.NetherInstance endergenesis$getSource() {
        return this.source;
    }

    @Override
    public void endergenesis$setName(String currName, String newName) {
        if (!this.savedDestinations.stream().filter(
                instance -> instance.name().equals(newName)
        ).toList().isEmpty()) {
            // better would be to notify the user, but I just want to release
            return;
        }
        List<EnderworldPortalBlock.NetherInstance> result = new ArrayList<>(this.savedDestinations.stream().map(
                instance -> {
                    return instance.name().equals(currName)
                            ? new EnderworldPortalBlock.NetherInstance(instance.id(), newName, instance.pos())
                            : instance;
                }
        ).toList());
        // it sounds weird, but the complexity is the same for recreating the list and for just changing in-place
        this.savedDestinations.clear();
        this.savedDestinations.addAll(result);
    }

    @Override
    public void endergenesis$removeWithName(String name) {
        this.savedDestinations.removeIf(instance -> instance.name().equals(name));
    }

    // todo: probably needs to be in a util class
    @Unique
    private static void putPos(String name, NbtCompound nbt, BlockPos pos) {
        NbtCompound posNbt = new NbtCompound();
        posNbt.putInt("x", pos.getX());
        posNbt.putInt("y", pos.getY());
        posNbt.putInt("z", pos.getZ());
        nbt.put(name, posNbt);
    }

    // todo: probably needs to be in a util class
    @Unique
    private static BlockPos getPos(String name, NbtCompound nbt) {
        NbtCompound nbtPos = nbt.getCompound(name);
        return new BlockPos(nbtPos.getInt("x"), nbtPos.getInt("y"), nbtPos.getInt("z"));
    }

    // todo: probably needs to be in a util class
    @Unique
    private static NbtCompound instanceToNbt(EnderworldPortalBlock.NetherInstance instance) {
        NbtCompound nbtInstance = new NbtCompound();
        nbtInstance.putString("name", instance.name());
        putPos("pos", nbtInstance, instance.pos());
        nbtInstance.putInt("id", instance.id());
        return nbtInstance;
    }

    // todo: probably needs to be in a util class
    @Unique
    private static EnderworldPortalBlock.NetherInstance nbtToInstance(NbtCompound nbtInstance) {
        String instanceName = nbtInstance.getString("name");
        BlockPos pos = getPos("pos", nbtInstance);
        int id = nbtInstance.getInt("id");
        return new EnderworldPortalBlock.NetherInstance(id, instanceName, pos);
    }
    @Unique
    private static void writeNbt(ServerPlayerEntity player, NbtCompound nbt) {
        ((IServerPlayerNetherEnderworldPortal)player).endergenesis$NetherEnderworldPortal$writeNbt(nbt);
    }

    @Override
    public void endergenesis$NetherEnderworldPortal$writeNbt(NbtCompound nbt) {
        NbtList nbtDestinations = new NbtList();
        this.savedDestinations.forEach(
                netherInstance -> nbtDestinations.add(instanceToNbt(netherInstance))
        );
        nbt.put("netherDestinations", nbtDestinations);
    }

    @Inject(method = "writeCustomDataToNbt", at=@At("TAIL"))
    public void writeCustomDataToNbtTail(NbtCompound nbt, CallbackInfo ci) {
        this.endergenesis$NetherEnderworldPortal$writeNbt(nbt);
    }

    @Override
    public void endergenesis$NetherEnderworldPortal$readNbt(NbtCompound nbt) {
        this.savedDestinations.clear();
        NbtList nbtDesinations = nbt.getList("netherDestinations", NbtElement.COMPOUND_TYPE);
        this.savedDestinations.addAll(
                nbtDesinations
                        .stream()
                        .map(nbtElement -> nbtToInstance((NbtCompound) nbtElement))
                        .toList()
        );
    }

    @Unique
    private static void readNbt(ServerPlayerEntity player, NbtCompound nbt) {
        ((IServerPlayerNetherEnderworldPortal) player).endergenesis$NetherEnderworldPortal$readNbt(nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at=@At("TAIL"))
    public void readCustomDataFromNbtTail(NbtCompound nbt, CallbackInfo ci) {
        this.endergenesis$NetherEnderworldPortal$readNbt(nbt);
    }
}
