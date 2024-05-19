package com.gmail.guitaekm.endergenesis.particle;

import com.gmail.guitaekm.endergenesis.keybinds.use_block_long.SendPacketToServer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class LongUseParticle extends SpriteBillboardParticle {
    public static int NR_SPRITES = 8;
    public static int SPRITE_DELAY = 4;
    public static float HORIZONTAL_VELOCITY_SCALE = 0.1f;
    public static float VERTICAL_VELOCITY_SCALE = 0.1f;
    public static int PARTICLES_PER_TICK = 4;
    public static float PARTICLE_SCALE = 0.3f;
    private final SpriteProvider spriteSet;
    protected LongUseParticle(
            ClientWorld world,
            double x,
            double y,
            double z,
            SpriteProvider spriteSet,
            double vx,
            double vy,
            double vz
    ) {
        super(world, x, y, z, vx, vy, vz);
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
        this.setMaxAge(40);
        this.scale *= LongUseParticle.PARTICLE_SCALE;
        this.spriteSet = spriteSet;
        this.setSpriteForAge(spriteSet);
    }
    public static float getProbabilityOfParticle(float x) {
        return 0.3f + 0.7f * x * x;
    }

    public static void spawnUsageParticle(World world, BlockPos pos, int age) {
        for (int i = 0; i < LongUseParticle.PARTICLES_PER_TICK; i++) {
            float relAge = (float) age / SendPacketToServer.MAX_AGE;
            if (world.getRandom().nextFloat() > LongUseParticle.getProbabilityOfParticle(relAge)) {
                return;
            }
            float vx = HORIZONTAL_VELOCITY_SCALE * (2 * world.getRandom().nextFloat() - 1);
            float vy = VERTICAL_VELOCITY_SCALE * world.getRandom().nextFloat();
            float vz = HORIZONTAL_VELOCITY_SCALE * (2 * world.getRandom().nextFloat() - 1);
            float scale;
            // project to the cube
            float checkVal = Math.max(Math.abs(vx), Math.max(Math.abs(vy), Math.abs(vz)));
            if (checkVal == Math.abs(vx)) {
                scale = 1 / Math.abs(vx);
            } else if (checkVal == Math.abs(vz)) {
                scale = 1 / Math.abs(vz);
            } else {
                scale = 1 / Math.abs(vy);
            }
            float volXZ = 0.5f;
            float volY = 1f;
            float dx = scale * vx * volXZ;
            float dy = scale * vy * volY;
            float dz = scale * vz * volXZ;
            world.addParticle(
                    ModParticles.LONG_USE_PARTICLE,
                    pos.getX() + 0.5 + dx,
                    pos.getY() + dy,
                    pos.getZ() + 0.5 + dz,
                    vx,
                    vy,
                    vz
            );
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.fadeOut();
        this.setSpriteForAge(this.spriteSet);
    }

    private void fadeOut() {
        float deltaAge = this.maxAge - this.age;
        this.colorAlpha = deltaAge * deltaAge / this.maxAge / this.maxAge;
    }

    @Override
    public void setSpriteForAge(SpriteProvider spriteProvider) {
        if (!this.dead) {
            // I have no idea why the maxAge that setSpriteForAge uses the same is
            // as when Particle dies. This makes no sense
            // to calculate the index of the next sprite, spriteProvider.getSprite will
            // approximately calculate i/j * nr_sprites
            // beware of out of bounds errors, as the getSprite won't check them
            this.setSprite(
                    spriteProvider.getSprite(
                            (this.age / LongUseParticle.SPRITE_DELAY) % LongUseParticle.NR_SPRITES,
                            LongUseParticle.NR_SPRITES
                    )
            );
        }
    }
    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteSet;
        public Factory(SpriteProvider spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new LongUseParticle(world, x, y, z, this.spriteSet, velocityX, velocityY, velocityZ);
        }
    }
}
