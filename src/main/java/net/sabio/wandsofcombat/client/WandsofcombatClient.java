package net.sabio.wandsofcombat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.sabio.wandsofcombat.item.ModDataComponentTypes;
import net.sabio.wandsofcombat.item.ModItems;
import net.sabio.wandsofcombat.item.MagnetTogglePacket;
import org.lwjgl.glfw.GLFW;

public class WandsofcombatClient implements ClientModInitializer {
    private static KeyBinding toggleRepelKey;

    @Override
    public void onInitializeClient() {
        MagnetTogglePacket.initializeClient();
        String wandsCategory = "key.category.wandsofcombat.wands";
        toggleRepelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wandsofcombat.toggle_repel",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                wandsCategory
        ));
        ModelPredicateProviderRegistry.register(ModItems.MAGNET_WAND, Identifier.of("wandsofcombat", "magnet_repel_mode"),
                (stack, world, entity, seed) -> {
                    boolean isRepelling = stack.getOrDefault(ModDataComponentTypes.REPEL_MODE, false);
                    return isRepelling ? 1.0f : 0.0f;
                });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleRepelKey.wasPressed()) {
                MagnetTogglePacket.sendToggle();
            }
        });
    }
}
