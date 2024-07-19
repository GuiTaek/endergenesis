package com.gmail.guitaekm.endergenesis.point_of_interest;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import com.gmail.guitaekm.endergenesis.blocks.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.function.Predicate;

public class ModPointsOfInterest {

    public static PointOfInterestType ENDERWORLD_PORTAL;

    public static final Predicate<RegistryEntry<PointOfInterestType>> IS_ENDERWORLD_PORTAL = (RegistryEntry<PointOfInterestType> poiType) -> {
        for (BlockState state : ModBlocks.ENDERWORLD_PORTAL_BLOCK_1.getStateManager().getStates()) {
            if (poiType.value().contains(state)) {
                return true;
            }
        }
        return false;
    };
    public static void registerClass() {
        ModPointsOfInterest.ENDERWORLD_PORTAL = PointOfInterestHelper.register(
                new Identifier(EnderGenesis.MOD_ID, "enderworld_portal_block_1"),
                1,
                1,
                ModBlocks.ENDERWORLD_PORTAL_BLOCK_1.getStateManager().getStates()
        );
    }
}
