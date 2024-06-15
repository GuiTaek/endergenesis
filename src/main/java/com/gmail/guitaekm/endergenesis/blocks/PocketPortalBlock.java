package com.gmail.guitaekm.endergenesis.blocks;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import com.gmail.guitaekm.endergenesis.access.IServerPlayerPocketPortalAccess;
import com.gmail.guitaekm.endergenesis.event.AllPlayerNbt;
import com.gmail.guitaekm.endergenesis.networking.HandleLongUseServer;
import com.gmail.guitaekm.endergenesis.teleport.TeleportParams;
import com.gmail.guitaekm.endergenesis.teleport.VehicleTeleport;
import com.gmail.guitaekm.endergenesis.worldgen.ModWorlds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;

import java.util.*;
import java.util.stream.Collectors;

public class PocketPortalBlock extends Block implements HandleLongUseServer.Listener {
    // these will be configured through arbitrary structures -- it's just not implemented yet
    // configure
    public static Set<Vec3i> MANTLE_OFFSETS = Set.of(
            new Vec3i(0, 1, 0),
            new Vec3i(0, 2, 0),
            new Vec3i(1, 3, 0),
            new Vec3i(0, 3, 1),
            new Vec3i(0, 3, -1),
            new Vec3i(-1, 3, 0),
            new Vec3i(0, 4, 0)
    );

    // configure
    public static Vec3i CORE_OFFSET = new Vec3i(0, 3, 0);

    // configure
    public static List<Set<Block>> WARD_BLOCK = List.of(
            Set.of(
                    Blocks.END_STONE_BRICKS
            ),
            Set.of(
                    Blocks.IRON_BLOCK
            ),
            Set.of(
                    Blocks.DIAMOND_BLOCK,
                    Blocks.AMETHYST_BLOCK
            ),
            Set.of(
                    Blocks.NETHERITE_BLOCK,
                    Blocks.BEACON
            )
    );

    // configure
    public static List<Vec3i> WARD_POWERS = List.of(
            new Vec3i(4, 2, 4),
            new Vec3i(8, 4, 8),
            new Vec3i(16, 8, 16)
    );
    // configure
    // this number gives more space than the current server with the most players
    public static int POCKET_DIMENSION_SPREAD = 100_000;
    // configure
    public static int POCKET_DIMENSION_RADIUS = 20;
    // configure
    public static double DROP_CHANCE = 0.8;
    public PocketPortalBlock(Settings settings) {
        super(settings);
    }
    public static void register(PocketPortalBlock block) {
        HandleLongUseServer.register(block);
    }

