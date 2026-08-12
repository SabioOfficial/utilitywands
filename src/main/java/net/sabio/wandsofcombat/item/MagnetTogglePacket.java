package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sabio.wandsofcombat.Wandsofcombat;

public class MagnetTogglePacket {
    public record Payload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Payload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "magnet_toggle"));
        public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = StreamCodec.unit(new Payload());
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
    public static void initializeServer() {
        PayloadTypeRegistry.clientboundPlay().register(Payload.ID, Payload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Payload.ID, (payload, context) ->
                context.server().execute(() ->
                        MagnetPullManager.toggleRepelMode(context.player())));
    }
    public static void initializeClient() {
        
    }
    public static void sendToggle() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new Payload());
    }
}