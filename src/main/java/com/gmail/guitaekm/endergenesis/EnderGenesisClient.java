package com.gmail.guitaekm.endergenesis;

import com.gmail.guitaekm.endergenesis.blocks.ModBlocksClient;
import com.gmail.guitaekm.endergenesis.event.ModEventsClient;
import com.gmail.guitaekm.endergenesis.gui.RegisterGui;
import com.gmail.guitaekm.endergenesis.keybinds.ModKeybindsClient;
import com.gmail.guitaekm.endergenesis.keybinds.RegisterKeyBinds;
import com.gmail.guitaekm.endergenesis.networking.ModNetworking;
import com.gmail.guitaekm.endergenesis.particle.ModParticles;
import com.gmail.guitaekm.endergenesis.particle.custom.EnderworldParticle;
import com.gmail.guitaekm.endergenesis.resources.ModResourcesClient;
import com.gmail.guitaekm.endergenesis.teleport.RegisterUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public class EnderGenesisClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModBlocksClient.register();
        ParticleFactoryRegistry.getInstance().register(ModParticles.ENDERWORLD_PARTICLE, EnderworldParticle.Factory::new);
        RegisterKeyBinds.registerKeyBinds();
        ModNetworking.registerNetworkingClient();
        ModParticles.registerParticles();
        ModEventsClient.registerEvents();
        ModResourcesClient.register();
        ModKeybindsClient.register();
        RegisterUtils.registerClient();
        RegisterGui.registerClient();
    }
}
