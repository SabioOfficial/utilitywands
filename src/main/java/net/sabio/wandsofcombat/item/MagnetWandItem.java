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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;

import java.util.ArrayList;
import java.util.List;

public class MagnetWandItem extends SwordItem {
    public static final int COOLDOWN = 600; // 30 seconds (in ticks)
    private static final float ATTACK_DAMAGE_BONUS = 3.0f;
    private static final float ATTACK_SPEED = -2.4f;
    private static final double ABILITY_PULL_RANGE = 12.0;
    private static final double REPEL_RANGE = 8.0;
    private static final float REPEL_DAMAGE = 6.0f;

    public MagnetWandItem(Settings settings) {
        super(ToolMaterials.DIAMOND, settings.attributeModifiers(
                SwordItem.createAttributeModifiers(ToolMaterials.DIAMOND, (int) ATTACK_DAMAGE_BONUS, ATTACK_SPEED)
        ));
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !attacker.getWorld().isClient()) {
            MagnetPullManager.recordHit(player, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (hand == Hand.OFF_HAND) return TypedActionResult.pass(player.getStackInHand(hand));
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(player.getStackInHand(hand));
        if (!world.isClient()) {
            player.getItemCooldownManager().set(this, COOLDOWN);
            assert ((ServerWorld) world).getServer() != null;
            WandCooldownState.get(((ServerWorld)world).getServer()).save(player.getUuid(), "magnet", COOLDOWN);
            if (MagnetPullManager.isRepelMode(player)) {
                MagnetPullManager.doRepel(player, REPEL_RANGE, REPEL_DAMAGE);
            } else {
                MagnetPullManager.doAbilityPull(player, ABILITY_PULL_RANGE);
            }
        }
        return TypedActionResult.success(player.getStackInHand(hand));
    }
}
