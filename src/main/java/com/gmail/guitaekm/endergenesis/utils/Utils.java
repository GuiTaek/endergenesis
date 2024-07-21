package com.gmail.guitaekm.endergenesis.utils;

import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static <T> List<T> shuffleList(List<T> unshuffled, Random random) {
        if (unshuffled.isEmpty()) {
            return new ArrayList<>();
        }
        if (unshuffled.size() == 1) {
            return unshuffled;
        }
        List<T> shuffled = new ArrayList<>();
        for (int i = 0; i < unshuffled.size(); i++) {
            int ind = random.nextBetween(0, unshuffled.size() - 1);
            shuffled.add(unshuffled.get(ind));
            // found no O(log(n)) data structure, problem: renaming the
            // indexes of the following elements in O(n)
            List<T> temp = unshuffled.subList(0, ind);
            temp.addAll(unshuffled.subList(ind + 1, unshuffled.size()));
            unshuffled = temp;
        }
        shuffled.add(unshuffled.stream().findFirst().get());
        return shuffled;
    }
}
