package net.sabio.wandsofcombat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.sabio.wandsofcombat.network.ManaSyncPacket;

@Environment(EnvType.CLIENT)
public class ManaSyncClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ManaSyncPacket.TYPE, (payload, context) ->
                context.client().execute(() -> ManaClientState.update(payload.points(), payload.maxPoints()))
        );
    }
}
