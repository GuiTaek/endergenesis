package com.gmail.guitaekm.endergenesis.gui;

import com.gmail.guitaekm.endergenesis.EnderGenesis;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class TranslateString {
    public static Text translate(String identifier) {
        return Text.translatable(
                Util.createTranslationKey(
                        identifier,
                        new Identifier(EnderGenesis.MOD_ID, "screen")
                )
        );
    }
}
