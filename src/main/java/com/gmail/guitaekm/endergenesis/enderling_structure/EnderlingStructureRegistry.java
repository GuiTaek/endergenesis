package com.gmail.guitaekm.endergenesis.enderling_structure;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import com.gmail.guitaekm.endergenesis.event.EnderlingStructureEvents;
import com.gmail.guitaekm.endergenesis.networking.HandleLongUseServer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.*;

public class EnderlingStructureRegistry implements
        ServerLifecycleEvents.ServerStarting,
        SimpleSynchronousResourceReloadListener,
        HandleLongUseServer.Listener {
    final private Map<Identifier, Pair<Identifier, Identifier>> uninitializedEnderlingStructures;
    final private Map<Identifier, EnderlingStructure> enderlingStructures;

    public EnderlingStructureRegistry() {
        this.uninitializedEnderlingStructures = new HashMap<>();
        this.enderlingStructures = new HashMap<>();
    }

    public void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(
                new Identifier(EnderGenesis.MOD_ID, "enderling_structure_registry"),
                this
        );
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(this);
        HandleLongUseServer.register(this);
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier("enderling_structure_new");
    }

    @Override
    public void reload(ResourceManager manager) {
        this.uninitializedEnderlingStructures.clear();
        this.enderlingStructures.clear();
        for(Identifier id : manager.findResources("enderling_structures_new", path -> path.endsWith(".json"))) {
            try {
                StringWriter writer = new StringWriter();
                IOUtils.copy(manager.getResource(id).getInputStream(), writer, "UTF-8");
                JsonElement json = JsonParser.parseString(writer.toString());
                Identifier enderlingStructureId = new Identifier(
                        id.getNamespace(),
                        id.getPath()
                                .split("enderling_structures_new/")[1]
                                .split(".json")[0]
                );
                // this is so small no need for an extra class
                Identifier convertible = new Identifier(json.getAsJsonObject().get("convertible").getAsString());
                Identifier placeable = new Identifier(json.getAsJsonObject().get("placeable").getAsString());
                this.uninitializedEnderlingStructures.put(enderlingStructureId, new Pair<>(convertible, placeable));
            } catch(Exception e) {
                EnderGenesis.LOGGER.error("Error occurred while loading resource json " + id.toString(), e);
            }
        }
    }

    @Override
    public void onServerStarting(MinecraftServer server) {
        for (Map.Entry<Identifier, Pair<Identifier, Identifier>> entry : this.uninitializedEnderlingStructures.entrySet()) {
            ArbitraryStructure convertible = EnderlingStructureInitializer.arbitraryStructureRegistry.get(entry.getValue().getLeft());
            if (convertible == null) {
                throw new IllegalArgumentException(
                        MessageFormat.format(
                                "Convertible \"{0}\" in enderling structure \"{1}\" doesn't exists.",
                                entry.getValue().getLeft(),
                                entry.getKey().toString())
                );
            }
            ArbitraryStructure placeable = EnderlingStructureInitializer.arbitraryStructureRegistry.get(entry.getValue().getRight());
            if (placeable == null) {
                throw new IllegalArgumentException(
                        MessageFormat.format(
                                "Placeable \"{0}\" in enderling structure \"{1}\" doesn't exists.",
                                entry.getValue().getRight(),
                                entry.getKey().toString())
                );
            }
            this.enderlingStructures.put(entry.getKey(), new EnderlingStructure(convertible, placeable));
        }
    }
    public EnderlingStructure get(Identifier id) {
        return this.enderlingStructures.get(id);
    }

    // called only by server
    @Override
    public void onLongUse(MinecraftServer server, ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = player.getWorld();
        Identifier id = this.findEnderlingStructure(player, pos);
        EnderlingStructure structure = this.get(id);
        if (id == null) {
            return;
        }
        Optional<BlockPos> resultPos = structure.convertible().check(world, pos);
        assert resultPos.isPresent();
        if (EnderlingStructureEvents.ON_CONVERT.invoker().onConvert(
                player,
                world,
                id,
                structure,
                resultPos.get()
        )) {
            structure.placeable().place(player.getWorld(), resultPos.get(), new Vec3i(0, 0, 0), Block.NOTIFY_ALL | Block.FORCE_STATE);
        }
    }
    // called by client and server
    public @Nullable Identifier findEnderlingStructure(PlayerEntity player, BlockPos pos) {
        List<Identifier> ids = new ArrayList<>(this.enderlingStructures.keySet());
        // there could be multiple structures possible. In that case, it shall be random which one is chosen
        Collections.shuffle(ids);
        for (Identifier id : ids) {
            EnderlingStructure structure = this.get(id);
            Optional<BlockPos> resultPos = structure.convertible().check(player.getWorld(), pos);
            if(resultPos.isPresent()) {
                return id;
            }
        }
        return null;
    }
}
