package com.gmail.guitaekm.endergenesis.enderling_structure;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import com.gmail.guitaekm.endergenesis.blocks.EnderworldPortalBlock;
import com.gmail.guitaekm.endergenesis.event.EnderlingStructureEvents;
import com.gmail.guitaekm.endergenesis.items.ModItems;
import com.gmail.guitaekm.endergenesis.teleport.TeleportParams;
import com.gmail.guitaekm.endergenesis.worldgen.ModWorlds;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LinkEnderworldPortals implements EnderlingStructureEvents.OnConvertListener {
    public void register() {
        EnderlingStructureEvents.ON_CONVERT.register(this);
    }

    @Override
    public boolean onConvert(ServerPlayerEntity player, ServerWorld world, Identifier portalId, EnderlingStructure portal, BlockPos root) {
        if (!portalId.equals(new Identifier(EnderGenesis.MOD_ID, "enderworld_portal"))) {
            return true;
        }
        if (!EnderworldPortalBlock.allowedDimensions.contains(world.getRegistryKey().getValue())) {
            return false;
        }
        if (world.getRegistryKey().equals(RegistryKey.of(
                Registry.WORLD_KEY,
                new Identifier("minecraft", "the_nether")
        ))) {
            return true;
        }
        return LinkEnderworldPortals.placePortal(player, world, portalId, portal, root);
    }

    public static boolean placePortal (ServerPlayerEntity player, ServerWorld world, Identifier portalId, EnderlingStructure portal, BlockPos root) {
        if (!portalId.equals(new Identifier(EnderGenesis.MOD_ID, "enderworld_portal"))) {
            return true;
        }
        MinecraftServer server = player.getServer();
        assert server != null;
        ServerWorld otherWorld, enderworld = ModWorlds.getInfo(server).enderworld();
        Optional<BlockPos> otherRoot;
        assert enderworld != null;
        if (world.equals(world.getServer().getOverworld())) {
            otherWorld = enderworld;
            otherRoot =  LinkEnderworldPortals.findPortalSpawnEnderworld(player, enderworld, root);
        } else if (world.equals(enderworld)) {
            otherWorld = world.getServer().getOverworld();
            otherRoot = LinkEnderworldPortals.findPortalSpawnOverworld(player, otherWorld, root);
        } else {
            return true;
        }
        if (otherRoot.isEmpty()) {
            return true;
        }
        if (LinkEnderworldPortals.tryTakePortalItems(player)) {
            buildPlatformAndClearRoom(otherWorld, player, otherRoot.get());
            portal.placeable().place(otherWorld, otherRoot.get().subtract(new Vec3i(0, 2, 0)), new Vec3i(0, 0, 0), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            return true;
        }
        return false;
    }

    public static BlockPos overworldToEnderworldRandom(WorldAccess enderworld, BlockPos overworldPos) {
        Random random = enderworld.getRandom();
        BlockPos start = EnderworldPortalBlock.overworldToEnderworldStart(enderworld.getServer(), overworldPos);
        BlockPos end = EnderworldPortalBlock.overworldToEnderworldEnd(enderworld.getServer(), overworldPos);
        return  new BlockPos(
                MathHelper.nextBetween(random, start.getX(), end.getX()),
                MathHelper.nextBetween(random, start.getY(), end.getY()),
                MathHelper.nextBetween(random, start.getZ(), end.getZ())
        );
    }

    /**
     * gives the exact position of the portal that is to be placed / recreated
     * @param player the player who is trying to activate the portal
     * @param enderworld the enderworld dimension
     * @param overworldPos the position of the deactivated, placed portal in the overworld
     * @return the position in the enderworld
     */
    public static Optional<BlockPos> findPortalSpawnEnderworld(
            ServerPlayerEntity player,
            ServerWorld enderworld,
            BlockPos overworldPos) {
        List<BlockPos> possiblePortals = EnderworldPortalBlock.findPortalPosToEnderworld(enderworld.getServer(), overworldPos).toList();
        BlockPos randomPosition = overworldToEnderworldRandom(enderworld, overworldPos);
        return findPortalSpawnHelper(
                player,
                enderworld,
                possiblePortals,
                overworldPos,
                randomPosition
        );
    }

    /**
     * gives the exact position of the portal that is to be placed / recreated
     * @param player the player who is trying to activate the portal
     * @param overworld the overworld dimension
     * @param enderworldPos the position of the deactivated, placed portal in the enderworld
     * @return the position of the portal in the overworld
     */
    public static Optional<BlockPos> findPortalSpawnOverworld(ServerPlayerEntity player, ServerWorld overworld, BlockPos enderworldPos) {
        List<BlockPos> possiblePortals = EnderworldPortalBlock.findPortalPosToOverworld(overworld.getServer(), enderworldPos).toList();
        return findPortalSpawnHelper(
                player,
                overworld,
                possiblePortals,
                enderworldPos,
                EnderworldPortalBlock.enderworldToOverworld(overworld.getServer(), enderworldPos)
        );
    }

    /**
     * this function removes code duplication from findPortalSpawnEnderworld and findPortalSpawnOverworld
     * @param player the player initiating the portal linking
     * @param destinationWorld the destination dimension of the linked portal
     * @param fromPos the position of the deactivated, built portal in the dimension the player currently is
     * @return the exact position of the portal to be built
     */
    public static Optional<BlockPos> findPortalSpawnHelper(ServerPlayerEntity player, ServerWorld destinationWorld, List<BlockPos> possiblePortals, BlockPos fromPos, BlockPos forcedSpawn) {
        if (!possiblePortals.isEmpty()) {
            TeleportParams params = EnderworldPortalBlock.getTeleportParamsWithTargetPortalPositions(
                    destinationWorld.getServer(),
                    destinationWorld,
                    player,
                    possiblePortals,
                    fromPos
            );
            if (params == null) {
                return possiblePortals
                        .stream().min(new Comparator<BlockPos>() {
                            public Integer mappedValue(BlockPos pos) {
                                return Math.abs(pos.getY() - fromPos.getY());
                            }

                            @Override
                            public int compare(BlockPos left, BlockPos right) {
                                return mappedValue(left).compareTo(mappedValue(right));
                            }
                        });
            }
            return Optional.empty();
        }
        return Optional.of(forcedSpawn);
    }

    /**
     * clears a 3x3x3 space and below build a platform if the player has enough items and there is not
     * already something to stand on. Will replace non-solid blocks like fence gates for obvious reasons.
     * Keep in mind, that the parameters have to be from the other side, where the portal will be build
     * @param world the dimension of the other side
     * @param player the player trying to build the portal
     * @param root the root position of the other portal
     */
    public static void buildPlatformAndClearRoom(ServerWorld world, ServerPlayerEntity player, BlockPos root) {
        LinkEnderworldPortals.clearRoom(world, root);
        List<ItemStack> platformStacks = new ArrayList<>();
        Set<Item> possiblePlatformItems = Sets.newHashSet(
                Items.END_STONE,
                Items.STONE,
                Items.GRANITE,
                Items.DIORITE,
                Items.ANDESITE
        );
        for (int i = 0; i < player.getInventory().size(); ++i) {
            ItemStack itemStack = player.getInventory().getStack(i);
            if(possiblePlatformItems.contains(itemStack.getItem())) {
                platformStacks.add(itemStack);
            }
        }
        for (Vec3i offset : LinkEnderworldPortals.iteratePlatformPositions(world.getRandom())) {
            BlockPos pos = root.add(offset);
            if (world.getBlockState(pos).isSideSolidFullSquare(
                    world,
                    pos,
                    Direction.UP
            )) {
                continue;
            }
            Block.dropStacks(world.getBlockState(pos), world, pos);
            BlockState state;
            if (player.isCreative()) {
                state = Blocks.END_STONE.getDefaultState();
            } else {
                if (platformStacks.isEmpty()) {
                    break;
                }
                state = ((BlockItem)platformStacks.get(0).split(1).getItem())
                        .getBlock()
                        .getDefaultState();
                if (platformStacks.get(0).getCount() == 0) {
                    platformStacks.remove(0);
                }
            }
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
        }
    }

    /**
     * tries to take only the portal items from the player, that is,
     * the glowstone and the two gold blocks (or the infused ones)
     * @param player the player how tries to activate the portal
     * @return true, if the items are taken
     */
    public static boolean tryTakePortalItems(ServerPlayerEntity player) {
        if (player.isCreative()) {
            return true;
        }
        // I don't think you can anyhow make it usable in data packs, has to be hard coded
        // better would be a config though
        List<ItemStack> goldStacks = new ArrayList<>();
        @Nullable ItemStack glowstoneStack = null;
        // scraped and modified from net.minecraft.entity.player.PlayerEntity.getArrowType
        for (int i = 0; i < player.getInventory().size(); ++i) {
            ItemStack itemStack = player.getInventory().getStack(i);
            if (itemStack.getItem().equals(ModItems.INFUSED_GLOWSTONE)) {
                if (glowstoneStack == null) {
                    glowstoneStack = itemStack;
                }
            } else if (itemStack.getItem().equals(ModItems.INFUSED_GOLD_BLOCK)) {
                if (goldStacks.stream().mapToInt(ItemStack::getCount).sum() < 2) {
                    goldStacks.add(itemStack);
                }
            }
        }
        for (int i = 0; i < player.getInventory().size(); ++i) {
            ItemStack itemStack = player.getInventory().getStack(i);
            if (itemStack.getItem().equals(Items.GLOWSTONE)) {
                if (glowstoneStack == null) {
                    glowstoneStack = itemStack;
                }
            } else if (itemStack.getItem().equals(Items.GOLD_BLOCK)) {
                if (goldStacks.stream().mapToInt(ItemStack::getCount).sum() < 2) {
                    goldStacks.add(itemStack);
                }
            }
        }

        if (glowstoneStack != null) {
            if (goldStacks.stream().mapToInt(ItemStack::getCount).sum() >= 2) {
                glowstoneStack.split(1);
                ItemStack goldStack1 = goldStacks.get(0);
                if (goldStack1.getCount() == 1) {
                    goldStack1.split(1);
                    goldStacks.get(1).split(1);
                } else {
                    goldStack1.split(2);
                }
                return true;
            }
        }
        return false;
    }
    protected static List<Vec3i> iteratePlatformPositions(Random random) {
        List<Vec3i> result = new ArrayList<>();
        // configure yoff
        int yoff = -2;
        // configure depth
        for (int depth = 0; depth <= 3; depth++) {
            // Set to have no duplication
            Set<Vec3i> unshuffledSet = new HashSet<>();
            for (int iterCoord = -depth; iterCoord <= depth; iterCoord++) {
                unshuffledSet.add(new Vec3i(iterCoord, yoff, -depth));
                unshuffledSet.add(new Vec3i(iterCoord, yoff, depth));
                unshuffledSet.add(new Vec3i(-depth, yoff, iterCoord));
                unshuffledSet.add(new Vec3i(depth, yoff, iterCoord));
            }
            List<Vec3i> toShuffledRing = new ArrayList<>(unshuffledSet);
            Collections.shuffle(toShuffledRing, random);
            result.addAll(toShuffledRing);
        }
        return result;
    }

    public static void clearRoom(ServerWorld world, BlockPos pos) {
        // configure
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos toBreak = pos.add(x, y, z);
                    Block.dropStacks(world.getBlockState(toBreak), world, toBreak);
                    world.setBlockState(
                            toBreak,
                            Blocks.AIR.getDefaultState(),
                            Block.NOTIFY_ALL | Block.FORCE_STATE
                    );
                }
            }
        }
    }
}
