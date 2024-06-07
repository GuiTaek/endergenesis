package com.gmail.guitaekm.endergenesis;

import com.gmail.guitaekm.endergenesis.blocks.ModBlocks;
import com.gmail.guitaekm.endergenesis.enderling_structure.EnderlingStructureInitializer;
import com.gmail.guitaekm.endergenesis.gui.RegisterGui;
import com.gmail.guitaekm.endergenesis.items.ModItems;
import com.gmail.guitaekm.endergenesis.point_of_interest.ModPointsOfInterest;
import com.gmail.guitaekm.endergenesis.event.ModEventsServer;
import com.gmail.guitaekm.endergenesis.networking.ModNetworking;
import com.gmail.guitaekm.endergenesis.teleport.RegisterUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;

public class EnderGenesisServer implements DedicatedServerModInitializer, ClientModInitializer {
	@Override
	public void onInitializeServer() {
		ModBlocks.register();
		ModItems.register();
		ModEventsServer.registerEvents();
		ModNetworking.registerNetworkingServer();
		ModPointsOfInterest.registerClass();
		EnderlingStructureInitializer.register();
		RegisterGui.registerServer();
		RegisterUtils.registerServer();
	}

	@Override
	public void onInitializeClient() {
		this.onInitializeServer();
	}
}