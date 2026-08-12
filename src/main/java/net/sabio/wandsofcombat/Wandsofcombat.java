package net.sabio.wandsofcombat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.sabio.wandsofcombat.item.*;
import net.sabio.wandsofcombat.network.PhantomSyncPacket;

import java.util.UUID;
import java.util.logging.Logger;

public class Wandsofcombat implements ModInitializer {
    public static final String MOD_ID = "wandsofcombat";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        MagnetPullManager.initialize();
        ElectricWandLightningInteractionHandler.initialize();
        ElectricWandPassiveInteractionHandler.initialize();
        LightningStrikeInteractionHandler.initialize();
        PhantomModeInteractionHandler.initialize();
        PullAttackScheduler.initialize();
        PhantomSyncPacket.initialize();
        MagmaWandInteractionHandler.initialize();
        IceWandComboInteractionHandler.initialize();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.minecraft.server.network.ServerPlayer player = handler.player;
            WandCooldownState state = WandCooldownState.get(server);
            UUID id = player.getUUID();
            for (String key : new String[]{"magnet","ice","electric","phantom","magma_ability","magma_ultimate"}) {
                int ticks = state.getRemainingTicks(id, key);
                if (ticks > 0) {
                    Item item = switch (key) {
                        case "magnet" -> ModItems.MAGNET_WAND;
                        case "ice" -> ModItems.ICE_WAND;
                        case "electric" -> ModItems.ELECTRIC_WAND;
                        case "phantom" -> ModItems.PHANTOM_WAND;
                        default -> ModItems.MAGMA_WAND;
                    };
                    player.getCooldowns().set(new ItemStack(item), ticks);
                }
            }
        });
    }
}