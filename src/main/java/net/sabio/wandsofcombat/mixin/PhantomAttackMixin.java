package net.sabio.wandsofcombat.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sabio.wandsofcombat.item.PhantomModeHandler;
import net.sabio.wandsofcombat.item.PhantomWandItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PhantomAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        if (!((Player) (Object) this instanceof ServerPlayer attacker)) return;
        if (!(attacker.getMainHandItem().getItem() instanceof PhantomWandItem)) return;
        if (!(target instanceof LivingEntity livingTarget)) return;
        boolean pulled = PhantomModeHandler.tryPullAttack(attacker, livingTarget);
        if (pulled) {
            ci.cancel();
        }
    }
}