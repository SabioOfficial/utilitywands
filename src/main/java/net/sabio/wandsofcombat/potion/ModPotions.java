package net.sabio.wandsofcombat.potion;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.sabio.wandsofcombat.Wandsofcombat;
import net.sabio.wandsofcombat.effect.ModEffects;

public class ModPotions {
    public static final ResourceKey<Potion> MANA_REGENERATION_KEY = ResourceKey.create(
            Registries.POTION,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "mana_regeneration")
    );

    public static final Holder<Potion> MANA_REGENERATION = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            MANA_REGENERATION_KEY,
            new Potion("mana_regeneration", new MobEffectInstance(
                    ModEffects.MANA_REGENERATION,
                    3600,
                    0
            ))
    );

    public static final ResourceKey<Potion> MANA_REGENERATION_PLUS_KEY = ResourceKey.create(
            Registries.POTION,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "mana_regeneration_plus")
    );

    public static final Holder<Potion> MANA_REGENERATION_PLUS = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            MANA_REGENERATION_PLUS_KEY,
            new Potion("mana_regeneration_plus", new MobEffectInstance(
                    ModEffects.MANA_REGENERATION,
                    1800,
                    1
            ))
    );

    public static void initialize() {

    }
}