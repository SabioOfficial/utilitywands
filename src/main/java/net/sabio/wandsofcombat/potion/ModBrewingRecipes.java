package net.sabio.wandsofcombat.potion;

import net.fabricmc.fabric.api.registry.FabricPotionBrewingBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.sabio.wandsofcombat.item.ModItems;

public final class ModBrewingRecipes {
    private ModBrewingRecipes() {}

    public static void initialize() {
        FabricPotionBrewingBuilder.BUILD.register(builder -> {
            builder.addMix(Potions.AWKWARD, ModItems.MAGICAL_STICK, ModPotions.MANA_REGENERATION);
            builder.addMix(ModPotions.MANA_REGENERATION, Items.GLOWSTONE_DUST, ModPotions.MANA_REGENERATION_PLUS);
        });
    }
}
