package net.sabio.wandsofcombat.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ElectricWandItem extends Item {
    public static final int COOLDOWN_DURATION = 600; // 30 seconds
    private static final float ATTACK_DAMAGE_BONUS = 4.0f; // total atk damage: 8
    private static final float ATTACK_SPEED = -3.259f; // 1.35s charge time
    private static final double ABILITY_RANGE_LARGE = 16.0;
    private static final double ABILITY_RANGE_SMALL = 10.0;
    private static final int LIGHTNING_BURST_COUNT = 5;
    private static final int LIGHTNING_FOLLOWUP_COUNT = 7;
    private static final float FOLLOWUP_DAMAGE_THRESHOLD = 6.0f; // if combined damage from the 5 initial strikes is less than 3 hearts (6 hp), trigger a follow-up attack
    private static final int STRIKE_INTERVAL = 10; // ticks between each lightning strike
    private static final Map<UUID, Integer> hitCounters = new HashMap<>();

    public ElectricWandItem(Settings settings) {
        super(ToolMaterial.DIAMOND.applyToolSettings(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }

    public static void strikeLightningOn(Entity target, ServerWorld world) {
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        lightning.setPosition(target.getX(), target.getY(), target.getZ());
        lightning.setCosmetic(true);
        world.spawnEntity(lightning);
        if (target instanceof LivingEntity livingEntity) {
            livingEntity.damage(world, world.getDamageSources().lightningBolt(), 5.0f);
        }
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !attacker.getEntityWorld().isClient()) {
            UUID playerId = player.getUuid();
            int hits = hitCounters.getOrDefault(playerId, 0) + 1;
            if (hits >= 3) {
                hitCounters.put(playerId, 0);
                strikeLightningOn(target, (ServerWorld) attacker.getEntityWorld());
            } else {
                hitCounters.put(playerId, hits);
            }
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.FAIL;
        }
        if (!world.isClient()) {
            ServerWorld serverWorld = (ServerWorld) world;
            player.getItemCooldownManager().set(stack, COOLDOWN_DURATION);
            Box largeBox = player.getBoundingBox().expand(ABILITY_RANGE_LARGE);
            List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, largeBox, entity -> entity != player && !entity.isRemoved());
            if (targets.isEmpty()) return ActionResult.SUCCESS;
            Map<LivingEntity, Float> healthBefore = new HashMap<>();
            for (LivingEntity target : targets) {
                healthBefore.put(target, target.getHealth());
            }
            ElectricWandLightningHandler.scheduleBurst(
                    serverWorld,
                    player,
                    targets,
                    healthBefore,
                    LIGHTNING_BURST_COUNT,
                    LIGHTNING_FOLLOWUP_COUNT,
                    FOLLOWUP_DAMAGE_THRESHOLD,
                    ABILITY_RANGE_SMALL,
                    STRIKE_INTERVAL
            );
        }
        return ActionResult.SUCCESS;
    }
}
