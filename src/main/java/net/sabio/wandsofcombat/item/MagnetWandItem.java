package net.sabio.wandsofcombat.item;

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

public class MagnetWandItem extends Item {
    private static final float ATTACK_DAMAGE_BONUS = 3.0f;
    private static final float ATTACK_SPEED = -2.4f;
    private static final double ABILITY_PULL_RANGE = 12.0;
    private static final double REPEL_RANGE = 8.0;
    private static final float REPEL_DAMAGE = 6.0f;

    public MagnetWandItem(Properties settings) {
        super(ToolMaterial.DIAMOND.applyToolProperties(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !attacker.level().isClientSide()) {
            MagnetPullManager.recordHit(player, target);
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!ManaManager.hasEnough(player, ManaCosts.MAGNET_WAND)) return InteractionResult.FAIL;
        if (!world.isClientSide()) {
            if (!ManaManager.tryConsume(player, ManaCosts.MAGNET_WAND)) return InteractionResult.FAIL;
            if (MagnetPullManager.isRepelMode(player)) {
                MagnetPullManager.doRepel(player, REPEL_RANGE, REPEL_DAMAGE);
            } else {
                MagnetPullManager.doAbilityPull(player, ABILITY_PULL_RANGE);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
