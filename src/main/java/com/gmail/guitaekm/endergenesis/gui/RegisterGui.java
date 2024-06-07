package com.gmail.guitaekm.endergenesis.gui;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class RegisterGui {
    public static final ExtendedScreenHandlerType<TeleportScreenHandler> TELEPORT_SCREEN_HANDLER_TYPE = new ExtendedScreenHandlerType<>(
            TeleportScreenHandler::new
    );
    public static final ScreenHandlerType<RenamingScreenHandler> RENAMING_SCREEN_SCREEN_HANDLER_TYPE = new ExtendedScreenHandlerType<>(
            RenamingScreenHandler::new
    );
    public static void registerServer() {
        Registry.register(
                Registry.SCREEN_HANDLER,
                new Identifier(EnderGenesis.MOD_ID, "teleport_screen"),
                TELEPORT_SCREEN_HANDLER_TYPE
        );
        Registry.register(
                Registry.SCREEN_HANDLER,
                new Identifier(EnderGenesis.MOD_ID, "renaming_screen"),
                RENAMING_SCREEN_SCREEN_HANDLER_TYPE
        );
    }
    public static void registerClient() {
        NetherTeleportHandler.registerClient();
    }
}
