package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
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
    private static void scheduleSkeletonSpawn(ServerLevel world, Player summoner, LivingEntity target, long currentTick) {
        UUID key = UUID.randomUUID();
        pendingSkeletonData.put(key, new Object[]{world, summoner, target, currentTick + 40});
    }
    private static void clearAllTargetGoals(Skeleton skeleton) {
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
    private static void doSpawnSkeleton(ServerLevel world, Player summoner, LivingEntity target, long currentTick) {
        Skeleton skeleton = new Skeleton(EntityType.SKELETON, world);
        skeleton.snapTo(target.getX(), target.getY(), target.getZ(), summoner.getYRot(), 0);
        skeleton.finalizeSpawn(world, world.getCurrentDifficultyAt(skeleton.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        world.addFreshEntity(skeleton);
        clearAllTargetGoals(skeleton);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        skeleton.setTarget(target);
        summonedSkeletons.put(skeleton.getUUID(), summoner.getUUID());
        skeletonTargets.put(skeleton.getUUID(), target);
        skeletonSpawnTimes.put(skeleton.getUUID(), currentTick);
        skeletonDespawnTimes.put(skeleton.getUUID(), currentTick + SKELETON_LIFETIME);
        skeletonNextAttackTick.put(skeleton.getUUID(), currentTick + 20);
        skeleton.setTarget(target);
    }
    private static class AbilityBurst {
        final ServerLevel world;
        final Player player;
        final List<LivingEntity> targets;
        final int totalStrikes;
        int strikesFired = 0;
        int tickTimer = 0;
        static final int INTERVAL = 10;
        AbilityBurst(ServerLevel world, Player player, List<LivingEntity> targets, int totalStrikes) {
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
                target.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS,
                        STUN_DURATION,
                        127,
                        false,
                        false,
                        false
                ));
                target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
                target.hurtMarked = true;
                if (target instanceof Mob mob) {
                    mob.setAggressive(false);
                    mob.setTarget(null);
                }
                stunnedEntities.put(target.getUUID(), currentTick + STUN_DURATION);
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
            UUID playerUuid = handler.player.getUUID();
            List<UUID> toKill = new ArrayList<>();
            for (Map.Entry<UUID, UUID> entry : summonedSkeletons.entrySet()) {
                if (entry.getValue().equals(playerUuid)) {
                    toKill.add(entry.getKey());
                }
            }
            for (ServerLevel world : server.getAllLevels()) {
                List<Entity> snap = new ArrayList<>(world.getEntities(EntityTypeTest.forClass(Entity.class), _ -> true));
                for (Entity entity : snap) {
                    if (toKill.contains(entity.getUUID())) {
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
        for (ServerLevel world : server.getAllLevels()) {
            long currentTick = world.getGameTime();
            pendingSkeletonData.entrySet().removeIf(entry -> {
                Object[] data = entry.getValue();
                ServerLevel ServerLevel = (ServerLevel) data[0];
                Player summoner = (Player) data[1];
                LivingEntity target = (LivingEntity) data[2];
                long spawnAt = (long) data[3];
                if (currentTick < spawnAt) return false;
                if (target.isAlive() && !target.isRemoved()) {
                    doSpawnSkeleton(ServerLevel, summoner, target, currentTick);
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
                List<Entity> snapshot = new ArrayList<>(world.getEntities(EntityTypeTest.forClass(Entity.class), _ -> true));
                for (Entity entity : snapshot) {
                    if (!(entity instanceof Mob mob)) continue;
                    if (expiredStuns.contains(mob.getUUID())) {
                        mob.setNoAi(false);
                    }
                }
            }
            List<UUID> toRemove = new ArrayList<>();
            List<Map.Entry<UUID, Long>> snapshot = new ArrayList<>(skeletonDespawnTimes.entrySet());
            List<Entity> worldEntities = new ArrayList<>(world.getEntities(EntityTypeTest.forClass(Entity.class), _ -> true));
            for (Map.Entry<UUID, Long> entry : snapshot) {
                UUID skeletonId = entry.getKey();
                long despawnAt = entry.getValue();
                Skeleton skeletonRef = null;
                for (Entity entity : worldEntities) {
                    if (entity.getUUID().equals(skeletonId) && entity instanceof Skeleton skeleton) {
                        skeletonRef = skeleton;
                        break;
                    }
                }
                if (skeletonRef == null) continue;
                LivingEntity target = skeletonTargets.get(skeletonId);
                boolean targetGone = target == null || target.isRemoved() || !target.isAlive();
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

    private static @NotNull Arrow getArrow(ServerLevel world, Skeleton skeleton, LivingEntity originalTarget) {
        Arrow arrow = new Arrow(world, skeleton, new ItemStack(Items.ARROW), null);
        double aimDx = originalTarget.getX() - skeleton.getX();
        double aimDy = originalTarget.getY() + originalTarget.getBbHeight() / 2.0 - skeleton.getY() - skeleton.getBbHeight() / 2.0;
        double aimDz = originalTarget.getZ() - skeleton.getZ();
        arrow.shoot(aimDx, aimDy, aimDz, 1.6f, 1.0f);
        arrow.setPos(skeleton.getX(), skeleton.getY() + skeleton.getBbHeight() / 2.0, skeleton.getZ());
        return arrow;
    }

    public static void scheduleBurst(ServerLevel world, Player player, List<LivingEntity> targets, int burstCount) {
        pendingBursts.add(new AbilityBurst(world, player, new ArrayList<>(targets), burstCount));
    }
    public static void scheduleBurst(ServerLevel world, Player player, List<LivingEntity> targets, Map<LivingEntity, Float> healthBefore, int burstCount, int followupCount, float damageThreshold, double followupRadius, int interval) {
        scheduleBurst(world, player, targets, burstCount);
    }
    public static void scheduleAbility(ServerLevel world, Player player, List<LivingEntity> targets, int strikes) {
        scheduleBurst(world, player, targets, strikes);
    }
}
