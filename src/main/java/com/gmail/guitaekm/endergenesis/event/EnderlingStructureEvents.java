package com.gmail.guitaekm.endergenesis.event;

import com.gmail.guitaekm.endergenesis.enderling_structure.EnderlingStructure;
import com.gmail.guitaekm.endergenesis.utils.Utils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EnderlingStructureEvents {
    public interface OnGenerateListener { boolean onGenerate(Random random, Identifier onGenerateId, BlockPos root); }
    public static Event<OnGenerateListener> ON_GENERATE = EventFactory.createArrayBacked(
            OnGenerateListener.class,
            (callbacks -> (random, onGenerateId, root) -> {
                List<OnGenerateListener> callbacksCopy = Arrays.asList(callbacks);
                // I want it to be random
                Collections.shuffle(callbacksCopy, random);
                for (OnGenerateListener callback : callbacksCopy) {
                    if (!callback.onGenerate(random, onGenerateId, root)) {
                        return false;
                    }
                }
                return true;
            })
    );

    public interface OnConvertListener {
        boolean onConvert(ServerPlayerEntity player, ServerWorld world, Identifier structureId, EnderlingStructure structure, BlockPos root);
    }
    public static Event<OnConvertListener> ON_CONVERT = EventFactory.createArrayBacked(
            OnConvertListener.class,
            (callbacks -> (player, world, structureId, structure, root) -> {
                // I want it to be random
                List<OnConvertListener> callbacksCopy = Utils.shuffleList(Arrays.asList(callbacks), world.getRandom());

                for (OnConvertListener callback : callbacksCopy) {
                    if (!callback.onConvert(player, world, structureId, structure, root)) {
                        return false;
                    }
                }
                return true;
            })
    );
}
