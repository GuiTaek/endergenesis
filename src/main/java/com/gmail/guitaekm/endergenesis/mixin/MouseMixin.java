package com.gmail.guitaekm.endergenesis.mixin;

import com.gmail.guitaekm.endergenesis.access.IMouseMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(Mouse.class)
public class MouseMixin implements IMouseMixin {
    @Unique
    protected boolean keepScreen;

    @Override
    public void endergenesis$setKeepScreen() {
        this.keepScreen = true;
    }

    @Override
    public void endergenesis$unsetKeepScreen() {
        this.keepScreen = false;
    }

    @Redirect(method = "lockCursor",
    at=@At(value="INVOKE", target="Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
    private void keepScreen(MinecraftClient client, Screen screen) {
        if (!this.keepScreen) {
            client.setScreen(null);
        }
    }
}
