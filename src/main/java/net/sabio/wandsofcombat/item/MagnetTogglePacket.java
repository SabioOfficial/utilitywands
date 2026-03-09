package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sabio.wandsofcombat.Wandsofcombat;

public class MagnetTogglePacket {
    public record Payload() implements CustomPayload {
        public static final CustomPayload.Id<Payload> ID = new CustomPayload.Id<>(Identifier.of(Wandsofcombat.MOD_ID, "magnet_toggle"));
        public static final PacketCodec<PacketByteBuf, Payload> CODEC = PacketCodec.unit(new Payload());
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public static void initializeServer() {
        PayloadTypeRegistry.playC2S().register(Payload.ID, Payload.CODEC);
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