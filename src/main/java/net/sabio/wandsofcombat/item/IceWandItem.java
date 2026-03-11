package net.sabio.wandsofcombat.item;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;

import java.util.List;

public class IceWandItem extends SwordItem {
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

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
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
                serverWorld.spawnParticles(
                        new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
                        mob.getX(),
                        mob.getY() + 0.5,
                        mob.getZ(),
                        15,
                        0.4,
                        0.4,
                        0.4,
                        0.1
                );
                serverWorld.spawnParticles(
                        new DustParticleEffect(new org.joml.Vector3f(0x80/255f, 0xD9/255f, 0xFF/255f), 2.0f),
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
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
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
                serverWorld.spawnParticles(
                        new DustParticleEffect(new org.joml.Vector3f(0x80/255f, 0xD9/255f, 0xFF/255f), 2.0f),
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

        if (world instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 24; i++) {
                double angle = (2.0 * Math.PI / 24) * i;
                double range = hitPlayer ? FULL_RANGE : LITE_RANGE;
                double posX = player.getX() + range * Math.cos(angle);
                double posZ = player.getZ() + range * Math.sin(range);
                serverWorld.spawnParticles(
                        new DustParticleEffect(new org.joml.Vector3f(0x80/255f, 0xD9/255f, 0xFF/255f), 1.5f),
                        posX,
                        player.getY() + 0.5,
                        posZ,
                        2,
                        0,
                        0.3,
                        0,
                        0
                );
                serverWorld.spawnParticles(
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

    public IceWandItem(Settings settings) {
        super(ToolMaterials.DIAMOND, settings.attributeModifiers(
                SwordItem.createAttributeModifiers(ToolMaterials.DIAMOND, (int) ATTACK_DAMAGE_BONUS, ATTACK_SPEED)
        ));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(player.getStackInHand(hand));
        }
        if (!world.isClient()) {
            boolean hitPlayer = applyAbility(world, player);
            int cooldown = hitPlayer ? FULL_COOLDOWN : LITE_COOLDOWN;
            player.getItemCooldownManager().set(this, cooldown);
            assert ((ServerWorld) world).getServer() != null;
            WandCooldownState.get(((ServerWorld)world).getServer()).save(player.getUuid(), "ice", cooldown);
        }

        return TypedActionResult.success(player.getStackInHand(hand));
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !attacker.getWorld().isClient()) {
            IceWandComboHandler.onHit(player, target);
        }
        return super.postHit(stack, target, attacker);
    }
}
