package com.gmail.guitaekm.technoenderling.blocks;

import com.gmail.guitaekm.technoenderling.TechnoEnderling;
import com.gmail.guitaekm.technoenderling.access.IServerPlayerNetherEnderworldPortal;
import com.gmail.guitaekm.technoenderling.enderling_structure.EnderlingStructure;
import com.gmail.guitaekm.technoenderling.enderling_structure.EnderlingStructureInitializer;
import com.gmail.guitaekm.technoenderling.enderling_structure.LinkGeneratedEnderworldPortal;
import com.gmail.guitaekm.technoenderling.gui.TeleportScreenFactory;
import com.gmail.guitaekm.technoenderling.networking.HandleLongUseServer;
import com.gmail.guitaekm.technoenderling.point_of_interest.ModPointsOfInterest;
import com.gmail.guitaekm.technoenderling.utils.ArbitraryStructure;
import com.gmail.guitaekm.technoenderling.utils.TeleportParams;
import com.gmail.guitaekm.technoenderling.utils.VehicleTeleport;
import com.gmail.guitaekm.technoenderling.worldgen.ModWorlds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class EnderworldPortalBlock extends Block implements HandleLongUseServer.Listener {
    // configure
    public static final int[][] RESPAWN_LAYERS = {
            {1, 1, 0},
            {1, 1, 1},
            {1, 2, 0},
            {1, 0, 0},
            {1, 2, 1},
            {1, 0, 1},
            {0, 3, 0}
    };

    // configure
    public static final int[][] VEHICLE_RESPAWN_LAYERS = {
            { 2, 1, 0},
            { 2, 1, 1},
            { 2,  2,  0},
            { 2,  2,  1},
            { 2,  0,  0},
            { 2,  0,  1},
            { 0,  3,  0}
    };
    final public int index;
    final public boolean active;
    final public Block unactiveCounterpart;
    public static final BooleanProperty GENERATED = BooleanProperty.of("generated");

    final static public List<Identifier> allowedDimensions = List.of(
            new Identifier(TechnoEnderling.MOD_ID, "enderworld"),
            new Identifier("minecraft:overworld"),
            new Identifier("minecraft:the_nether")
    );
    public static class LazyInformation {
        public ServerWorld enderworld;
        public int dimensionScaleInverse;
        public EnderlingStructure portal;
        public ArbitraryStructure portalGenerated;
        protected LazyInformation(ServerWorld enderworld, int dimensionScaleInverse, EnderlingStructure portal, ArbitraryStructure portalGenerated) {
            this.enderworld = enderworld;
            this.dimensionScaleInverse = dimensionScaleInverse;
            this.portal = portal;
            this.portalGenerated = portalGenerated;
        }
    }
    protected static @Nullable LazyInformation info;

    public static LazyInformation getInfo(MinecraftServer server) {
        if (EnderworldPortalBlock.info != null) {
            return info;
        }
        ServerWorld enderworld = ModWorlds.getInfo(server).enderworld();
        assert enderworld != null;
        assert enderworld.getDimension() != null;
        int dimensionScaleInverse = (int) Math.round(1D / enderworld.getDimension().getCoordinateScale());

        // the reason this is here is, because I want, that the EnderlingStructureRegistry is ready
        EnderlingStructure portal = EnderlingStructureInitializer
                .enderlingStructureRegistry
                .get(new Identifier(TechnoEnderling.MOD_ID, "enderworld_portal"));
        ArbitraryStructure portalGenerated = EnderlingStructureInitializer
                .arbitraryStructureRegistry
                .get(new Identifier(TechnoEnderling.MOD_ID, "enderworld_portal_generated"));
        assert portal != null;
        EnderworldPortalBlock.info = new LazyInformation(enderworld, dimensionScaleInverse, portal, portalGenerated);
        return EnderworldPortalBlock.info;
    }

    public EnderworldPortalBlock(Settings settings, int index, boolean active, Block unactiveCounterpart) {
        super(settings);
        this.index = index;
        this.active = active;
        this.unactiveCounterpart = unactiveCounterpart;
        HandleLongUseServer.register(this);
        setDefaultState(getDefaultState().with(GENERATED, false));
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> EnderworldPortalBlock.info = null);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(GENERATED);
    }
    @Override
    public void onUse(MinecraftServer server, ServerPlayerEntity player, BlockPos pos) {
        LazyInformation info = EnderworldPortalBlock.getInfo(server);
        if(player.getWorld().getBlockState(pos).getBlock() != this) {
            return;
        }
        if(!((EnderworldPortalBlock)player.getWorld().getBlockState(pos).getBlock()).active) {
            return;
        }
        if (!EnderworldPortalBlock.allowedDimensions.contains(player.getWorld().getRegistryKey().getValue())) {
            return;
        }
        if (player.getWorld().getRegistryKey().getValue().equals(
            new Identifier("minecraft:the_nether")
        )) {
            Optional<BlockPos> portalPosOptional = info.portal.placeable().check(player.getWorld(), pos);
            if (portalPosOptional.isEmpty()) {
                TechnoEnderling.LOGGER.warn("Invalid portal block found. Invalidating it");
                player.getWorld().setBlockState(pos, this.unactiveCounterpart.getDefaultState());
                return;
            }
            // configure
            BlockPos toAddToDestinations = portalPosOptional.get().add(0, 2, 0);
            EnderworldPortalBlock.NetherInstance source = ((IServerPlayerNetherEnderworldPortal)player)
                    .techno_enderling$addIfNotPresent(toAddToDestinations);
            ((IServerPlayerNetherEnderworldPortal)player).techno_enderling$setSource(source);
            player.openHandledScreen(new TeleportScreenFactory());
            // it's probably better to remove the source after the screen
            ((IServerPlayerNetherEnderworldPortal)player).techno_enderling$setSource(null);
            return;
        }
        if(player.getWorld().getBlockState(pos).get(GENERATED)) {
            LinkGeneratedEnderworldPortal.generateOtherPortal(server, info.portal.placeable(), pos);
            Optional<BlockPos> portalPosOptional = info.portalGenerated.check(player.getWorld(), pos);
            if (portalPosOptional.isEmpty()) {
                TechnoEnderling.LOGGER.warn("Invalid portal block found. Invalidating it");
                player.getWorld().setBlockState(pos, this.unactiveCounterpart.getDefaultState());
                return;
            }
            BlockPos portalPos = portalPosOptional.get();
            info.portal.placeable().place(player.getWorld(), portalPos, new Vec3i(0, 0, 0), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
        if (player.getWorld().getRegistryKey().getValue().toString().equals("technoenderling:enderworld")) {
            ServerWorld destination = server.getOverworld();
            List<BlockPos> portalStream = EnderworldPortalBlock.findPortalPosToOverworld(server, pos).toList();
            TeleportParams params = EnderworldPortalBlock.getTeleportParamsWithTargetPortalPositions(server, destination, player, portalStream, pos);
            if (params == null) {
                return;
            }
            VehicleTeleport.teleportWithVehicle(params);
        } else {
            ServerWorld destination = info.enderworld;
            List<BlockPos> portalStream = EnderworldPortalBlock.findPortalPosToEnderworld(server, pos).toList();
            TeleportParams params = EnderworldPortalBlock.getTeleportParamsWithTargetPortalPositions(server, destination, player, portalStream, pos);
            if (params == null) {
                return;
            }
            VehicleTeleport.teleportWithVehicle(params);
        }
    }
    public record Rotation(boolean signFirst, boolean signSecond, boolean switched) { }
    public static int[][] getOffsets(long seed, int x, int z, boolean vehicle) {
        int[][] layers = vehicle ? EnderworldPortalBlock.VEHICLE_RESPAWN_LAYERS : EnderworldPortalBlock.RESPAWN_LAYERS;
        // this doesn't need configure, because this is a mathematical property of 3-dim space
        List<Rotation> rotations = getAllPossibleRotations();
        List<Vec3i> result = new ArrayList<>();
        Random random = new Random(seed ^ x ^ z);
        for (int[] layer : layers) {
            Collections.shuffle(rotations, random);
            for (Rotation rotation : rotations) {
                int newX = layer[0];
                int newY = layer[1];
                int newZ = layer[2];
                if (!rotation.signFirst) {
                    if (newX == 0) {
                        // these would be taken two times
                        continue;
                    }
                    newX = -newX;
                }
                if (!rotation.signSecond) {
                    if (newZ == 0) {
                        // these would be taken two times
                        continue;
                    }
                    newZ = -newZ;
                }
                if (rotation.switched) {
                    int temp = newZ;
                    newZ = newX;
                    newX = temp;
                }
                Vec3i toAdd = new Vec3i(newX, newY, newZ);
                if (!result.contains(toAdd)) {
                    result.add(toAdd);
                }
            }
        }
        // from https://stackoverflow.com/a/372134/3289974
        int[][] resultIntArray = new int[result.size()][];
        for (int i = 0; i < result.size(); i++) {
            Vec3i off = result.get(i);
            resultIntArray[i] = new int[] {off.getX(), off.getY(), off.getZ()};
        }
        return resultIntArray;
    }

    @NotNull
    private static List<Rotation> getAllPossibleRotations() {
        List<Rotation> rotations = new ArrayList<>();
        for (int signFirstInt = 0; signFirstInt < 2; signFirstInt++) {
            for (int signSecondInt = 0; signSecondInt < 2; signSecondInt++) {
                for (int switchedInt = 0; switchedInt < 2; switchedInt++) {
                    rotations.add(new Rotation(
                            signFirstInt == 1,
                            signSecondInt == 1,
                            switchedInt == 1
                    ));
                }
            }
        }
        return rotations;
    }

    public static @Nullable TeleportParams getTeleportParamsWithTargetPortalPositions(
            MinecraftServer server,
            ServerWorld destination,
            ServerPlayerEntity player,
            List<BlockPos> portalPositions,
            BlockPos oldPortalPos
    ) {
        Optional<BlockPos> chosenPortal = EnderworldPortalBlock.getValidPortal(
            portalPositions,
            server,
            destination,
            player,
            oldPortalPos.getY()
        );
        if (chosenPortal.isEmpty()) {
            return null;
        }
        Optional<Vec3d> destPosOptional = VehicleTeleport.findWakeUpPosition(
                player,
                destination,
                chosenPortal.get(),
                EnderworldPortalBlock.getOffsets(
                        destination.getSeed(),
                        chosenPortal.get().getX(),
                        chosenPortal.get().getZ(),
                        player.hasVehicle()
                ),
                false
        );
        // as this was checked inside getSpawnPositionFromStream, this should never happen
        assert destPosOptional.isPresent();
        Vec3d destPos = destPosOptional.get();
        return new TeleportParams(
                player,
                destination,
                chosenPortal.get(),
                destPos.getX(),
                destPos.getY(),
                destPos.getZ()
        );
    }

    public static BlockPos enderworldToOverworld(MinecraftServer server, BlockPos posEnderworld) {
        LazyInformation info = EnderworldPortalBlock.getInfo(server);
        return new BlockPos(
                Math.floorDiv(posEnderworld.getX(), info.dimensionScaleInverse),
                posEnderworld.getY(),
                Math.floorDiv(posEnderworld.getZ(), info.dimensionScaleInverse)
        );
    }
    public static Stream<BlockPos> findPortalPosToOverworld(MinecraftServer server, BlockPos posEnderworld) {
        LazyInformation info = EnderworldPortalBlock.getInfo(server);
        BlockPos posOverworld = EnderworldPortalBlock.enderworldToOverworld(server, posEnderworld);
        PointOfInterestStorage pointOfInterestStorage = server.getOverworld().getPointOfInterestStorage();

        // scraped and adjusted from net.minecraft.world.PortalForcer.getPortalRect
        pointOfInterestStorage.preloadChunks(server.getOverworld(), new BlockPos(posOverworld), 1);

        return pointOfInterestStorage.getInSquare(
                        ModPointsOfInterest.IS_ENDERWORLD_PORTAL,
                        new BlockPos(posOverworld),
                        info.dimensionScaleInverse,
                        PointOfInterestStorage.OccupationStatus.ANY
                )
                // I will need this when I implement the ticket propagation, therefore I will leave it there until consumed
                // this.world.getChunkManager().addTicket(ChunkTicketType.PORTAL, new ChunkPos(blockPos), 3, blockPos);
                .map(PointOfInterest::getPos)
                .filter(filterPos -> posOverworld.getX() == filterPos.getX())
                .filter(filterPos -> posOverworld.getZ() == filterPos.getZ());
    }
    public static BlockPos overworldToEnderworldStart(MinecraftServer server, BlockPos posOverworld) {
        LazyInformation info = EnderworldPortalBlock.getInfo(server);
        return new BlockPos(
                posOverworld.getX() * info.dimensionScaleInverse,
                posOverworld.getY(),
                posOverworld.getZ() * info.dimensionScaleInverse
        );
    }
    public static BlockPos overworldToEnderworldMiddle(MinecraftServer server, BlockPos posOverworld) {
        LazyInformation info = EnderworldPortalBlock.getInfo(server);
        return  new BlockPos(
                posOverworld.getX() * info.dimensionScaleInverse + info.dimensionScaleInverse / 2,
                posOverworld.getY(),
                posOverworld.getZ() * info.dimensionScaleInverse + info.dimensionScaleInverse / 2
        );
    }
    public static BlockPos overworldToEnderworldEnd(MinecraftServer server, BlockPos posOverworld) {
        LazyInformation info = EnderworldPortalBlock.getInfo(server);
        return new BlockPos(
                posOverworld.getX() * info.dimensionScaleInverse + info.dimensionScaleInverse - 1,
                posOverworld.getY(),
                posOverworld.getZ() * info.dimensionScaleInverse + info.dimensionScaleInverse - 1
        );
    }
    public static Stream<BlockPos> findPortalPosToEnderworld(MinecraftServer server, BlockPos posOverworld) {
        LazyInformation info = EnderworldPortalBlock.getInfo(server);
        Vec3i vecStart = EnderworldPortalBlock.overworldToEnderworldStart(server, posOverworld);
        BlockPos posMiddle = EnderworldPortalBlock.overworldToEnderworldMiddle(server, posOverworld);
        Vec3i vecEnd = EnderworldPortalBlock.overworldToEnderworldEnd(server, posOverworld);
        PointOfInterestStorage pointOfInterestStorage = info.enderworld.getPointOfInterestStorage();

        // scraped and adjusted from net.minecraft.world.PortalForcer.getPortalRect
        pointOfInterestStorage.preloadChunks(info.enderworld, posMiddle, info.dimensionScaleInverse);

        return pointOfInterestStorage.getInSquare(
                        ModPointsOfInterest.IS_ENDERWORLD_PORTAL,
                        posMiddle,
                        info.dimensionScaleInverse,
                        PointOfInterestStorage.OccupationStatus.ANY
            )
        // I will need this when I implement the ticket propagation, therefore I will leave it there until consumed
        // this.world.getChunkManager().addTicket(ChunkTicketType.PORTAL, new ChunkPos(blockPos), 3, blockPos);
                .map(PointOfInterest::getPos)
                .filter(filterPos -> vecStart.getX() <= filterPos.getX() && filterPos.getX() <= vecEnd.getX())
                .filter(filterPos -> vecStart.getZ() <= filterPos.getZ() && filterPos.getZ() <= vecEnd.getZ());
    }

    public static Optional<BlockPos> getValidPortal(
            List<BlockPos> possiblePortals,
            MinecraftServer server,
            ServerWorld destination,
            ServerPlayerEntity player,
            int yToFind
    ) {
        LazyInformation info = EnderworldPortalBlock.getInfo(server);
        WorldBorder worldBorder = destination.getWorldBorder();
        return possiblePortals.stream()
                .filter(filterPos -> info.portal
                        .placeable()
                        .check(destination, filterPos)
                        .isPresent()
                )
                .filter(worldBorder::contains)
                .sorted(new Comparator<BlockPos>() {
                    public Integer mappedValue(BlockPos pos) {
                        return Math.abs(pos.getY() - yToFind);
                    }
                    @Override
                    public int compare(BlockPos left, BlockPos right) {
                        return mappedValue(left).compareTo(mappedValue(right));
                    }
                }).filter((BlockPos filterPortalBlockedPos) -> VehicleTeleport.findWakeUpPosition(
                        player,
                        destination,
                        filterPortalBlockedPos,
                        EnderworldPortalBlock.getOffsets(
                                destination.getSeed(),
                                filterPortalBlockedPos.getX(),
                                filterPortalBlockedPos.getZ(),
                                player.hasVehicle()
                        ),
                        false
                ).isPresent())
                .findFirst();

    }

    // this isn't deprecated in later versions, so you can ignore it
    @SuppressWarnings("deprecation")
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (world.isClient()) {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        }
        // can't use getInfo() because sometimes, this method is called on server starting leading to rare
        // crashes
        EnderlingStructure portal = EnderlingStructureInitializer
                .enderlingStructureRegistry
                .get(new Identifier(TechnoEnderling.MOD_ID, "enderworld_portal"));
        ArbitraryStructure portalGenerated = EnderlingStructureInitializer
                .arbitraryStructureRegistry
                .get(new Identifier(TechnoEnderling.MOD_ID, "enderworld_portal_generated"));

        assert world.getServer() != null;
        if (state.get(EnderworldPortalBlock.GENERATED)) {
            if (portalGenerated.check(world, pos).isEmpty()) {
                removeNetherDestination(world.getServer(), world, pos);
                return this.unactiveCounterpart.getDefaultState();
            }
        } else {
            if (portal.placeable().check(world, pos).isEmpty()) {
                removeNetherDestination(world.getServer(), world, pos);
                return this.unactiveCounterpart.getDefaultState();
            }
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    public record NetherInstance(int id, String name, BlockPos pos) { }

    @Override
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
            super.onBroken(world, pos, state);
            return;
        }
        removeNetherDestination(world.getServer(), world, pos);
        super.onBroken(world, pos, state);
    }

    public static void removeNetherDestination(MinecraftServer server, WorldAccess world, BlockPos pos) {
        RegistryKey<World> netherKey = RegistryKey.of(
                Registry.WORLD_KEY,
                new Identifier("minecraft:the_nether")
        );
        ServerWorld nether = server.getWorld(netherKey);
        assert nether != null;
        boolean isNether = world.getDimension().equals(nether.getDimension());
        if (isNether) {
            server.getPlayerManager().getPlayerList().forEach(
                    arbitraryPlayer -> ((IServerPlayerNetherEnderworldPortal)arbitraryPlayer).techno_nederling$remove(pos)
            );
        }
    }
}
