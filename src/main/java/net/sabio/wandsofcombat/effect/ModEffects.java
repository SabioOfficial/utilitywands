package net.sabio.wandsofcombat.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.sabio.wandsofcombat.Wandsofcombat;

public class ModEffects {
    public static final Holder<MobEffect> MANA_REGENERATION = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "mana_regeneration"),
            new ManaRegenerationEffect()
    );

    public static void initialize() {

    }
}
