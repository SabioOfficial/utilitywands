package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ElectricWandPassiveHandler {
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(ElectricWandPassiveHandler::onTick);
    }

    private static void onTick(MinecraftServer server) {
        for (ServerLevel world : server.getAllLevels()) {
            for (ServerPlayer player : world.players()) {
                if (isHoldingElectricWand(player)) {
                    if (LightningFireTracker.isLightningFire(player)) {
                        player.extinguishFire();
                        LightningFireTracker.clear(player);
                    }
                }
            }
        }
    }

    public static boolean isHoldingElectricWand(Player player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand  = player.getItemInHand(InteractionHand.OFF_HAND);
        return mainHand.getItem() instanceof ElectricWandItem || offHand.getItem() instanceof ElectricWandItem;
    }
}