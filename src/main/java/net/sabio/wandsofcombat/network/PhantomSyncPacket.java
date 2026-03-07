package net.sabio.wandsofcombat.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sabio.wandsofcombat.Wandsofcombat;

public record PhantomSyncPacket(boolean active) implements CustomPayload {
    public static final CustomPayload.Id<PhantomSyncPacket> ID = new CustomPayload.Id<>(Identifier.of(Wandsofcombat.MOD_ID, "phantom_sync"));
    public static final PacketCodec<PacketByteBuf, PhantomSyncPacket> CODEC = PacketCodec.tuple(PacketCodecs.BOOLEAN, PhantomSyncPacket::active, PhantomSyncPacket::new);
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
