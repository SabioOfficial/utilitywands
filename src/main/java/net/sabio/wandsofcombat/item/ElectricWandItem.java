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
    private static final float ATTACK_SPEED = -3f;
    private static final double ABILITY_RANGE = 16.0;
    private static final int LIGHTNING_COUNT = 3;
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
        target.damage(world, world.getDamageSources().lightningBolt(), 5.0f);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !attacker.getEntityWorld().isClient()) {
            UUID uuid = player.getUuid();
            int hits = hitCounters.getOrDefault(uuid, 0) + 1;
            if (hits >= 3) {
                hitCounters.put(uuid, 0);
                strikeLightningOn(target, (ServerWorld) attacker.getEntityWorld());
            } else {
                hitCounters.put(uuid, hits);
            }
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(stack)) return ActionResult.FAIL;
        if (!world.isClient()) {
            ServerWorld serverWorld = (ServerWorld) world;
            player.getItemCooldownManager().set(stack, COOLDOWN_DURATION);
            assert serverWorld.getServer() != null;
            WandCooldownState.get(serverWorld.getServer()).save(player.getUuid(), "electric", COOLDOWN_DURATION);
            Box box = player.getBoundingBox().expand(ABILITY_RANGE);
            List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box, entity -> entity != player && !entity.isRemoved());
            if (!targets.isEmpty()) {
                ElectricWandLightningHandler.scheduleAbility(serverWorld, player, targets, LIGHTNING_COUNT);
            }
        }
        return ActionResult.SUCCESS;
    }
}
