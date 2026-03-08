package net.sabio.wandsofcombat.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class IceWandItem extends Item {
    public static final int LITE_COOLDOWN = 400; // 20 seconds; lite = mobs only affected ability
    public static final int FULL_COOLDOWN = 600; // 30 seconds; full = mobs + players affected ability
    private static final double LITE_RANGE = 8.0;
    private static final double FULL_RANGE = 12.0;
    private static final int FREEZE_DURATION = 80; // 4 seconds
    private static final int POWDER_SNOW_FREEZE_DURATION = 300; // 15 seconds; power snow visuals kick in at >140 ticks
    private static final int LITE_SLOWNESS_AMPLIFIER = 126; // slowness 127
    private static final int FULL_SLOWNESS_AMPLIFIER = 1; // slowness 2
    private static final float ATTACK_SPEED = -3.3f;
    private static final float ATTACK_DAMAGE_BONUS = 8.0f; // 12 total attack damage

    private boolean applyAbility(World world, PlayerEntity player) {
        boolean hitPlayer = false;
        Box mobBox = player.getBoundingBox().expand(LITE_RANGE);
        List<LivingEntity> nearbyMobs = world.getEntitiesByClass(LivingEntity.class, mobBox, entity -> !(entity instanceof PlayerEntity) && !entity.isRemoved());
        for (LivingEntity mob : nearbyMobs) {
            mob.setFrozenTicks(POWDER_SNOW_FREEZE_DURATION);
            mob.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS,
                    FREEZE_DURATION,
                    LITE_SLOWNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            mob.setVelocity(Vec3d.ZERO);
            mob.velocityDirty = true;
        }
        Box playerBox = player.getBoundingBox().expand(FULL_RANGE);
        List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                PlayerEntity.class,
                playerBox,
                entity -> entity != player && !entity.isRemoved()
        );
        for (PlayerEntity target : nearbyPlayers) {
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS,
                    120,
                    FULL_SLOWNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            target.setFrozenTicks(POWDER_SNOW_FREEZE_DURATION);
            hitPlayer = true;
        }

        return hitPlayer;
    }

    public IceWandItem(Settings settings) {
        super(ToolMaterial.DIAMOND.applyToolSettings(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.FAIL;
        }
        if (!world.isClient()) {
            boolean hitPlayer = applyAbility(world, player);
            int cooldown = hitPlayer ? FULL_COOLDOWN : LITE_COOLDOWN;
            player.getItemCooldownManager().set(stack, cooldown);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !attacker.getEntityWorld().isClient()) {
            IceWandComboHandler.onHit(player, target);
        }
        super.postHit(stack, target, attacker);
    }
}
