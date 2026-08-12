package net.sabio.wandsofcombat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sabio.wandsofcombat.item.*;
import net.sabio.wandsofcombat.mana.ManaManager;
import net.sabio.wandsofcombat.network.ManaSyncPacket;
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
        ElectricWandLightningHandler.initialize();
        ElectricWandPassiveHandler.initialize();
        LightningStrikeHandler.initialize();
        PhantomModeHandler.initialize();
        PullAttackScheduler.initialize();
        PhantomSyncPacket.initialize();
        MagmaWandHandler.initialize();
        IceWandComboHandler.initialize();
        ManaSyncPacket.initialize();
        ManaManager.initialize();
    }
}