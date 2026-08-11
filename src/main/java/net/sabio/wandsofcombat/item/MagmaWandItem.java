package net.sabio.wandsofcombat.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.Player;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerLevel;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MagmaWandItem extends Item {
    private static final float ATTACK_DAMAGE_BONUS = 5.0f; // total: 9
    private static final float ATTACK_SPEED = -3.231f; // roughly 1.3s
    public static final int ABILITY_COOLDOWN = 1200; // 1 min
    public static final int ULTIMATE_COOLDOWN = 1800; // 1:30mins; yeah we having these now
    public static final int ABILITY_DURATION = 900; // 45 seconds
    public static final int ULTIMATE_DURATION = 600; // 30 seconds
    public static final Map<UUID, Integer> hitCounters = new HashMap<>();
    public MagmaWandItem(Settings settings) {
        super(ToolMaterial.DIAMOND.applyToolSettings(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }
    @Override
    public ActionResult use(World world, Player player, Hand hand) {
        if (hand == Hand.OFF_HAND && player.getMainHandStack().getItem() instanceof MagmaWandItem) {
            return ActionResult.PASS;
        }
        if (!world.isClient()) {
            if (player.isSneaking()) {
                MagmaWandHandler.tryActivateUltimate(player);
            } else {
                MagmaWandHandler.tryActivateAbility(player);
            }
        }
        return ActionResult.SUCCESS;
    }
    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !attacker.getEntityWorld().isClient()) {
            UUID uuid = player.getUUID();
            int hits = hitCounters.getOrDefault(uuid, 0) + 1;
            if (hits >= 5) {
                hitCounters.put(uuid, 0);
                if (attacker.getEntityWorld() instanceof ServerLevel ServerLevel) {
                    ServerLevel.spawnParticles(
                            new DustParticleEffect(0xDC4810, 2.0f),
                            attacker.getX(),
                            attacker.getY() + 1.0,
                            attacker.getZ(),
                            10,
                            0.3,
                            0.3,
                            0.3,
                            0
                    );
                    ServerLevel.spawnParticles(
                            ParticleTypes.FLAME,
                            attacker.getX(),
                            attacker.getY() + 1.0,
                            attacker.getZ(),
                            8,
                            0.2,
                            0.3,
                            0.2,
                            0.05
                    );
                }
                MagmaWandHandler.launchFireball(player, target);
            } else {
                hitCounters.put(uuid, hits);
            }
        }
        super.postHit(stack, target, attacker);
    }
}
