package com.gmail.guitaekm.endergenesis.enderling_structure;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.*;
import net.minecraft.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.WorldAccess;
import com.gmail.guitaekm.endergenesis.utils.Utils;

import java.util.*;


public class ArbitraryStructure {
    public final List<PaletteElement> palette;
    public final Map<Vec3i, Integer> structure;
    public final List<Vec3i> checkOffsets;
    public final Vec3i size;

    public ArbitraryStructure(NbtCompound nbt) {
        NbtList sizeNbt = nbt.getList("size", NbtElement.INT_TYPE);
        this.size = new Vec3i(sizeNbt.getInt(0), sizeNbt.getInt(1), sizeNbt.getInt(2));
        NbtList paletteRaw = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
        this.structure = new HashMap<>();
        this.palette = new ArrayList<>();
        this.checkOffsets = new ArrayList<>();
        for (NbtElement elem : paletteRaw) {
            if (!(elem instanceof NbtCompound)) {
                throw new IllegalArgumentException("Palette element is not a nbtCompound");
            }
            palette.add(PaletteElement.create((NbtCompound) elem));
        }
        NbtList blocks = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);
        for (NbtElement block : blocks) {
            if (block instanceof NbtCompound blockCompound) {
                NbtList nbtPos = blockCompound.getList("pos", NbtElement.INT_TYPE);
                Vec3i pos = new Vec3i(nbtPos.getInt(0), nbtPos.getInt(1), nbtPos.getInt(2));
                this.structure.put(pos, blockCompound.getInt("state"));
                continue;
            }
            throw new IllegalArgumentException("The structure has wrong format");
        }
        if(!nbt.contains("offsets")) {
            // so they also work when just a vanilla structure (kind of)
            checkOffsets.add(new Vec3i(0, 0, 0));
            return;
        }
        for (NbtElement rawOffset : nbt.getList("offsets", NbtElement.LIST_TYPE)) {
            NbtList offset = (NbtList) rawOffset;
            this.checkOffsets.add(new Vec3i(offset.getInt(0), offset.getInt(1), offset.getInt(2)));
        }
    }
    public void place(WorldAccess world, BlockPos pos, Vec3i offset, int flags) {
        for (Map.Entry<Vec3i, Integer> entry : this.structure.entrySet()) {
            PaletteElement elem = this.palette.get(entry.getValue());
            BlockState toPlaceState = elem.getState();
            if (toPlaceState == null) {
                TagKey<Block> tag = elem.getTag();
                assert tag != null;
                List<Block> list = Registry.BLOCK.getOrCreateEntryList(tag).stream().map(RegistryEntry::value).toList();
                toPlaceState = list.get(world.getRandom().nextInt(list.size())).getDefaultState();
            }
            world.setBlockState(pos.subtract(offset).add(entry.getKey()), toPlaceState, flags);
        }
    }
    public Optional<BlockPos> check(WorldAccess world, BlockPos pos) {
        Random random = world.getRandom();
        List<Vec3i> offsetsShuffled = Utils.shuffleList(new ArrayList<Vec3i>(), random);


        //Collections.shuffle(offsetsShuffled, world.getRandom());
        for (Vec3i offset : offsetsShuffled) {
            if (this.checkWithOffset(world, pos, offset)) {
                return Optional.of(pos.subtract(offset));
            }
        }
        return Optional.empty();
    }
    public boolean checkWithOffset(WorldAccess world, BlockPos pos, Vec3i offset) {
        BlockPos alteredPos = pos.subtract(offset);
        for (Map.Entry<Vec3i, Integer> entry : this.structure.entrySet()) {
            BlockState toTestState = world.getBlockState(alteredPos.add(entry.getKey()));
            PaletteElement elem = this.palette.get(entry.getValue());
            BlockState toCompareState = elem.getState();
            if (toCompareState != null) {
                if (!toTestState.equals(toCompareState)) {
                    return false;
                }
                continue;
            }
            TagKey<Block> tag = elem.getTag();
            assert tag != null;
            if (Registry.BLOCK.getOrCreateEntry(Registry.BLOCK.getKey(toTestState.getBlock()).get()).isIn(tag)) {
                return false;
            }
        }
        return true;
    }
}
