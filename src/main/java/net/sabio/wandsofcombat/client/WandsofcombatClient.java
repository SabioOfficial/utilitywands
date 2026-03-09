package net.sabio.wandsofcombat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.sabio.wandsofcombat.Wandsofcombat;
import net.sabio.wandsofcombat.item.MagnetTogglePacket;
import org.lwjgl.glfw.GLFW;

public class WandsofcombatClient implements ClientModInitializer {
    private static KeyBinding toggleRepelKey;

    @Override
    public void onInitializeClient() {
        MagnetTogglePacket.initializeClient();
        KeyBinding.Category wandsCategory;
        try {
            wandsCategory = KeyBinding.Category.create(Identifier.of(Wandsofcombat.MOD_ID, "wands"));
        } catch (IllegalArgumentException error) {
            wandsCategory = KeyBinding.Category.MISC;
        }
        toggleRepelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wandsofcombat.toggle_repel",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                wandsCategory
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleRepelKey.wasPressed()) {
                MagnetTogglePacket.sendToggle();
            }
        });
    }
}
