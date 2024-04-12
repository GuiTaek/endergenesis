package com.gmail.guitaekm.endergenesis.event;

import com.gmail.guitaekm.endergenesis.resources.FogBiomes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.List;
import java.util.function.Supplier;

public class DarknessApplierServer implements ServerTickEvents.EndTick {
    @Override
    public void onEndTick(MinecraftServer server) {
        server.getPlayerManager().getPlayerList().stream()
                .filter(
                        (PlayerEntity player)->FogBiomes.isFogBiome(
                                player
                                        .getWorld()
                                        .getBiomeKey(player.getBlockPos())
                                        .orElse(BiomeKeys.PLAINS)
                                        .getValue()
                                        .toString()
                        )
                )
                .filter(player -> !player.isSpectator())
                .filter(player -> !player.isCreative())
                .forEach(
                        (PlayerEntity player)->{
                            player.addStatusEffect(
                                    new StatusEffectInstance(
                                            StatusEffects.BLINDNESS,
                                            40,
                                            0,
                                            true,
                                            false
                                    )
                            );
                        }
                );
    }
}
