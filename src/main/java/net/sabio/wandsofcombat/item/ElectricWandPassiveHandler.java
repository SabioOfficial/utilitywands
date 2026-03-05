package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

public class ElectricWandPassiveHandler {
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(ElectricWandPassiveHandler::onTick);
    }

    private static void onTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (PlayerEntity player : world.getPlayers()) {
                if (isHoldingElectricWand(player)) {
                    if (player.getDataTracker() != null) {
                        if (LightningFireTracker.isLightningFire(player)) {
                            player.extinguish();
                            LightningFireTracker.clear(player);
                        }
                    }
                }
            }
        }
    }

    public static boolean isHoldingElectricWand(PlayerEntity player) {
        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand  = player.getStackInHand(Hand.OFF_HAND);
        return mainHand.getItem() instanceof ElectricWandItem || offHand.getItem() instanceof ElectricWandItem;
    }
}