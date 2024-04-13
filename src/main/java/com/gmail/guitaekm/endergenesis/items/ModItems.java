package com.gmail.guitaekm.endergenesis.items;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import com.gmail.guitaekm.endergenesis.blocks.ModBlocks;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {
    public static final BlockItem ENDERWORLD_PORTAL_BLOCK_1 = new BlockItem(
            ModBlocks.ENDERWORLD_PORTAL_BLOCK_1,
            new FabricItemSettings()
    );
    public static final BlockItem ENDERWORLD_PORTAL_BLOCK_2 = new BlockItem(
            ModBlocks.ENDERWORLD_PORTAL_BLOCK_2,
            new FabricItemSettings()
    );
    public static final BlockItem ENDERWORLD_PORTAL_BLOCK_3 = new BlockItem(
            ModBlocks.ENDERWORLD_PORTAL_BLOCK_3,
            new FabricItemSettings()
    );
    public static final BlockItem INFUSED_GLOWSTONE = new BlockItem(
            ModBlocks.INFUSED_GLOWSTONE,
            new FabricItemSettings().group(ItemGroup.TRANSPORTATION)
    );
    public static final BlockItem INFUSED_GOLD_BLOCK = new BlockItem(
            ModBlocks.INFUSED_GOLD_BLOCK,
            new FabricItemSettings().group(ItemGroup.TRANSPORTATION)
    );
    public static final BlockItem POCKET_PORTAL_BLOCK = new BlockItem(
            ModBlocks.POCKET_PORTAL_BLOCK,
            new FabricItemSettings()
    );
    public static void register() {
        Registry.register(
                Registry.ITEM,
                new Identifier(EnderGenesis.MOD_ID, "enderworld_portal_block_1"),
                ModItems.ENDERWORLD_PORTAL_BLOCK_1
        );
        Registry.register(
                Registry.ITEM,
                new Identifier(EnderGenesis.MOD_ID, "enderworld_portal_block_2"),
                ModItems.ENDERWORLD_PORTAL_BLOCK_2
        );
        Registry.register(
                Registry.ITEM,
                new Identifier(EnderGenesis.MOD_ID, "enderworld_portal_block_3"),
                ModItems.ENDERWORLD_PORTAL_BLOCK_3
        );
        Registry.register(
                Registry.ITEM,
                new Identifier(EnderGenesis.MOD_ID, "infused_glowstone"),
                ModItems.INFUSED_GLOWSTONE
        );
        Registry.register(
                Registry.ITEM,
                new Identifier(EnderGenesis.MOD_ID, "infused_gold_block"),
                ModItems.INFUSED_GOLD_BLOCK
        );
        Registry.register(
                Registry.ITEM,
                new Identifier(EnderGenesis.MOD_ID, "pocket_portal_block"),
                ModItems.POCKET_PORTAL_BLOCK
        );
    }
}
