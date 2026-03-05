package net.sabio.wandsofcombat;

import net.fabricmc.api.ModInitializer;
import net.sabio.wandsofcombat.item.*;

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
    }
}