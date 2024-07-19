package com.gmail.guitaekm.endergenesis.networking;

import com.gmail.guitaekm.endergenesis.blocks.EnderworldPortalBlock;
import net.minecraft.network.PacketByteBuf;

import java.util.List;

public class TeleportDestinations {

    public final EnderworldPortalBlock.NetherInstance source;
    public final List<EnderworldPortalBlock.NetherInstance> destinations;

    public TeleportDestinations(EnderworldPortalBlock.NetherInstance source, List<EnderworldPortalBlock.NetherInstance> destinations) {
        this.source = source;
        this.destinations = destinations;
    }

    public TeleportDestinations(PacketByteBuf packet) {
        int sourceIndex = packet.readInt();
        List<EnderworldPortalBlock.NetherInstance> destinations = packet.readList(new PacketByteBuf.PacketReader<>() {
            int id = 0;

            @Override
            public EnderworldPortalBlock.NetherInstance apply(PacketByteBuf packetByteBuf) {
                return new EnderworldPortalBlock.NetherInstance(id++, packet.readString(), packet.readBlockPos());
            }
        });
        if (sourceIndex == -1) {
            this.source = null;
        } else {
            this.source = destinations.get(sourceIndex);
        }
        this.destinations = destinations;
    }

    public void writeToPacket(PacketByteBuf packet) {
        packet.writeInt(this.destinations.indexOf(this.source));
        packet.writeCollection(
                this.destinations,
                (packetByteBuf, netherInstance) -> {
                    packetByteBuf.writeString(netherInstance.name());
                    packetByteBuf.writeBlockPos(netherInstance.pos());
                });
    }
}