    public static ChunkPos findFreePocketDimensionPlace(MinecraftServer server) {
        Set<ChunkPos> occupiedPos = AllPlayerNbt.getPlayerNbts(server)
                .values()
                .stream()
                .map(
                        nbt -> nbt.getCompound("pocketDimensionPlace")
                ).map(
                        nbt -> new ChunkPos(
                                nbt.getInt("x"),
                                nbt.getInt("z")
                        )
                ).collect(Collectors.toSet());
        // it's probably fun to randomize this
        long seed = server.getOverworld().getSeed();
        Random rand = new Random(seed + occupiedPos.size());
        int NR_CHUNKS_WIDTH = 2 * (int)Math.ceil((double)POCKET_DIMENSION_RADIUS / 16d);
        int CHUNK_SPREAD = (int)((double)POCKET_DIMENSION_SPREAD / 16 / NR_CHUNKS_WIDTH);
        for (int radius = 0; radius < CHUNK_SPREAD; radius++) {
            // I use taxi geometry
            List<ChunkPos> possiblePlaces = new ArrayList<>();
            for (int x = -radius; x <= radius; x++) {
                int z = radius - Math.abs(x);
                possiblePlaces.add(new ChunkPos(NR_CHUNKS_WIDTH * x, NR_CHUNKS_WIDTH * z));
                if (z != 0) {
                    possiblePlaces.add(new ChunkPos(NR_CHUNKS_WIDTH * x, NR_CHUNKS_WIDTH * (-z)));
                }
            }
            Collections.shuffle(possiblePlaces, rand);
            Optional<ChunkPos> possibleResult = possiblePlaces
                    .stream()
                    .filter(chunkPos -> !occupiedPos.contains(chunkPos))
                    .findFirst();
            if (possibleResult.isPresent()) {
                return possibleResult.get();
            }
        }
        // there is nothing I can do in this case than just increasing the size of the field
        return new ChunkPos(0, 0);
    }
    public static ChunkPos getOrCreatePocketPortalPos(ServerPlayerEntity player) {
        ChunkPos pos = ((IServerPlayerPocketPortalAccess) player).endergenesis$getPocketDimensionPlace();
        if (pos != null) {
            return pos;
        }
        ChunkPos pocketPlace = PocketPortalBlock.findFreePocketDimensionPlace(Objects.requireNonNull(player.getServer()));
        ((IServerPlayerPocketPortalAccess) player).endergenesis$setPocketDimensionPlace(pocketPlace);
        return pocketPlace;
    }
    @Override
    public void onUse(MinecraftServer server, ServerPlayerEntity player, BlockPos pos) {
        ModWorlds.LazyInformation info = ModWorlds.getInfo(server);
        if (!player.getWorld().getBlockState(pos).getBlock().equals(this)) {
            return;
        }
        Optional<Structure> pocketPortalOptional = server
                .getStructureManager()
                .getStructure(new Identifier(EnderGenesis.MOD_ID, "pocket_portal"));
        assert pocketPortalOptional.isPresent();
        Structure pocketPortal = pocketPortalOptional.get();
        // from SpawnHelper
        ConfiguredStructureFeature<?, ?> rarePocketPortal = server
                .getOverworld()
                .getStructureAccessor()
                .method_41036()
                .get(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY)
                .get(new Identifier("endergenesis:rare_pocket_portal"));
        ConfiguredStructureFeature<?, ?> commonPocketPortal = server.getOverworld().getStructureAccessor().method_41036().get(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY).get(new Identifier("endergenesis:common_pocket_portal"));
        if (player.getWorld().getRegistryKey().equals(info.pocketDimensionKey())) {
            BlockPos targetPos = ((IServerPlayerPocketPortalAccess) player).endergenesis$getLastUsedPocketPortal();
            if (targetPos == null) {
                EnderGenesis.LOGGER.warn("Tried to leave the pocket dimension two times in the row. The player likely used another teleportation method");
                VehicleTeleport.teleportToEnderworldSpawn(server, info.enderworld(), player);
                return;
            }
            ((IServerPlayerPocketPortalAccess) player).endergenesis$setLastUsedPocketPortal(null);
            if (!info.enderworld().getStructureAccessor().getStructureAt(targetPos, rarePocketPortal).hasChildren()) {
                if (!info.enderworld().getStructureAccessor().getStructureAt(targetPos, commonPocketPortal).hasChildren()) {
                    EnderGenesis.LOGGER.warn("corrupted player data");
                    VehicleTeleport.teleportToEnderworldSpawn(server, info.enderworld(), player);
                    return;
                }
            }
            PocketPortalBlock.pocketPortalTeleport(info.enderworld(), player, pocketPortal, targetPos);

        } else if (player.getWorld().getRegistryKey().equals(info.enderworldKey())) {
            ((IServerPlayerPocketPortalAccess) player).endergenesis$setLastUsedPocketPortal(pos.up());
            // history of ideas for calculating the pocket dimension position:
            // the position should be different for each player and depend on the seed and the hash value of the player name
            // actually, the idea/comment explained directly above isn't good because of the birthday paradox.
            // It should be saved in the world and a free space should be chosen
            BlockPos pocketPos = PocketPortalBlock.getOrCreatePocketPortalPos(player).getBlockPos(7, 0, 7);
            this.preparePocketDimension(info.pocketDimension(), pocketPos);
            PocketPortalBlock.pocketPortalTeleport(info.pocketDimension(), player, pocketPortal, pocketPos.withY(5));
        } else {
            EnderGenesis.LOGGER.warn("Player tried to walk through a pocket portal outside of enderworld and pocket dimension. Should be impossible in survival.");
        }
    }

