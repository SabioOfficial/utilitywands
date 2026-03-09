package net.sabio.wandsofcombat.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MagnetWandItem extends Item {
    public static final int COOLDOWN = 600; // 30 seconds (in ticks)
    private static final float ATTACK_DAMAGE_BONUS = 3.0f;
    private static final float ATTACK_SPEED = -2.4f;
    private static final double ABILITY_PULL_RANGE = 12.0;
    private static final double REPEL_RANGE = 8.0;
    private static final float REPEL_DAMAGE = 6.0f;

    public MagnetWandItem(Settings settings) {
        super(ToolMaterial.DIAMOND.applyToolSettings(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !attacker.getEntityWorld().isClient()) {
            MagnetPullManager.recordHit(player, target);
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (hand == Hand.OFF_HAND) return ActionResult.PASS;
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(stack)) return ActionResult.FAIL;
        if (!world.isClient()) {
            player.getItemCooldownManager().set(stack, COOLDOWN);
            if (MagnetPullManager.isRepelMode(player)) {
                MagnetPullManager.doRepel(player, REPEL_RANGE, REPEL_DAMAGE);
            } else {
                MagnetPullManager.doAbilityPull(player, ABILITY_PULL_RANGE);
            }
        }
        return ActionResult.SUCCESS;
    }
}
