package net.sabio.utilitywands.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sabio.utilitywands.Utilitywands;

public class ModItems {
    public static final RegistryKey<Item> MAGNET_WAND_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Utilitywands.MOD_ID, "magnet_wand")
    );

    public static final MagnetWandItem MAGNET_WAND = Registry.register(
            Registries.ITEM,
            MAGNET_WAND_KEY,
            new MagnetWandItem(new Item.Settings().registryKey(MAGNET_WAND_KEY))
    );

    public static final RegistryKey<ItemGroup> UTILITY_WANDS_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of(Utilitywands.MOD_ID, "utility_wands")
    );

    public static final ItemGroup UTILITY_WANDS_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            UTILITY_WANDS_GROUP_KEY,
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.utilityWands"))
                    .icon(() -> new ItemStack(MAGNET_WAND))
                    .entries((displayContext, entries) -> {
                        entries.add(MAGNET_WAND);
                    })
                    .build()
    );

    public static void initialize() {

    }
}
