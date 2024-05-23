package com.gmail.guitaekm.endergenesis.mixin;

import com.gmail.guitaekm.endergenesis.access.IIgnoreVehicleMovePackets;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class IgnoreVehicleMovePackets implements IIgnoreVehicleMovePackets {
    private boolean ignore = false;
    @Override
    public void endergenesis$setIgnore(boolean ignore) {
        this.ignore = ignore;
    }
    @Override
    public boolean endergenesis$getIgnore() {
        return this.ignore;
    }
    @Inject(method = "onVehicleMove", at = @At("HEAD"), cancellable = true)
    public void ignorePacket(VehicleMoveC2SPacket packet, CallbackInfo ci) {
        if (this.ignore) {
            ci.cancel();
        }
    }
}
