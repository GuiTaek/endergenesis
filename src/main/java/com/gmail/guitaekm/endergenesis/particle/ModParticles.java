package com.gmail.guitaekm.endergenesis.particle;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModParticles {
    public static final DefaultParticleType ENDERWORLD_PARTICLE = FabricParticleTypes.simple();
    public static final DefaultParticleType LONG_USE_PARTICLE = FabricParticleTypes.simple();

    public static void registerParticles() {
        Registry.register(
                Registry.PARTICLE_TYPE,
                new Identifier(EnderGenesis.MOD_ID, "enderworld"),
                ENDERWORLD_PARTICLE
        );
        Registry.register(
                Registry.PARTICLE_TYPE,
                new Identifier(EnderGenesis.MOD_ID, "long_use"),
                LONG_USE_PARTICLE
        );
    }
}
