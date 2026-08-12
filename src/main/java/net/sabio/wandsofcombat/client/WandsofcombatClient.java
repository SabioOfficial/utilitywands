package net.sabio.wandsofcombat.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.sabio.wandsofcombat.Wandsofcombat;
import net.sabio.wandsofcombat.item.MagnetTogglePacket;
import org.lwjgl.glfw.GLFW;

public class WandsofcombatClient implements ClientModInitializer {
    private static KeyMapping toggleRepelKey;

    @Override
    public void onInitializeClient() {
        MagnetTogglePacket.initializeClient();
        KeyMapping.Category wandsCategory;
        try {
            wandsCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "wands"));
        } catch (IllegalArgumentException error) {
            wandsCategory = KeyMapping.Category.MISC;
        }
        toggleRepelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.wandsofcombat.toggle_repel",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                wandsCategory
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleRepelKey.consumeClick()) {
                MagnetTogglePacket.sendToggle();
            }
        });
    }
}
