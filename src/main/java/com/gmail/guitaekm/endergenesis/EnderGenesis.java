package com.gmail.guitaekm.endergenesis;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderGenesis {
    public static String MOD_ID = "endergenesis";
    public static final Logger LOGGER = LoggerFactory.getLogger(EnderGenesis.MOD_ID);
    public void onInitialize() {
        //
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            // here, we are at a physical client, that is different to a logical client
            // see https://fabricmc.net/wiki/tutorial:side
            new EnderGenesisServer().onInitializeServer();
            new EnderGenesisClient().onInitializeClient();
        } else {
            new EnderGenesisServer().onInitializeServer();
        }
    }
}
