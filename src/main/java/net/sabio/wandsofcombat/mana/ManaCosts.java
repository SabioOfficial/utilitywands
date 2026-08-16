package net.sabio.wandsofcombat.mana;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sabio.wandsofcombat.item.*;

public final class ManaCosts {
    private ManaCosts() {}

    public static final int MAGNET_WAND = 3;
    public static final int ICE_WAND_LITE = 3;
    public static final int ICE_WAND_FULL = 4;
    public static final int ELECTRIC_WAND = 3;
    public static final int PHANTOM_WAND = 5;
    public static final int MAGMA_ABILITY = 3;
    public static final int MAGMA_ULTIMATE = 8;

    public static int nextCastCost(Player player) {
        ItemStack held = player.getMainHandItem();
        Item item = held.getItem();
        return switch (item) {
            case MagmaWandItem magmaWandItem -> player.isCrouching() ? MAGMA_ULTIMATE : MAGMA_ABILITY;
            case IceWandItem iceWandItem -> ICE_WAND_LITE;
            case ElectricWandItem electricWandItem -> ELECTRIC_WAND;
            case MagnetWandItem magnetWandItem -> MAGNET_WAND;
            case PhantomWandItem phantomWandItem -> PHANTOM_WAND;
            default -> -1;
        };
    }
}
