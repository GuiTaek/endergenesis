package com.gmail.guitaekm.endergenesis.enderling_structure;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ArbitraryStructureRegistry implements SimpleSynchronousResourceReloadListener, ServerLifecycleEvents.ServerStarting {
    private final Map<Identifier, NbtCompound> arbitraryStructuresUninitialized = new HashMap<>();
    private final Map<Identifier, ArbitraryStructure> arbitraryStructures = new HashMap<>();

    @Override
    public void onServerStarting(MinecraftServer server) {
        for (Map.Entry<Identifier, NbtCompound> entry : this.arbitraryStructuresUninitialized.entrySet()) {
            this.arbitraryStructures.put(
                    entry.getKey(),
                    new ArbitraryStructure(entry.getValue())
            );
        }
    }
    public void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(this);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(this);
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier(EnderGenesis.MOD_ID, "arbitrary_structures");
    }

    @Override
    public void reload(ResourceManager manager) {
        this.arbitraryStructuresUninitialized.clear();
        this.arbitraryStructures.clear();
        for(Identifier id : manager.findResources("structures", path -> path.getPath().endsWith(".nbt")).keySet()) {
            Identifier modifiedId = new Identifier(
                    id.getNamespace(),
                    id.getPath()
                            .split("structures/")[1]
                            .split(".nbt")[0]
            );
            try {
                Optional<Resource> resource = manager.getResource(id);
                if (resource.isEmpty()) {
                    // no idea how this could happen
                    continue;
                }
                NbtCompound nbt = NbtIo.readCompressed(resource.get().getInputStream());
                this.arbitraryStructuresUninitialized.put(modifiedId, nbt);
            } catch(IOException e) {
                EnderGenesis.LOGGER.error("Error with file opening of enderling structure json {}", id.toString(), e);
                throw new RuntimeException("handle exception while loading data pack");
            } catch(Exception e) {
                EnderGenesis.LOGGER.error("Error occurred while loading resource arbitrary structure nbt {}", id.toString(), e);
            }
        }
    }
    public ArbitraryStructure get(Identifier id) {
        return this.arbitraryStructures.get(id);
    }

}
