package net.sabio.wandsofcombat.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireballEntity;
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
            hitResult.getEntity().damage(
                    serverWorld,
                    serverWorld.getDamageSources().fireball(this, getOwner()),
                    8.0f
            );
            hitResult.getEntity().setOnFireFor(5);
        }
    }

    @Override
    protected void onBlockHit(net.minecraft.util.hit.BlockHitResult hitResult) {
        super.onBlockHit(hitResult);
        this.discard();
    }
}