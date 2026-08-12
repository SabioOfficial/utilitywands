package net.sabio.wandsofcombat.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ElectricWandItem extends Item {
    public static final int COOLDOWN_DURATION = 600; // 30 seconds
    private static final float ATTACK_DAMAGE_BONUS = 4.0f; // total atk damage: 8
    private static final float ATTACK_SPEED = -3f;
    private static final double ABILITY_RANGE = 16.0;
    private static final int LIGHTNING_COUNT = 3;
    private static final Map<UUID, Integer> hitCounters = new HashMap<>();

    public ElectricWandItem(Properties settings) {
        super(ToolMaterial.DIAMOND.applyToolProperties(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }

    public static void strikeLightningOn(Entity target, ServerLevel world) {
        LightningBolt lightning = new LightningBolt(EntityTypes.LIGHTNING_BOLT, world);
        lightning.snapTo(target.getX(), target.getY(), target.getZ());
        lightning.setVisualOnly(true);
        world.addFreshEntity(lightning);
        target.hurtServer(world, world.damageSources().lightningBolt(), 5.0f);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !attacker.level().isClientSide()) {
            UUID uuid = player.getUUID();
            int hits = hitCounters.getOrDefault(uuid, 0) + 1;
            if (hits >= 3) {
                hitCounters.put(uuid, 0);
                strikeLightningOn(target, (ServerLevel) attacker.level());
            } else {
                hitCounters.put(uuid, hits);
            }
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (!world.isClientSide()) {
            ServerLevel ServerLevel = (ServerLevel) world;
            player.getCooldowns().addCooldown(stack, COOLDOWN_DURATION);
            assert ServerLevel.getServer() != null;
            WandCooldownState.get(ServerLevel.getServer()).save(player.getUUID(), "electric", COOLDOWN_DURATION);
            AABB box = player.getBoundingBox().inflate(ABILITY_RANGE);
            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && !entity.isRemoved());
            if (!targets.isEmpty()) {
                ElectricWandLightningHandler.scheduleAbility(ServerLevel, player, targets, LIGHTNING_COUNT);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
