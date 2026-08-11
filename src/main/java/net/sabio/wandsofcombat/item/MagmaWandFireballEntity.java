package net.sabio.wandsofcombat.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerLevel;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MagmaWandFireballEntity extends FireballEntity {
    public MagmaWandFireballEntity(World world, LivingEntity owner, Vec3d direction) {
        super(world, owner, direction, 0);
    }

    public MagmaWandFireballEntity(EntityType<? extends FireballEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void onEntityHit(EntityHitResult hitResult) {
        super.onEntityHit(hitResult);
        if (getEntityWorld() instanceof ServerLevel ServerLevel) {
            hitResult.getEntity().damage(
                    ServerLevel,
                    ServerLevel.getDamageSources().fireball(this, getOwner()),
                    8.0f
            );
            hitResult.getEntity().setOnFireFor(5);
            ServerLevel.spawnParticles(
                    new DustParticleEffect(0xF7803D, 2.5f),
                    hitResult.getEntity().getX(),
                    hitResult.getEntity().getY() + 1.0,
                    hitResult.getEntity().getZ(),
                    16,
                    0.4,
                    0.4,
                    0.4,
                    0
            );
            ServerLevel.spawnParticles(
                    ParticleTypes.LAVA,
                    hitResult.getEntity().getX(),
                    hitResult.getEntity().getY() + 1.0,
                    hitResult.getEntity().getZ(),
                    8,
                    0.3,
                    0.3,
                    0.3,
                    0.2
            );
            ServerLevel.spawnParticles(
                    ParticleTypes.LARGE_SMOKE,
                    hitResult.getEntity().getX(),
                    hitResult.getEntity().getY() + 1.0,
                    hitResult.getEntity().getZ(),
                    5,
                    0.2,
                    0.3,
                    0.2,
                    0.02
            );
        }
    }

    @Override
    protected void onBlockHit(net.minecraft.util.hit.BlockHitResult hitResult) {
        super.onBlockHit(hitResult);
        this.discard();
    }
}