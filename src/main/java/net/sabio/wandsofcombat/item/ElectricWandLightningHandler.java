package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

public class ElectricWandLightningHandler {
    private static final Map<UUID, UUID> summonedSkeletons = new HashMap<>();
    private static final Map<UUID, Long> skeletonDespawnTimes = new HashMap<>();
    private static final Map<UUID, Long> stunnedEntities = new HashMap<>();
    private static final Map<UUID, Long> skeletonSpawnTimes = new HashMap<>();
    private static final Map<UUID, LivingEntity> skeletonTargets = new HashMap<>();
    private static final Map<UUID, Long> skeletonNextAttackTick = new HashMap<>();
    private static final Map<UUID, Object[]> pendingSkeletonData = new HashMap<>();
    private static final int STUN_DURATION = 8; // 1.5 seconds
    private static final int SKELETON_LIFETIME = 160; // 8 seconds
    private static void scheduleSkeletonSpawn(ServerWorld world, PlayerEntity summoner, LivingEntity target, long currentTick) {
        UUID key = UUID.randomUUID();
        pendingSkeletonData.put(key, new Object[]{world, summoner, target, currentTick + 40});
    }
    private static void clearAllTargetGoals(SkeletonEntity skeleton) {
        try {
            Field tsField = null;
            Class<?> aClass = skeleton.getClass();
            while (aClass != null) {
                try {
                    tsField = aClass.getDeclaredField("targetSelector");
                    break;
                } catch (NoSuchFieldException ignored) {
                    aClass = aClass.getSuperclass();
                }
            }
            if (tsField == null) return;
            tsField.setAccessible(true);
            Object ts = tsField.get(skeleton);
            Class<?> gsClass = ts.getClass();
            while (gsClass != null) {
                for (Field field : gsClass.getDeclaredFields()) {
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        try {
                            Collection<?> collection = (Collection<?>) field.get(ts);
                            new ArrayList<>(collection).forEach(item -> {
                                try {
                                    collection.remove(item);
                                } catch (Exception ignored) {}
                            });
                        } catch (Exception ignored) {}
                    }
                }
                gsClass = gsClass.getSuperclass();
            }
        } catch (Exception ignored) {}
    }
    private static void doSpawnSkeleton(ServerWorld world, PlayerEntity summoner, LivingEntity target, long currentTick) {
        SkeletonEntity skeleton = new SkeletonEntity(EntityType.SKELETON, world);
        skeleton.refreshPositionAndAngles(target.getX(), target.getY(), target.getZ(), summoner.getYaw(), 0);
        skeleton.initialize(world, world.getLocalDifficulty(skeleton.getBlockPos()), SpawnReason.MOB_SUMMONED, null);
        world.spawnEntity(skeleton);
        clearAllTargetGoals(skeleton);
        skeleton.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        skeleton.setTarget(target);
        summonedSkeletons.put(skeleton.getUuid(), summoner.getUuid());
        skeletonTargets.put(skeleton.getUuid(), target);
        skeletonSpawnTimes.put(skeleton.getUuid(), currentTick);
        skeletonDespawnTimes.put(skeleton.getUuid(), currentTick + SKELETON_LIFETIME);
        skeletonNextAttackTick.put(skeleton.getUuid(), currentTick + 20);
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
                    scheduleSkeletonSpawn(world, player, target, currentTick);
                }
                return true;
            }
            return false;
        }
    }
    private static final List<AbilityBurst> pendingBursts = new ArrayList<>();
    public static void initialize() {
        ServerTickEvents.START_SERVER_TICK.register(ElectricWandLightningHandler::onTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerUuid = handler.player.getUuid();
            List<UUID> toKill = new ArrayList<>();
            for (Map.Entry<UUID, UUID> entry : summonedSkeletons.entrySet()) {
                if (entry.getValue().equals(playerUuid)) {
                    toKill.add(entry.getKey());
                }
            }
            for (ServerWorld world : server.getWorlds()) {
                List<Entity> snap = new ArrayList<>();
                world.iterateEntities().forEach(snap::add);
                for (Entity entity : snap) {
                    if (toKill.contains(entity.getUuid())) {
                        entity.discard();
                    }
                }
            }
            toKill.forEach(uuid -> {
                summonedSkeletons.remove(uuid);
                skeletonDespawnTimes.remove(uuid);
                skeletonSpawnTimes.remove(uuid);
                skeletonTargets.remove(uuid);
                skeletonNextAttackTick.remove(uuid);
            });
        });
    }
    private static void onTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            long currentTick = world.getTime();
            pendingSkeletonData.entrySet().removeIf(entry -> {
                Object[] data = entry.getValue();
                ServerWorld serverWorld = (ServerWorld) data[0];
                PlayerEntity summoner = (PlayerEntity) data[1];
                LivingEntity target = (LivingEntity) data[2];
                long spawnAt = (long) data[3];
                if (currentTick < spawnAt) return false;
                if (!target.isDead() && !target.isRemoved()) {
                    doSpawnSkeleton(serverWorld, summoner, target, currentTick);
                }
                return true;
            });
            pendingBursts.removeIf(burst -> burst.tick(currentTick));
            Set<UUID> currentlyStunned = new HashSet<>(stunnedEntities.keySet());
            List<UUID> expiredStuns = new ArrayList<>();
            stunnedEntities.entrySet().removeIf(entry -> {
                if (currentTick >= entry.getValue()) {
                    expiredStuns.add(entry.getKey());
                    return true;
                }
                return false;
            });
            if (!expiredStuns.isEmpty()) {
                List<Entity> snapshot = new ArrayList<>();
                world.iterateEntities().forEach(snapshot::add);
                for (Entity entity : snapshot) {
                    if (!(entity instanceof MobEntity mob)) continue;
                    if (expiredStuns.contains(mob.getUuid())) {
                        mob.setAiDisabled(false);
                    }
                }
            }
            List<UUID> toRemove = new ArrayList<>();
            List<Map.Entry<UUID, Long>> snapshot = new ArrayList<>(skeletonDespawnTimes.entrySet());
            List<Entity> worldEntities = new ArrayList<>();
            world.iterateEntities().forEach(worldEntities::add);
            for (Map.Entry<UUID, Long> entry : snapshot) {
                UUID skeletonId = entry.getKey();
                long despawnAt = entry.getValue();
                SkeletonEntity skeletonRef = null;
                for (Entity entity : worldEntities) {
                    if (entity.getUuid().equals(skeletonId) && entity instanceof SkeletonEntity skeleton) {
                        skeletonRef = skeleton;
                        break;
                    }
                }
                if (skeletonRef == null) continue;
                LivingEntity target = skeletonTargets.get(skeletonId);
                boolean targetGone = target == null || target.isRemoved() || target.isDead();
                boolean expired = currentTick >= despawnAt;
                if (targetGone || expired) {
                    skeletonRef.discard();
                    toRemove.add(skeletonId);
                    continue;
                }
                skeletonRef.setTarget(target);
            }
            toRemove.forEach(uuid -> {
                skeletonDespawnTimes.remove(uuid);
                skeletonSpawnTimes.remove(uuid);
                summonedSkeletons.remove(uuid);
                skeletonTargets.remove(uuid);
                skeletonNextAttackTick.remove(uuid);
            });
        }
    }

    private static @NotNull ArrowEntity getArrowEntity(ServerWorld world, SkeletonEntity skeleton, LivingEntity originalTarget) {
        ArrowEntity arrow = new ArrowEntity(world, skeleton, new ItemStack(Items.ARROW), null);
        double aimDx = originalTarget.getX() - skeleton.getX();
        double aimDy = originalTarget.getY() + originalTarget.getHeight() / 2.0 - skeleton.getY() - skeleton.getHeight() / 2.0;
        double aimDz = originalTarget.getZ() - skeleton.getZ();
        arrow.setVelocity(aimDx, aimDy, aimDz, 1.6f, 1.0f);
        arrow.setPosition(skeleton.getX(), skeleton.getY() + skeleton.getHeight() / 2.0, skeleton.getZ());
        return arrow;
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
