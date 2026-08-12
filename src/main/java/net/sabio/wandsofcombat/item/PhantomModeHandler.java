package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.MobEffectInstance;
import net.minecraft.entity.effect.MobEffects;
import net.minecraft.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayer;
import net.minecraft.server.world.ServerLevel;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sabio.wandsofcombat.Wandsofcombat;
import net.sabio.wandsofcombat.network.PhantomSyncPacket;

import java.util.*;

public class PhantomModeHandler {
    private static final Map<UUID, Long> phantomEndTimes = new HashMap<>();
    private static final Map<UUID, Long> pullImmunityEndTimes = new HashMap<>();
    private static final Map<UUID, MobEffectInstance> savedInvisibility = new HashMap<>();
    private static final Map<UUID, MobEffectInstance> savedNightVision = new HashMap<>();
    private static final Map<UUID, MobEffectInstance> savedSlowness = new HashMap<>();
    private static final Map<UUID, MobEffectInstance> savedBlindness = new HashMap<>();
    private static final Set<UUID> applyingReducedDamage = new HashSet<>();
    private static final int PHANTOM_DURATION = 200; // 10 seconds
    private static final double PULL_RANGE = 5.0;
    private static final double MELEE_RANGE = 3.0;
    private static final double PULL_SPEED = 1.2;
    private static final int PULL_IMMUNITY_DURATION = 10; // immune for 10 ticks after pull
    private static final Identifier REACH_MODIFIER_ID = Identifier.of(Wandsofcombat.MOD_ID, "phantom_wand_reach");
    private static class PullProgress {
        final ServerPlayer attacker;
        final LivingEntity target;
        final Vec3d startPos;
        final Vec3d endPos;
        int totalTicks;
        int ticksElapsed;
        PullProgress(ServerPlayer attacker, LivingEntity target, Vec3d startPos, Vec3d endPos, int totalTicks) {
            this.attacker = attacker;
            this.target = target;
            this.startPos = startPos;
            this.endPos = endPos;
            this.totalTicks = totalTicks;
            this.ticksElapsed = 0;
        }
    }
    private static final Map<UUID, PullProgress> activePulls = new HashMap<>();
    private static void applyPhantomEffects(Player player) {
        MobEffectInstance existingInvisibility = player.getStatusEffect(MobEffects.INVISIBILITY);
        MobEffectInstance existingNightVision = player.getStatusEffect(MobEffects.NIGHT_VISION);
        MobEffectInstance existingSlowness = player.getStatusEffect(MobEffects.SLOWNESS);
        MobEffectInstance existingBlindness = player.getStatusEffect(MobEffects.BLINDNESS);
        if (existingInvisibility != null) {
            savedInvisibility.put(player.getUUID(), new MobEffectInstance(existingInvisibility));
        } else {
            savedInvisibility.remove(player.getUUID());
        }
        if (existingNightVision != null) {
            savedNightVision.put(player.getUUID(), new MobEffectInstance(existingNightVision));
        } else {
            savedNightVision.remove(player.getUUID());
        }
        if (existingSlowness != null) {
            savedSlowness.put(player.getUUID(), new MobEffectInstance(existingSlowness));
        } else {
            savedSlowness.remove(player.getUUID());
        }
        if (existingBlindness != null) {
            savedBlindness.put(player.getUUID(), new MobEffectInstance(existingBlindness));
        } else {
            savedBlindness.remove(player.getUUID());
        }
        player.addStatusEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY,
                PHANTOM_DURATION,
                0,
                false,
                false,
                false
        ));
        player.addStatusEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                PHANTOM_DURATION,
                0,
                false,
                false,
                false
        ));
        player.addStatusEffect(new MobEffectInstance(
                MobEffects.DARKNESS,
                PHANTOM_DURATION,
                0,
                false,
                false,
                false
        ));
    }
    private static void removePhantomEffects(Player player) {
        UUID uuid = player.getUUID();
        player.removeStatusEffect(MobEffects.INVISIBILITY);
        player.removeStatusEffect(MobEffects.NIGHT_VISION);
        player.removeStatusEffect(MobEffects.SLOWNESS);
        player.removeStatusEffect(MobEffects.BLINDNESS);
        player.setInvisible(false);
        MobEffectInstance savedInvis = savedInvisibility.remove(uuid);
        MobEffectInstance savedNV = savedNightVision.remove(uuid);
        MobEffectInstance savedSlow = savedSlowness.remove(uuid);
        MobEffectInstance savedBlind = savedBlindness.remove(uuid);
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
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new PhantomSyncPacket(false));
        }
    }
    private static void manageReachAttribute(Player player, boolean holding) {
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
        for (ServerLevel world : server.getWorlds()) {
            long currentTick = world.getTime();
            for (Player player : world.getPlayers()) {
                UUID uuid = player.getUUID();
                boolean holdingPhantomWand = player.getMainInteractionHandStack().getItem() instanceof PhantomWandItem;
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
                player.addStatusEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS,
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
                        pull.attacker.getEntityWorld(),
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
        ServerTickEvents.END_SERVER_TICK.register(PhantomModeInteractionHandler::onTick);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof Player player) {
                Long immunityEnd = pullImmunityEndTimes.get(player.getUUID());
                if (immunityEnd != null && entity.getEntityWorld().getTime() <= immunityEnd) {
                    return false;
                }
            }
            if (source.getAttacker() instanceof Player attacker) {
                UUID attackerId = attacker.getUUID();
                if (PhantomWandItem.phantomPlayers.contains(attackerId) && !applyingReducedDamage.contains(attackerId) && attacker.getMainInteractionHandStack().getItem() instanceof PhantomWandItem) {
                    applyingReducedDamage.add(attackerId);
                    entity.damage((ServerLevel) attacker.getEntityWorld(), source, amount * 0.2f);
                    applyingReducedDamage.remove(attackerId);
                    return false;
                }
            }
            return true;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
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
    public static void activatePhantomMode(Player player, ServerLevel world) {
        UUID uuid = player.getUUID();
        long endTick = world.getTime() + PHANTOM_DURATION;
        phantomEndTimes.put(uuid, endTick);
        PhantomWandItem.phantomPlayers.add(uuid);
        applyPhantomEffects(player);
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new PhantomSyncPacket(true));
        }
    }
    public static boolean tryPullAttack(ServerPlayer attacker, LivingEntity target) {
        if (!(attacker.getMainInteractionHandStack().getItem() instanceof PhantomWandItem)) {
            return false;
        }
        double distance = attacker.distanceTo(target);
        if (distance <= MELEE_RANGE || distance > PULL_RANGE) {
            return false;
        }
        long immunityEnd = (attacker.getEntityWorld()).getTime() + PULL_IMMUNITY_DURATION + 5;
        pullImmunityEndTimes.put(attacker.getUUID(), immunityEnd);
        PhantomWandItem.pullingPlayers.add(attacker.getUUID());
        Vec3d direction = target.getEntityPos().subtract(attacker.getEntityPos()).normalize();
        double pullDistance = distance - MELEE_RANGE + 0.5;
        Vec3d destination = attacker.getEntityPos().add(direction.multiply(pullDistance));

        activePulls.put(attacker.getUUID(), new PullProgress(
                attacker,
                target,
                attacker.getEntityPos(),
                destination,
                8
        ));
        return true;
    }
    public static float modifyOutgoingDamage(Player attacker, float originalDamage) {
        if (PhantomWandItem.phantomPlayers.contains(attacker.getUUID())) {
            return originalDamage * 0.2f;
        }
        return originalDamage;
    }
}
