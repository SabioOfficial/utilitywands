package net.sabio.wandsofcombat.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PhantomWandItem extends Item {
    public static final int ABILITY_DURATION = 120; // 6 seconds
    private static final float ATTACK_DAMAGE_BONUS = 2.0f; // 6 total attack damage
    private static final float ATTACK_SPEED = -1.5f;
    public static final Set<UUID> phantomPlayers = new HashSet<>();
    public static final Set<UUID> pullingPlayers = new HashSet<>();
    public PhantomWandItem(Properties settings) {
        super(ToolMaterial.DIAMOND.applyToolProperties(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }
    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (!ManaManager.hasEnough(player, ManaCosts.PHANTOM_WAND)) {
            return InteractionResult.FAIL;
        }
        if (!world.isClientSide()) {
            if (!ManaManager.tryConsume(player, ManaCosts.PHANTOM_WAND)) {
                return InteractionResult.FAIL;
            }
            PhantomModeHandler.activatePhantomMode(player, (ServerLevel) world);
        }

        return InteractionResult.SUCCESS;
    }
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
    }
}
