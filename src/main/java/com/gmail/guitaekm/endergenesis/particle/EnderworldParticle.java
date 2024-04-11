/*
from the YouTube video https://www.youtube.com/watch?v=cGEbensmW_c
 */

package com.gmail.guitaekm.endergenesis.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(value=EnvType.CLIENT)
public class EnderworldParticle
        extends AscendingParticle {
    protected SpriteProvider spriteProvider;
    protected static int NR_SPRITES = 2;
    protected EnderworldParticle(
            ClientWorld world,
            double x,
            double y,
            double z,
            float scaleMultiplier,
            SpriteProvider spriteProvider
    ) {
        super(
                world,
                x,
                y,
                z,
                0f,
                0f,
                0f,
                0,
                0,
                0,
                scaleMultiplier,
                spriteProvider,
                1f,
                1,
                0.0f,
                false
        );
        this.spriteProvider = spriteProvider;
        this.setMaxAge(600);
        this.colorRed = 1F;
        this.colorGreen = 1F;
        this.colorBlue = 1F;
    }
    public void setSpriteForAge(SpriteProvider spriteProvider) {
        if (!this.dead) {
            // I have no idea why the maxAge that setSpriteForAge uses the same is
            // as when Particle dies. This makes no sense
            // to calculate the index of the next sprite, spriteProvider.getSprite will
            // approximately calculate i/j * nr_sprites
            // beware of out of bounds errors, as the getSprite won't check them
            this.setSprite(spriteProvider.getSprite(this.age, this.maxAge / EnderworldParticle.NR_SPRITES + 1));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Factory
            implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(
                DefaultParticleType defaultParticleType,
                ClientWorld clientWorld,
                double x,
                double y,
                double z,
                double g,
                double h,
                double i
        ) {
            return new EnderworldParticle(
                    clientWorld,
                    x, y, z,
                    0.3f,
                    this.spriteProvider
            );
        }
    }
}
