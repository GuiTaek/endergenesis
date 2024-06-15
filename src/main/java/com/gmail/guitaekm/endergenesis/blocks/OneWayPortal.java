package com.gmail.guitaekm.endergenesis.blocks;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import com.gmail.guitaekm.endergenesis.networking.HandleLongUseServer;
import com.gmail.guitaekm.endergenesis.teleport.TeleportParams;
import com.gmail.guitaekm.endergenesis.teleport.VehicleTeleport;
import com.gmail.guitaekm.endergenesis.worldgen.ModWorlds;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class OneWayPortal extends BlockWithEntity implements HandleLongUseServer.Listener {
    public OneWayPortal(Settings settings) {
        super(settings);
    }

    public void registerServer() {
        HandleLongUseServer.register(this);
    }
    public void registerClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(this, RenderLayer.getTranslucent());
    }

    @Override
    public void onLongUse(MinecraftServer server, ServerPlayerEntity player, BlockPos pos) {
        if (!this.getStateManager().getStates().contains(player.getWorld().getBlockState(pos))) {
            return;
        }
        if (!player.getWorld().getRegistryKey().equals(ModWorlds.getInfo(server).enderworldKey())) {
            EnderGenesis.LOGGER.warn("The one way portal was used outside the enderworld. Should be impossible in survival.");
            return;
        }
        BlockPos overworldPos = EnderworldPortalBlock.enderworldToOverworld(server, pos);
        int targetHeight = server
                .getOverworld()
                .getChunk(overworldPos)
                .getHeightmap(Heightmap.Type.WORLD_SURFACE)
                .get(overworldPos.getX() & 15, overworldPos.getZ() & 15);
        VehicleTeleport.teleportWithVehicle(new TeleportParams(
                player,
                server.getOverworld(),
                overworldPos.withY(targetHeight - 1),
                overworldPos.getX() + 0.5,
                targetHeight,
                overworldPos.getZ() + 0.5
        ));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        if (this.getStateManager().getStates().contains(stateFrom)) {
            return true;
        }
        return super.isSideInvisible(state, stateFrom, direction);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new OneWayPortalEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlocks.ONE_WAY_PORTAL_ENTITY, OneWayPortalEntity::tick);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // With inheriting from BlockWithEntity this defaults to INVISIBLE, so we need to change that!
        return BlockRenderType.MODEL;
    }
    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}