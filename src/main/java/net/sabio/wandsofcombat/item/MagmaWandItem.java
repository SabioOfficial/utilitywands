package net.sabio.wandsofcombat.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
    public MagmaWandItem(Properties settings) {
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
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof MagmaWandItem) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide()) {
            if (player.isCrouching()) {
                MagmaWandHandler.tryActivateUltimate(player);
            } else {
                MagmaWandHandler.tryActivateAbility(player);
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !attacker.level().isClientSide()) {
            UUID uuid = player.getUUID();
            int hits = hitCounters.getOrDefault(uuid, 0) + 1;
            if (hits >= 5) {
                hitCounters.put(uuid, 0);
                if (attacker.level() instanceof ServerLevel ServerLevel) {
                    ServerLevel.sendParticles(
                            new DustParticleOptions(0xDC4810, 2.0f),
                            attacker.getX(),
                            attacker.getY() + 1.0,
                            attacker.getZ(),
                            10,
                            0.3,
                            0.3,
                            0.3,
                            0
                    );
                    ServerLevel.sendParticles(
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
        super.postHurtEnemy(stack, target, attacker);
    }
}
