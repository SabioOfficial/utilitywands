package net.sabio.wandsofcombat.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class MagmaWandFireballEntity extends Fireball {
    public MagmaWandFireballEntity(Level world, LivingEntity owner, Vec3 direction) {
        super(EntityType.FIREBALL, owner, direction, world);
    }

    public MagmaWandFireballEntity(EntityType<? extends Fireball> type, Level world) {
        super(type, world);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (level() instanceof ServerLevel ServerLevel) {
            hitResult.getEntity().hurtServer(
                    ServerLevel,
                    ServerLevel.damageSources().fireball(this, getOwner()),
                    8.0f
            );
            hitResult.getEntity().igniteForSeconds(5);
            ServerLevel.sendParticles(
                    new DustParticleOptions(0xF7803D, 2.5f),
                    hitResult.getEntity().getX(),
                    hitResult.getEntity().getY() + 1.0,
                    hitResult.getEntity().getZ(),
                    16,
                    0.4,
                    0.4,
                    0.4,
                    0
            );
            ServerLevel.sendParticles(
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
            ServerLevel.sendParticles(
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
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        this.discard();
    }
}