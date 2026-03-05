package net.sabio.wandsofcombat.item;

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
import net.sabio.wandsofcombat.Wandsofcombat;

public class ModItems {
    public static final RegistryKey<Item> MAGNET_WAND_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Wandsofcombat.MOD_ID, "magnet_wand")
    );

    public static final MagnetWandItem MAGNET_WAND = Registry.register(
            Registries.ITEM,
            MAGNET_WAND_KEY,
            new MagnetWandItem(new Item.Settings().registryKey(MAGNET_WAND_KEY))
    );

    public static final RegistryKey<Item> ICE_WAND_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Wandsofcombat.MOD_ID, "ice_wand")
    );

    public static final IceWandItem ICE_WAND = Registry.register(
            Registries.ITEM,
            ICE_WAND_KEY,
            new IceWandItem(new Item.Settings().registryKey(ICE_WAND_KEY))
    );

    public static final RegistryKey<Item> ELECTRIC_WAND_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Wandsofcombat.MOD_ID, "electric_wand")
    );

    public static final ElectricWandItem ELECTRIC_WAND = Registry.register(
            Registries.ITEM,
            ELECTRIC_WAND_KEY,
            new ElectricWandItem(new Item.Settings().registryKey(ELECTRIC_WAND_KEY))
    );

    public static final RegistryKey<ItemGroup> WANDS_OF_COMBAT_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of(Wandsofcombat.MOD_ID, "wands_of_combat")
    );

    public static final ItemGroup WANDS_OF_COMBAT_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            WANDS_OF_COMBAT_GROUP_KEY,
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.wandsofcombat"))
                    .icon(() -> new ItemStack(MAGNET_WAND))
                    .entries((displayContext, entries) -> {
                        entries.add(MAGNET_WAND);
                        entries.add(ICE_WAND);
                        entries.add(ELECTRIC_WAND);
                    })
                    .build()
    );

    public static void initialize() {

    }
}
