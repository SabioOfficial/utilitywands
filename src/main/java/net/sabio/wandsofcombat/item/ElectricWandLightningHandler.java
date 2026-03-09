package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

public class ElectricWandLightningHandler {
    private static final Map<UUID, UUID> summonedSkeletons = new HashMap<>();
    private static final Map<UUID, Long> skeletonDespawnTimes = new HashMap<>();
    private static final Map<UUID, Long> stunnedEntities = new HashMap<>();
    private static final int STUN_DURATION = 30; // 1.5 seconds
    private static final int SKELETON_LIFETIME = 160; // 8 seconds
    private static void spawnSkeleton(ServerWorld world, PlayerEntity summoner, LivingEntity target, long currentTick) {
        SkeletonEntity skeleton = new SkeletonEntity(EntityType.SKELETON, world);
        skeleton.refreshPositionAndAngles(target.getX(), target.getY(), target.getZ(), summoner.getYaw(), 0);
        skeleton.initialize(world, world.getLocalDifficulty(skeleton.getBlockPos()), SpawnReason.MOB_SUMMONED, null);
        world.spawnEntity(skeleton);
        summonedSkeletons.put(skeleton.getUuid(), summoner.getUuid());
        skeletonDespawnTimes.put(skeleton.getUuid(), currentTick + SKELETON_LIFETIME);
        skeleton.setTarget(target);
    }
    private static class AbilityBurst {
        final ServerWorld world;
        final PlayerEntity player;
        final List<LivingEntity> targets;
        final int totalStrikes;
        int strikesFired = 0;
        int tickTimer = 0;
        static final int INTERVAL = 10;
        AbilityBurst(ServerWorld world, PlayerEntity player, List<LivingEntity> targets, int totalStrikes) {
            this.world = world;
            this.player = player;
            this.targets = targets;
            this.totalStrikes = totalStrikes;
        }
        boolean tick(long currentTick) {
            if (tickTimer > 0) {
                tickTimer--;
                return false;
            }
            if (strikesFired >= totalStrikes) return true;
            for (LivingEntity target : targets) {
                if (target.isRemoved()) continue;
                ElectricWandItem.strikeLightningOn(target, world);
                target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS,
                        STUN_DURATION,
                        127,
                        false,
                        false,
                        false
                ));
                target.setVelocity(0, target.getVelocity().y, 0);
                target.velocityDirty = true;
                if (target instanceof MobEntity mob) {
                    mob.setAttacking(false);
                    mob.setTarget(null);
                }
                stunnedEntities.put(target.getUuid(), currentTick + STUN_DURATION);
            }
            strikesFired++;
            tickTimer = INTERVAL;
            if (strikesFired >= totalStrikes) {
                for (LivingEntity target : targets) {
                    if (target.isRemoved()) continue;
                    spawnSkeleton(world, player, target, currentTick);
                }
                return true;
            }
            return false;
        }
    }
    private static final List<AbilityBurst> pendingBursts = new ArrayList<>();
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(ElectricWandLightningHandler::onTick);
    }
    private static void onTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            long currentTick = world.getTime();
            pendingBursts.removeIf(burst -> burst.tick(currentTick));
            stunnedEntities.entrySet().removeIf(entry -> {
                if (currentTick >= entry.getValue()) {
                    world.iterateEntities().forEach(entity -> {
                        if (!(entity instanceof MobEntity mob)) return;
                        if (!mob.getUuid().equals(entry.getKey())) return;
                        mob.setAiDisabled(false);
                    });
                    return true;
                }
                world.iterateEntities().forEach(entity -> {
                    if (!(entity instanceof MobEntity mob)) return;
                    if (!stunnedEntities.containsKey(mob.getUuid())) return;
                    mob.setAiDisabled(true);
                    mob.setAttacking(false);
                    mob.setVelocity(0, mob.getVelocity().y, 0);
                    mob.velocityDirty = true;
                });
                return false;
            });
            List<UUID> toRemove = Collections.synchronizedList(new ArrayList<>());
            for (Map.Entry<UUID, Long> entry : skeletonDespawnTimes.entrySet()) {
                UUID skeletonId = entry.getKey();
                world.iterateEntities().forEach(entity -> {
                    if (!(entity instanceof SkeletonEntity skeleton)) return;
                    if (!skeleton.getUuid().equals(skeletonId)) return;
                    LivingEntity target = skeleton.getTarget();
                    boolean expired = currentTick >= entry.getValue();
                    boolean targetDead = target == null || target.isDead() || target.isRemoved();
                    if (expired || targetDead) {
                        skeleton.discard();
                        toRemove.add(skeletonId);
                        summonedSkeletons.remove(skeletonId);
                        return;
                    }
                    UUID summonerId = summonedSkeletons.get(skeletonId);
                    if (summonerId == null) return;
                    if (skeleton.getTarget() instanceof PlayerEntity tp && tp.getUuid().equals(summonerId)) {
                        skeleton.setTarget(null);
                    }
                });
            }
            toRemove.forEach(uuid -> {
                skeletonDespawnTimes.remove(uuid);
                summonedSkeletons.remove(uuid);
            });
        }
    }
    public static void scheduleBurst(ServerWorld world, PlayerEntity player, List<LivingEntity> targets, int burstCount) {
        pendingBursts.add(new AbilityBurst(world, player, new ArrayList<>(targets), burstCount));
    }
    public static void scheduleBurst(ServerWorld world, PlayerEntity player, List<LivingEntity> targets, Map<LivingEntity, Float> healthBefore, int burstCount, int followupCount, float damageThreshold, double followupRadius, int interval) {
        scheduleBurst(world, player, targets, burstCount);
    }
    public static void scheduleAbility(ServerWorld world, PlayerEntity player, List<LivingEntity> targets, int strikes) {
        scheduleBurst(world, player, targets, strikes);
    }
}
