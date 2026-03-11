package net.sabio.wandsofcombat.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
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
        if (getEntityWorld() instanceof ServerWorld serverWorld) {
            hitResult.getEntity().damage(serverWorld.getDamageSources().fireball(this, getOwner()), 8.0f);
            hitResult.getEntity().setOnFireFor(5);
            serverWorld.spawnParticles(
                    new DustParticleEffect(new org.joml.Vector3f(0xF7/255f, 0x80/255f, 0x3D/255f), 2.5f),
                    hitResult.getEntity().getX(),
                    hitResult.getEntity().getY() + 1.0,
                    hitResult.getEntity().getZ(),
                    16,
                    0.4,
                    0.4,
                    0.4,
                    0
            );
            serverWorld.spawnParticles(
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
            serverWorld.spawnParticles(
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