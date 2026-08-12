package net.sabio.wandsofcombat.item;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
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
            new MagnetWandItem(new Item.Properties().registryKey(MAGNET_WAND_KEY))
    );

    public static final RegistryKey<Item> ICE_WAND_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Wandsofcombat.MOD_ID, "ice_wand")
    );

    public static final IceWandItem ICE_WAND = Registry.register(
            Registries.ITEM,
            ICE_WAND_KEY,
            new IceWandItem(new Item.Properties().registryKey(ICE_WAND_KEY))
    );

    public static final RegistryKey<Item> ELECTRIC_WAND_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Wandsofcombat.MOD_ID, "electric_wand")
    );

    public static final ElectricWandItem ELECTRIC_WAND = Registry.register(
            Registries.ITEM,
            ELECTRIC_WAND_KEY,
            new ElectricWandItem(new Item.Properties().registryKey(ELECTRIC_WAND_KEY))
    );

    public static final RegistryKey<Item> PHANTOM_WAND_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Wandsofcombat.MOD_ID, "phantom_wand")
    );

    public static final PhantomWandItem PHANTOM_WAND = Registry.register(
            Registries.ITEM,
            PHANTOM_WAND_KEY,
            new PhantomWandItem(new Item.Properties().registryKey(PHANTOM_WAND_KEY))
    );

    public static final RegistryKey<Item> MAGMA_WAND_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Wandsofcombat.MOD_ID, "magma_wand")
    );

    public static final MagmaWandItem MAGMA_WAND = Registry.register(
            Registries.ITEM,
            MAGMA_WAND_KEY,
            new MagmaWandItem(new Item.Properties().registryKey(MAGMA_WAND_KEY))
    );

    public static final RegistryKey<Item> MAGICAL_STICK_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Wandsofcombat.MOD_ID, "magical_stick")
    );

    public static final Item MAGICAL_STICK = Registry.register(
            Registries.ITEM,
            MAGICAL_STICK_KEY,
            new Item(new Item.Properties().registryKey(MAGICAL_STICK_KEY))
    );

    public static final ComponentType<Boolean> MAGNET_REPEL_MODE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Wandsofcombat.MOD_ID, "magnet_repel_mode"),
            ComponentType.<Boolean>builder()
                    .codec(Codec.BOOL)
                    .packetCodec(PacketCodecs.BOOLEAN)
                    .build()
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
                        entries.add(MAGICAL_STICK);
                        entries.add(MAGNET_WAND);
                        entries.add(ICE_WAND);
                        entries.add(ELECTRIC_WAND);
                        entries.add(PHANTOM_WAND);
                        entries.add(MAGMA_WAND);
                    })
                    .build()
    );

    public static void initialize() {

    }
}
