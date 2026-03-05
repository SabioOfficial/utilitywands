package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ElectricWandLightningHandler {
    private static class ScheduledBurst {
        final ServerWorld world;
        final PlayerEntity player;
        final List<LivingEntity> targets;
        final Map<LivingEntity, Float> healthBefore;
        final int burstCount;
        final int followupCount;
        final float damageThreshold;
        final double followupRadius;
        final int interval;

        int strikesFired = 0;
        int tickTimer = 0;
        boolean burstDone = false;
        boolean followupScheduled = false;
        int followupStrikesFired = 0;

        ScheduledBurst(ServerWorld world, PlayerEntity player, List<LivingEntity> targets, Map<LivingEntity, Float> healthBefore, int burstCount, int followupCount, float damageThreshold, double followupRadius, int interval) {
            this.world = world;
            this.player = player;
            this.targets = targets;
            this.healthBefore = healthBefore;
            this.burstCount = burstCount;
            this.followupCount = followupCount;
            this.damageThreshold = damageThreshold;
            this.followupRadius = followupRadius;
            this.interval = interval;
        }

        boolean tick() {
            if (world == null || player == null) return true;
            tickTimer--;
            if (!burstDone) {
                if (tickTimer <= 0 && strikesFired < burstCount) {
                    for (LivingEntity target : targets) {
                        if (!target.isRemoved()) {
                            ElectricWandItem.strikeLightningOn(target, world);
                        }
                    }
                    strikesFired++;
                    tickTimer = interval;
                }
                if (strikesFired >= burstCount) {
                    burstDone = true;
                    tickTimer = interval;
                }
                return false;
            }
            if (burstDone && !followupScheduled) {
                if (tickTimer > 0) return false;
                boolean anyBelowThreshold = false;
                for (LivingEntity target : targets) {
                    if (target.isRemoved()) continue;
                    float before = healthBefore.getOrDefault(target, target.getHealth());
                    float after = target.getHealth();
                    float totalDamage = before - after;
                    if (totalDamage < damageThreshold) {
                        anyBelowThreshold = true;
                        break;
                    }
                }
                followupScheduled = true;
                if (!anyBelowThreshold) {return true;}

                Box smallBox = player.getBoundingBox().expand(followupRadius);
                List<LivingEntity> followupTargets = world.getEntitiesByClass(LivingEntity.class, smallBox, entity -> entity != player && !entity.isRemoved());
                if (followupTargets.isEmpty()) return true;
                targets.clear();
                targets.addAll(followupTargets);
                burstDone = false;
                strikesFired = 0;
                followupPhase = true;
                tickTimer = interval;
                return false;
            }

            if (followupPhase) {
                if (tickTimer <= 0 && followupStrikesFired < followupCount) {
                    for (LivingEntity target : targets) {
                        if (!target.isRemoved()) {
                            ElectricWandItem.strikeLightningOn(target, world);
                        }
                    }
                    followupStrikesFired++;
                    tickTimer = interval;
                }
                return followupStrikesFired >= followupCount;
            }
            return true;
        }

        boolean followupPhase = false;
    }
    private static final List<ScheduledBurst> pendingBursts = new ArrayList<>();
    private static void onTick(MinecraftServer server) {
        pendingBursts.removeIf(ScheduledBurst::tick);
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(ElectricWandLightningHandler::onTick);
    }
    public static void scheduleBurst(ServerWorld world, PlayerEntity player, List<LivingEntity> targets, Map<LivingEntity, Float> healthBefore, int burstCount, int followupCount, float damageThreshold, double followupRadius, int interval) {
        pendingBursts.add(new ScheduledBurst(world, player, new ArrayList<>(targets), healthBefore, burstCount, followupCount, damageThreshold, followupRadius, interval));
    }
}
