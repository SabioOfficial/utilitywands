package net.sabio.wandsofcombat.item;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sabio.wandsofcombat.Wandsofcombat;

public class ModItems {
    public static final ResourceKey<Item> MAGNET_WAND_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "magnet_wand")
    );

    public static final MagnetWandItem MAGNET_WAND = Registry.register(
            BuiltInRegistries.ITEM,
            MAGNET_WAND_KEY,
            new MagnetWandItem(new Item.Properties().setId(MAGNET_WAND_KEY))
    );

    public static final ResourceKey<Item> ICE_WAND_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "ice_wand")
    );

    public static final IceWandItem ICE_WAND = Registry.register(
            BuiltInRegistries.ITEM,
            ICE_WAND_KEY,
            new IceWandItem(new Item.Properties().setId(ICE_WAND_KEY))
    );

    public static final ResourceKey<Item> ELECTRIC_WAND_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "electric_wand")
    );

    public static final ElectricWandItem ELECTRIC_WAND = Registry.register(
            BuiltInRegistries.ITEM,
            ELECTRIC_WAND_KEY,
            new ElectricWandItem(new Item.Properties().setId(ELECTRIC_WAND_KEY))
    );

    public static final ResourceKey<Item> PHANTOM_WAND_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "phantom_wand")
    );

    public static final PhantomWandItem PHANTOM_WAND = Registry.register(
            BuiltInRegistries.ITEM,
            PHANTOM_WAND_KEY,
            new PhantomWandItem(new Item.Properties().setId(PHANTOM_WAND_KEY))
    );

    public static final ResourceKey<Item> MAGMA_WAND_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "magma_wand")
    );

    public static final MagmaWandItem MAGMA_WAND = Registry.register(
            BuiltInRegistries.ITEM,
            MAGMA_WAND_KEY,
            new MagmaWandItem(new Item.Properties().setId(MAGMA_WAND_KEY))
    );

    public static final ResourceKey<Item> MAGICAL_STICK_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "magical_stick")
    );

    public static final Item MAGICAL_STICK = Registry.register(
            BuiltInRegistries.ITEM,
            MAGICAL_STICK_KEY,
            new Item(new Item.Properties().setId(MAGICAL_STICK_KEY))
    );

    public static final DataComponentType<Boolean> MAGNET_REPEL_MODE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "magnet_repel_mode"),
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
    );

    public static final ResourceKey<CreativeModeTab> WANDS_OF_COMBAT_GROUP_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "wands_of_combat")
    );

    public static final CreativeModeTab WANDS_OF_COMBAT_GROUP = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            WANDS_OF_COMBAT_GROUP_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wandsofcombat"))
                    .icon(() -> new ItemStack(MAGNET_WAND))
                    .displayItems((displayContext, entries) -> {
                        entries.accept(MAGICAL_STICK);
                        entries.accept(MAGNET_WAND);
                        entries.accept(ICE_WAND);
                        entries.accept(ELECTRIC_WAND);
                        entries.accept(PHANTOM_WAND);
                        entries.accept(MAGMA_WAND);
                    })
                    .build()
    );

    public static void initialize() {

    }
}
