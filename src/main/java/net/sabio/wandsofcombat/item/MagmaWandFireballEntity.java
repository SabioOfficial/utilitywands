package net.sabio.wandsofcombat.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class MagmaWandFireballEntity extends Fireball {
    private boolean orbiting = false;

    public MagmaWandFireballEntity(Level world, LivingEntity owner, Vec3 direction) {
        super(EntityTypes.FIREBALL, owner, direction, world);
    }

    public MagmaWandFireballEntity(EntityType<? extends Fireball> type, Level world) {
        super(type, world);
    }

    public void setOrbiting(boolean orbiting) {
        this.orbiting = orbiting;
        this.setNoGravity(true);
    }

    public void launchOutward(Vec3 direction, double power) {
        this.orbiting = false;
        this.setNoGravity(false);
        try {
            var xf = Fireball.class.getDeclaredField("xPower");
            var yf = Fireball.class.getDeclaredField("yPower");
            var zf = Fireball.class.getDeclaredField("zPower");
            xf.setAccessible(true);
            yf.setAccessible(true);
            zf.setAccessible(true);
            xf.setDouble(this, direction.x * power);
            yf.setDouble(this, direction.y * power);
            zf.setDouble(this, direction.z * power);
        } catch (ReflectiveOperationException e) {
            this.setDeltaMovement(direction.scale(power));
        }
    }

    @Override
    public void tick() {
        if (orbiting) {
            this.setDeltaMovement(Vec3.ZERO);
            this.tickCount++;
            return;
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (orbiting) return;
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
        if (orbiting) return;
        super.onHitBlock(hitResult);
        this.discard();
    }
}