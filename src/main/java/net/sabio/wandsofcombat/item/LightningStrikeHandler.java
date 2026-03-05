package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.player.PlayerEntity;

public class LightningStrikeHandler {
    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof PlayerEntity player)) return true;
            if (!ElectricWandPassiveHandler.isHoldingElectricWand(player)) return true;

            if (source.getType().msgId().equals("lightningBolt")) {
                LightningFireTracker.markLightningFire(player);
                return false;
            }

            return true;
        });
    }
}