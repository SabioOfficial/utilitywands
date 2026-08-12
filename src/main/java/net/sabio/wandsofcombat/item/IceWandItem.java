package net.sabio.wandsofcombat.item;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;

import java.util.List;

public class IceWandItem extends Item {
    private static final double LITE_RANGE = 8.0;
    private static final double FULL_RANGE = 12.0;
    private static final int FREEZE_DURATION = 80; // 4 seconds
    private static final int POWDER_SNOW_FREEZE_DURATION = 300; // 15 seconds; power snow visuals kick in at >140 ticks
    private static final int LITE_SLOWNESS_AMPLIFIER = 126; // slowness 127
    private static final int FULL_SLOWNESS_AMPLIFIER = 1; // slowness 2
    private static final float ATTACK_SPEED = -3.3f;
    private static final float ATTACK_DAMAGE_BONUS = 8.0f; // 12 total attack damage

    private boolean applyAbility(Level world, Player player) {
        boolean hitPlayer = false;
        AABB mobBox = player.getBoundingBox().inflate(LITE_RANGE);
        List<LivingEntity> nearbyMobs = world.getEntitiesOfClass(LivingEntity.class, mobBox, entity -> !(entity instanceof Player) && !entity.isRemoved());
        for (LivingEntity mob : nearbyMobs) {
            mob.setTicksFrozen(POWDER_SNOW_FREEZE_DURATION);
            mob.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    FREEZE_DURATION,
                    LITE_SLOWNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            mob.setDeltaMovement(Vec3.ZERO);
            mob.hurtMarked = true;

            if (world instanceof ServerLevel ServerLevel) {
                ServerLevel.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        mob.getX(),
                        mob.getY() + 1.0,
                        mob.getZ(),
                        20,
                        0.4,
                        0.6,
                        0.4,
                        0.05
                );
                ServerLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                        mob.getX(),
                        mob.getY() + 0.5,
                        mob.getZ(),
                        15,
                        0.4,
                        0.4,
                        0.4,
                        0.1
                );
                ServerLevel.sendParticles(
                        new DustParticleOptions(0x80D9FF, 2.0f),
                        mob.getX(),
                        mob.getY() + 1.0,
                        mob.getZ(),
                        10,
                        0.3,
                        0.5,
                        0.3,
                        0
                );
            }
        }
        AABB playerBox = player.getBoundingBox().inflate(FULL_RANGE);
        List<Player> nearbyPlayers = world.getEntitiesOfClass(
                Player.class,
                playerBox,
                entity -> entity != player && !entity.isRemoved()
        );
        for (Player target : nearbyPlayers) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    120,
                    FULL_SLOWNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            target.setTicksFrozen(POWDER_SNOW_FREEZE_DURATION);
            hitPlayer = true;
            if (world instanceof ServerLevel ServerLevel) {
                ServerLevel.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        target.getX(),
                        target.getY() + 1.0,
                        target.getZ(),
                        20,
                        0.4,
                        0.6,
                        0.4,
                        0.05
                );
                ServerLevel.sendParticles(
                        new DustParticleOptions(0x80D9FF, 2.0f),
                        target.getX(),
                        target.getY() + 1.0,
                        target.getZ(),
                        10,
                        0.3,
                        0.5,
                        0.3,
                        0
                );
            }
        }

        if (world instanceof ServerLevel ServerLevel) {
            for (int i = 0; i < 24; i++) {
                double angle = (2.0 * Math.PI / 24) * i;
                double range = hitPlayer ? FULL_RANGE : LITE_RANGE;
                double posX = player.getX() + range * Math.cos(angle);
                double posZ = player.getZ() + range * Math.sin(angle);
                ServerLevel.sendParticles(
                        new DustParticleOptions(0x80D9FF, 1.5f),
                        posX,
                        player.getY() + 0.5,
                        posZ,
                        2,
                        0,
                        0.3,
                        0,
                        0
                );
                ServerLevel.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        posX,
                        player.getY() + 0.5,
                        posZ,
                        1,
                        0,
                        0.2,
                        0,
                        0.01
                );
            }
        }

        return hitPlayer;
    }

    public IceWandItem(Properties settings) {
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
        if (!ManaManager.hasEnough(player, ManaCosts.ICE_WAND_LITE)) {
            return InteractionResult.FAIL;
        }
        if (!world.isClientSide()) {
            if (!ManaManager.tryConsume(player, ManaCosts.ICE_WAND_LITE)) {
                return InteractionResult.FAIL;
            }
            boolean hitPlayer = applyAbility(world, player);
            if (hitPlayer) {
                int extraCost = ManaCosts.ICE_WAND_FULL - ManaCosts.ICE_WAND_LITE;
                ManaManager.tryConsume(player, extraCost);
            }
        }

        return InteractionResult.SUCCESS;
    }


    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !attacker.level().isClientSide()) {
            IceWandComboHandler.onHit(player, target);
        }
        super.postHurtEnemy(stack, target, attacker);
    }
}
