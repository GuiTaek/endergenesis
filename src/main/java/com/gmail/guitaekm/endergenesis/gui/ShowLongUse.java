package com.gmail.guitaekm.endergenesis.gui;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import com.gmail.guitaekm.endergenesis.blocks.ModBlocks;
import com.gmail.guitaekm.endergenesis.enderling_structure.*;
import com.gmail.guitaekm.endergenesis.keybinds.use_block_long.CallbackClient;
import com.gmail.guitaekm.endergenesis.keybinds.use_block_long.SendPacketToServer;
import com.gmail.guitaekm.endergenesis.keybinds.use_block_long.UseBlockLong;
import com.gmail.guitaekm.endergenesis.particle.LongUseParticle;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.List;

public class ShowLongUse implements CallbackClient {
    public static String LONG_USE_NAME = "long_use_holding";
    public SoundEvent LONG_USE_SOUND;
    public static PositionedSoundInstance soundInProgress;
    public static boolean isValidPos;
    public ShowLongUse() {
        UseBlockLong.registerListener(SendPacketToServer.MAX_AGE, this);
        Identifier id = new Identifier(EnderGenesis.MOD_ID, ShowLongUse.LONG_USE_NAME);
        LONG_USE_SOUND = Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));
    }

    public static boolean checkValidLongUse(World world, PlayerEntity player, BlockPos pos) {
        Identifier id = EnderlingStructureInitializer.enderlingStructureRegistry.findEnderlingStructure(player, pos);
        BlockState toCheck = world.getBlockState(pos);
        List<Block> blocksToCheck = List.of(
//              no, not the first block, as it is meant to "not function"
//              ModBlocks.ENDERWORLD_PORTAL_BLOCK_1,
                ModBlocks.ENDERWORLD_PORTAL_BLOCK_2,
                ModBlocks.ENDERWORLD_PORTAL_BLOCK_3,
                ModBlocks.POCKET_PORTAL_BLOCK,
                ModBlocks.ONE_WAY_PORTAL_BLOCK
        );
        return id != null
                || blocksToCheck
                .stream()
                .anyMatch(
                        block -> block
                                .getStateManager()
                                .getStates()
                                .contains(toCheck));
    }
    @Override
    public void onStartUse(MinecraftClient client, World world, PlayerEntity player, BlockPos pos) {
        ShowLongUse.isValidPos = ShowLongUse.checkValidLongUse(world, player, pos);
        if (ShowLongUse.isValidPos) {
            ShowLongUse.soundInProgress = new PositionedSoundInstance(this.LONG_USE_SOUND, SoundCategory.AMBIENT, 1f, 1f, pos);
            client.getSoundManager().play(ShowLongUse.soundInProgress);
        }
    }

    @Override
    public void onUseTick(MinecraftClient client, World world, PlayerEntity player, BlockPos pos, int age) {
        if (!ShowLongUse.isValidPos) {
            return;
        }
        LongUseParticle.spawnUsageParticle(world, pos, age);
    }

    @Override
    public void onEndUse(MinecraftClient client, World world, PlayerEntity player, BlockPos pos, int age) {
        if (!ShowLongUse.isValidPos) {
            return;
        }
        client.getSoundManager().stop(ShowLongUse.soundInProgress);
    }
}
