package net.sabio.wandsofcombat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.sabio.wandsofcombat.item.PhantomWandItem;
import net.sabio.wandsofcombat.network.PhantomSyncPacket;

@Environment(EnvType.CLIENT)
public class PhantomSyncClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PhantomSyncPacket.TYPE, (payload, context) -> {
            boolean active = payload.active();
            context.client().execute(() -> {
                if (context.client().player != null) {
                    context.client().player.noPhysics = active;
                }
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (PhantomWandItem.phantomPlayers.contains(client.player.getUUID())) {
                client.player.noPhysics = true;
            }
        });
    }
}
