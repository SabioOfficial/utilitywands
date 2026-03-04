package net.sabio.utilitywands;

import net.fabricmc.api.ModInitializer;
import net.sabio.utilitywands.item.ModItems;

import java.util.logging.Logger;

public class Utilitywands implements ModInitializer {
    public static final String MOD_ID = "utilitywands";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.register();
    }
}