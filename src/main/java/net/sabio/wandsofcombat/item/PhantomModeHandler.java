package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
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
    private static final Map<UUID, StatusEffectInstance> savedInvisibility = new HashMap<>();
    private static final Map<UUID, StatusEffectInstance> savedNightVision = new HashMap<>();
    private static final Map<UUID, StatusEffectInstance> savedSlowness = new HashMap<>();
    private static final Map<UUID, StatusEffectInstance> savedBlindness = new HashMap<>();
    private static final Set<UUID> applyingReducedDamage = new HashSet<>();
    private static final int PHANTOM_DURATION = 200; // 10 seconds
    private static final double PULL_RANGE = 5.0;
    private static final double MELEE_RANGE = 3.0;
    private static final double PULL_SPEED = 1.2;
    private static final int PULL_IMMUNITY_DURATION = 10; // immune for 10 ticks after pull
    private static final Identifier REACH_MODIFIER_ID = Identifier.of(Wandsofcombat.MOD_ID, "phantom_wand_reach");
    private static class PullProgress {
        final ServerPlayerEntity attacker;
        final LivingEntity target;
        final Vec3d startPos;
        final Vec3d endPos;
        int totalTicks;
        int ticksElapsed;
        PullProgress(ServerPlayerEntity attacker, LivingEntity target, Vec3d startPos, Vec3d endPos, int totalTicks) {
            this.attacker = attacker;
            this.target = target;
            this.startPos = startPos;
            this.endPos = endPos;
            this.totalTicks = totalTicks;
            this.ticksElapsed = 0;
        }
    }
    private static final Map<UUID, PullProgress> activePulls = new HashMap<>();
    private static void applyPhantomEffects(PlayerEntity player) {
        StatusEffectInstance existingInvisibility = player.getStatusEffect(StatusEffects.INVISIBILITY);
        StatusEffectInstance existingNightVision = player.getStatusEffect(StatusEffects.NIGHT_VISION);
        StatusEffectInstance existingSlowness = player.getStatusEffect(StatusEffects.SLOWNESS);
        StatusEffectInstance existingBlindness = player.getStatusEffect(StatusEffects.BLINDNESS);
        if (existingInvisibility != null) {
            savedInvisibility.put(player.getUuid(), new StatusEffectInstance(existingInvisibility));
        } else {
            savedInvisibility.remove(player.getUuid());
        }
        if (existingNightVision != null) {
            savedNightVision.put(player.getUuid(), new StatusEffectInstance(existingNightVision));
        } else {
            savedNightVision.remove(player.getUuid());
        }
        if (existingSlowness != null) {
            savedSlowness.put(player.getUuid(), new StatusEffectInstance(existingSlowness));
        } else {
            savedSlowness.remove(player.getUuid());
        }
        if (existingBlindness != null) {
            savedBlindness.put(player.getUuid(), new StatusEffectInstance(existingBlindness));
        } else {
            savedBlindness.remove(player.getUuid());
        }
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
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.DARKNESS,
                PHANTOM_DURATION,
                0,
                false,
                false,
                false
        ));
    }
    private static void removePhantomEffects(PlayerEntity player) {
        UUID uuid = player.getUuid();
        player.removeStatusEffect(StatusEffects.INVISIBILITY);
        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.setInvisible(false);
        StatusEffectInstance savedInvis = savedInvisibility.remove(uuid);
        StatusEffectInstance savedNV = savedNightVision.remove(uuid);
        StatusEffectInstance savedSlow = savedSlowness.remove(uuid);
        StatusEffectInstance savedBlind = savedBlindness.remove(uuid);
        if (savedInvis != null) {
            player.addStatusEffect(savedInvis);
        }
        if (savedNV != null) {
            player.addStatusEffect(savedNV);
        }
        if (savedSlow != null) {
            player.addStatusEffect(savedSlow);
        }
        if (savedBlind != null) {
            player.addStatusEffect(savedSlow);
        }
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().allowFlying = false;
            player.sendAbilitiesUpdate();
        }
        player.noClip = false;
        PhantomWandItem.phantomPlayers.remove(uuid);
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
                player.setInvisible(true);
                player.noClip = true;
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().allowFlying = true;
                    player.getAbilities().flying = true;
                    player.sendAbilitiesUpdate();
                }
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS,
                        2,
                        5,
                        false,
                        false,
                        false
                ));
            }
            List<UUID> finishedPulls = new ArrayList<>();
            for (Map.Entry<UUID, PullProgress> entry : activePulls.entrySet()) {
                PullProgress pull = entry.getValue();
                pull.ticksElapsed++;
                if (pull.attacker.isRemoved() || pull.target.isRemoved()) {
                    finishedPulls.add(entry.getKey());
                    continue;
                }
                float t = (float) pull.ticksElapsed / pull.totalTicks;
                t = Math.min(t, 1.0f);
                double x = pull.startPos.x + (pull.endPos.x - pull.startPos.x) * t;
                double y = pull.startPos.y + (pull.endPos.y - pull.startPos.y) * t;
                double z = pull.startPos.z + (pull.endPos.z - pull.startPos.z) * t;
                pull.attacker.teleport(
                        pull.attacker.getWorld(),
                        x,
                        y,
                        z,
                        java.util.Set.of(),
                        pull.attacker.getYaw(),
                        pull.attacker.getPitch(),
                        false
                );
                if (pull.ticksElapsed >= pull.totalTicks) {
                    pull.attacker.attack(pull.target);
                    PhantomWandItem.pullingPlayers.remove(entry.getKey());
                    finishedPulls.add(entry.getKey());
                }
            }
            finishedPulls.forEach(activePulls::remove);
        }
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PhantomModeHandler::onTick);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof PlayerEntity player) {
                Long immunityEnd = pullImmunityEndTimes.get(player.getUuid());
                if (immunityEnd != null && entity.getWorld().getTime() <= immunityEnd) {
                    return false;
                }
            }
            if (source.getAttacker() instanceof PlayerEntity attacker) {
                UUID attackerId = attacker.getUuid();
                if (PhantomWandItem.phantomPlayers.contains(attackerId) && !applyingReducedDamage.contains(attackerId) && attacker.getMainHandStack().getItem() instanceof PhantomWandItem) {
                    applyingReducedDamage.add(attackerId);
                    entity.damage((ServerWorld) attacker.getWorld(), source, amount * 0.2f);
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
            savedInvisibility.remove(uuid);
            savedNightVision.remove(uuid);
            savedSlowness.remove(uuid);
            savedBlindness.remove(uuid);
            activePulls.remove(uuid);
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
        long immunityEnd = (attacker.getWorld()).getTime() + PULL_IMMUNITY_DURATION + 5;
        pullImmunityEndTimes.put(attacker.getUuid(), immunityEnd);
        PhantomWandItem.pullingPlayers.add(attacker.getUuid());
        Vec3d direction = target.getPos().subtract(attacker.getPos()).normalize();
        double pullDistance = distance - MELEE_RANGE + 0.5;
        Vec3d destination = attacker.getPos().add(direction.multiply(pullDistance));

        activePulls.put(attacker.getUuid(), new PullProgress(
                attacker,
                target,
                attacker.getPos(),
                destination,
                8
        ));
        return true;
    }
    public static float modifyOutgoingDamage(PlayerEntity attacker, float originalDamage) {
        if (PhantomWandItem.phantomPlayers.contains(attacker.getUuid())) {
            return originalDamage * 0.2f;
        }
        return originalDamage;
    }
}
