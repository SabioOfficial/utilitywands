package net.sabio.wandsofcombat.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sabio.wandsofcombat.item.PhantomModeHandler;
import net.sabio.wandsofcombat.item.PhantomWandItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class PhantomAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        ServerPlayerEntity attacker = (ServerPlayerEntity) (Object) this;
        if (!(attacker.getMainHandStack().getItem() instanceof PhantomWandItem)) return;
        if (!(target instanceof LivingEntity livingTarget)) return;
        boolean pulled = PhantomModeHandler.tryPullAttack(attacker, livingTarget);
        if (pulled) {
            ci.cancel();
        }
    }
}