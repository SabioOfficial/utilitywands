package net.sabio.wandsofcombat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.sabio.wandsofcombat.network.PhantomSyncPacket;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public class PhantomSyncClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PhantomSyncPacket.ID, (payload, context) -> {
            boolean active = payload.active();
            context.client().execute(() -> {
                if (context.client().player != null) {
                    Objects.requireNonNull(context.client().player).noClip = active;
                }
            });
        });
    }
}
