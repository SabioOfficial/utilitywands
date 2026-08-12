package net.sabio.wandsofcombat.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sabio.wandsofcombat.Wandsofcombat;

public record PhantomSyncPacket(boolean active) implements CustomPacketPayload {
    public static final Identifier PACKET_ID = Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "phantom_sync");
    public static final CustomPacketPayload.Type<PhantomSyncPacket> TYPE = new CustomPacketPayload.Type<>(PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PhantomSyncPacket> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, PhantomSyncPacket::active, PhantomSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
    }
}