    /**
     *
     * @param destination the destination dimension
     * @param position the bottom left corner
     * @param size the size of the pocket portal
     * @param ignoreLayers how many layers to be ignored from the bottom
     */
    public static void dropByPortalPlacement(ServerWorld destination, BlockPos position, Vec3i size, int ignoreLayers, ServerPlayerEntity player) {
        // configure
        Vec3i pocketPortalOffset = new Vec3i(-3, -5, -3);
        for (int x = 0; x < size.getX(); x++) {
            for (int y = ignoreLayers; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    Vec3i toPortalBlockOffset = new Vec3i(x, y, z).add(pocketPortalOffset).add(0, 1, 0);
                    if (MANTLE_OFFSETS.contains(toPortalBlockOffset) || CORE_OFFSET.equals(toPortalBlockOffset)) {
                        continue;
                    }
                    BlockPos toDestroy = position.add(x, y, z);
                    // somehow this needs a chunk, not the server world
                    BlockEntity blockEntity = destination.getChunk(toDestroy).getBlockEntity(toDestroy);
                    if (blockEntity instanceof Inventory inventory) {
                        for(int i = 0; i < inventory.size(); ++i) {
                            if (decideDrop(destination)) {
                                ItemScatterer.spawn(
                                        destination,
                                        toDestroy.getX(),
                                        toDestroy.getY(),
                                        toDestroy.getZ(),
                                        inventory.getStack(i)
                                );
                            }
                        }
                        Block toUpdate = destination.getBlockState(toDestroy).getBlock();
                        destination.updateComparators(toDestroy, toUpdate);
                    }
                    destination.breakBlock(toDestroy, decideDrop(destination), player);
                }
            }
        }
    }

    /**
     * asserts the portal position to be valid
     * @param destination the world to teleport to
     * @param portalPosition position of the portal block
     */
    public static void pocketPortalTeleport(ServerWorld destination, ServerPlayerEntity player, Structure pocketPortal, BlockPos portalPosition) {
        // configure
        Vec3i pocketPortalOffset = new Vec3i(-3, -5, -3);

        // configure
        Vec3i pocketPortalSize = new Vec3i(7, 10, 7);
        dropByPortalPlacement(destination, portalPosition.add(pocketPortalOffset), pocketPortalSize, 1, player);

        pocketPortal.place(
                destination,
                portalPosition.add(pocketPortalOffset),
                // the pivot parameter isn't used a single time, I looked through every implementation
                // and there is no documentation for that one
                portalPosition,
                new StructurePlacementData(),
                destination.getRandom(),
                Block.NOTIFY_ALL
        );
        // configure
        int yOff = -4;
        VehicleTeleport.teleportWithVehicle(new TeleportParams(
                player, destination, portalPosition,
                portalPosition.getX() + 0.5,
                portalPosition.getY() + yOff,
                portalPosition.getZ() + 0.5
        ));
    }
    public static boolean decideDrop(ServerWorld destination) {
        return destination.getRandom().nextDouble() <= PocketPortalBlock.DROP_CHANCE;
    }
    public void preparePocketDimension(ServerWorld pocketDimension, BlockPos pos) {
        // configure
        Vec3i portalSize = new Vec3i(7, 10, 7);
        Vec3i portalRadiusses = new Vec3i(
                Math.floorDiv(portalSize.getX() - 1, 2),
                portalSize.getY(),
                Math.floorDiv(portalSize.getZ() - 1, 2)
        );
        Vec3i radius = getPocketDimensionDimensions(pocketDimension, pos.add(0, 4, 0));
        Vec3i biggestDim = WARD_POWERS.get(WARD_POWERS.size() - 1);
        int xRadiusBedrock = biggestDim.getX() + 1;
        int yRadiusBedrock = Math.max(portalSize.getY(), biggestDim.getY()) + 1;
        int zRadiusBedrock = biggestDim.getZ() + 1;

        // configure
        for (int x = -xRadiusBedrock; x <= xRadiusBedrock; x++) {
            for (int z = -zRadiusBedrock; z <= zRadiusBedrock; z++) {
                {
                    int y = 0;
                    pocketDimension.setBlockState(pos.add(x, y, z), Blocks.BEDROCK.getDefaultState());
                }
                for (int y = 1; y <= yRadiusBedrock; y++) {
                    if (-radius.getX() <= x && x <= radius.getX()
                            && -radius.getZ() <= z && z <= radius.getZ()
                            && y <= radius.getY()) {
                        continue;
                    }
                    if (-portalRadiusses.getX() <= x && x <= portalRadiusses.getX()
                        && -portalRadiusses.getZ() <= z && z <= portalRadiusses.getZ()
                            && y <= portalRadiusses.getY()
                    ) {
                        if (pocketDimension.getBlockState(pos.add(x, y, z)).getBlock().equals(Blocks.BARRIER)) {
                            // here, the pocket dimension is entered the first time
                            pocketDimension.setBlockState(pos.add(x, y, z), Blocks.AIR.getDefaultState());
                        }
                        continue;
                    }
                    if (
                            x >= xRadiusBedrock
                            || x <= -xRadiusBedrock
                            || y >= yRadiusBedrock
                            || z >= zRadiusBedrock
                            || z <= -zRadiusBedrock
                    ) {
                        // bottleneck
                        pocketDimension.setBlockState(pos.add(x, y, z), Blocks.BEDROCK.getDefaultState());
                        continue;
                    }
                    if (List.of(Blocks.CHEST, Blocks.TRAPPED_CHEST)
                            .contains(pocketDimension.getBlockState(pos.add(x, y, z)).getBlock())
                    ) {
                        // this shall help making changes in the pocket dimension upgrade not as scary
                        continue;
                    }
                    pocketDimension.setBlockState(pos.add(x, y, z), Blocks.BARRIER.getDefaultState());
                }
            }
        }
        for (int x = -radius.getX(); x <= radius.getX(); x++) {
            for (int z = -radius.getZ(); z <= radius.getZ(); z++) {
                for (int y = 1; y <= radius.getY(); y++) {
                    if (pocketDimension.getBlockState(pos.add(x, y, z)).getBlock().equals(Blocks.BARRIER)) {
                        pocketDimension.setBlockState(pos.add(x, y, z), Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    /**
     * temp method to check if the pocket portal is argumented
     * @param portalPos the position of the pocket portal block
     * @return the dimensions the pocket portal should have, but only the radiusses
     */
    public Vec3i getPocketDimensionDimensions(ServerWorld pocketDimension, BlockPos portalPos) {
        Vec3i firstOffset = MANTLE_OFFSETS.iterator().next();
        Block mantleBlock = pocketDimension
                .getBlockState(portalPos.add(firstOffset.getX(), firstOffset.getY(), firstOffset.getZ()))
                .getBlock();
        List<Vec3i> notFittingBlocks = MANTLE_OFFSETS.stream().filter(offset -> (
                pocketDimension
                        .getBlockState(portalPos.add(offset.getX(), offset.getY(), offset.getZ()))
                        .getBlock()
                        != mantleBlock
        )).toList();
        if (!notFittingBlocks.isEmpty()) {
            return new Vec3i(0, 0, 0);
        }
        Block coreBlock = pocketDimension.getBlockState(portalPos.add(CORE_OFFSET.getX(), CORE_OFFSET.getY(), CORE_OFFSET.getZ())).getBlock();
        for (int i = 0; i < WARD_POWERS.size(); i++) {
            if (WARD_BLOCK.get(i).contains(mantleBlock)) {
                if (WARD_BLOCK.get(i + 1).contains(coreBlock)) {
                    return WARD_POWERS.get(i);
                }
            }
        }
        return new Vec3i(0, 0, 0);
    }
    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
