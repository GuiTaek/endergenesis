package com.gmail.guitaekm.endergenesis.event;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class DarknessApplierServer implements ServerTickEvents.EndTick {
    @Override
    public void onEndTick(MinecraftServer server) {
        server.getPlayerManager().getPlayerList().stream()
                .filter(
                        (PlayerEntity player)->player
                                        .getWorld()
                                        .getBiome(player.getBlockPos())
                                        .isIn(TagKey.of(Registry.BIOME_KEY, new Identifier(EnderGenesis.MOD_ID, "fog_biomes")))
                ).filter(player -> !player.isSpectator())
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
