package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sabio.wandsofcombat.Wandsofcombat;
import net.sabio.wandsofcombat.network.PhantomSyncPacket;

import java.util.*;

public class PhantomModeHandler {
    private static final Map<UUID, Long> phantomEndTimes = new HashMap<>();
    private static final Map<UUID, Long> pullImmunityEndTimes = new HashMap<>();
    private static final Set<UUID> applyingReducedDamage = new HashSet<>();
    private static final int PHANTOM_DURATION = 200; // 10 seconds
    private static final double PULL_RANGE = 5.0;
    private static final double MELEE_RANGE = 3.0;
    private static final double PULL_SPEED = 1.2;
    private static final int PULL_IMMUNITY_DURATION = 10; // immune for 10 ticks after pull
    private static final Identifier REACH_MODIFIER_ID = Identifier.of(Wandsofcombat.MOD_ID, "phantom_wand_reach");
    private static void applyPhantomEffects(PlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.INVISIBILITY,
                PHANTOM_DURATION,
                0,
                false,
                false,
                false
        ));
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                PHANTOM_DURATION,
                0,
                false,
                false,
                false
        ));
    }
    private static void removePhantomEffects(PlayerEntity player) {
        player.removeStatusEffect(StatusEffects.INVISIBILITY);
        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().allowFlying = false;
            player.sendAbilitiesUpdate();
        }
        player.noClip = false;
        PhantomWandItem.phantomPlayers.remove(player.getUuid());
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new PhantomSyncPacket(false));
        }
    }
    private static void manageReachAttribute(PlayerEntity player, boolean holding) {
        var reachAttribute = player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
        if (reachAttribute == null) return;
        reachAttribute.removeModifier(REACH_MODIFIER_ID);
        if (holding) {
            reachAttribute.addTemporaryModifier(new EntityAttributeModifier(
                    REACH_MODIFIER_ID,
                    2.0,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }
    }
    private static void onTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            long currentTick = world.getTime();
            for (PlayerEntity player : world.getPlayers()) {
                UUID uuid = player.getUuid();
                boolean holdingPhantomWand = player.getMainHandStack().getItem() instanceof PhantomWandItem;
                manageReachAttribute(player, holdingPhantomWand);
                if (!PhantomWandItem.phantomPlayers.contains(uuid)) continue;
                Long endTick = phantomEndTimes.get(uuid);
                if (endTick == null || currentTick >= endTick) {
                    removePhantomEffects(player);
                    phantomEndTimes.remove(uuid);
                    continue;
                }
                player.noClip = true;
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().allowFlying = true;
                    player.getAbilities().flying = true;
                    player.sendAbilitiesUpdate();
                }
            }
        }
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PhantomModeHandler::onTick);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof PlayerEntity player) {
                Long immunityEnd = pullImmunityEndTimes.get(player.getUuid());
                if (immunityEnd != null && entity.getEntityWorld().getTime() <= immunityEnd) {
                    return false;
                }
            }
            if (source.getAttacker() instanceof PlayerEntity attacker) {
                UUID attackerId = attacker.getUuid();
                if (PhantomWandItem.phantomPlayers.contains(attackerId) && !applyingReducedDamage.contains(attackerId) && attacker.getMainHandStack().getItem() instanceof PhantomWandItem) {
                    applyingReducedDamage.add(attackerId);
                    entity.damage((ServerWorld) attacker.getEntityWorld(), source, amount * 0.2f);
                    applyingReducedDamage.remove(attackerId);
                    return false;
                }
            }
            return true;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            PhantomWandItem.phantomPlayers.remove(uuid);
            PhantomWandItem.pullingPlayers.remove(uuid);
            phantomEndTimes.remove(uuid);
            pullImmunityEndTimes.remove(uuid);
            handler.player.noClip = false;
            if (!handler.player.isCreative() && !handler.player.isSpectator()) {
                handler.player.getAbilities().allowFlying = false;
                handler.player.getAbilities().flying = false;
                handler.player.sendAbilitiesUpdate();
            }
            var reachAttribute = handler.player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
            if (reachAttribute != null) {
                reachAttribute.removeModifier(REACH_MODIFIER_ID);
            }
        });
    }
    public static void activatePhantomMode(PlayerEntity player, ServerWorld world) {
        UUID uuid = player.getUuid();
        long endTick = world.getTime() + PHANTOM_DURATION;
        phantomEndTimes.put(uuid, endTick);
        PhantomWandItem.phantomPlayers.add(uuid);
        applyPhantomEffects(player);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new PhantomSyncPacket(true));
        }
    }
    public static boolean tryPullAttack(ServerPlayerEntity attacker, LivingEntity target) {
        if (!(attacker.getMainHandStack().getItem() instanceof PhantomWandItem)) {
            return false;
        }
        double distance = attacker.distanceTo(target);
        if (distance <= MELEE_RANGE || distance > PULL_RANGE) {
            return false;
        }
        long immunityEnd = ((ServerWorld) attacker.getEntityWorld()).getTime() + PULL_IMMUNITY_DURATION;
        pullImmunityEndTimes.put(attacker.getUuid(), immunityEnd);
        PhantomWandItem.pullingPlayers.add(attacker.getUuid());
        Vec3d direction = target.getEntityPos().subtract(attacker.getEntityPos()).normalize();
        double pullDistance = distance - MELEE_RANGE + 0.5;
        Vec3d destination = attacker.getEntityPos().add(direction.multiply(pullDistance));

        attacker.teleport(
                (ServerWorld) attacker.getEntityWorld(),
                destination.x,
                destination.y,
                destination.z,
                java.util.Set.of(),
                attacker.getYaw(),
                attacker.getPitch(),
                false
        );
        PullAttackScheduler.schedule(attacker, target, ((ServerWorld) attacker.getEntityWorld()).getTime() + 1);
        PhantomWandItem.pullingPlayers.remove(attacker.getUuid());
        return true;
    }
    public static float modifyOutgoingDamage(PlayerEntity attacker, float originalDamage) {
        if (PhantomWandItem.phantomPlayers.contains(attacker.getUuid())) {
            return originalDamage * 0.2f;
        }
        return originalDamage;
    }
}
