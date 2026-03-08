package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class IceWandComboHandler {
    private static final int COMBO_HITS = 6; // 6 combos = combo trigger triggers
    private static final float DAMAGE = 22.0f;
    private static final int SLOWNESS_DURATION = 60; // 3 seconds
    private static final int SLOWNESS_AMPLIFIER = 3; // slowness 4
    private static final double SPIKE_SPACING = 0.55; // distance between each spike
    private static final int SPIKES_COUNT = 12;
    private static final int SPIKE_INTERVAL = 4; // in ticks
    private static final Map<UUID, Integer> hitCounters = new HashMap<>();
    private static class SpikeAnimation {
        final ServerWorld world;
        final LivingEntity target;
        final PlayerEntity attacker;
        final List<Vec3d> spikePositions;
        int currentSpike = 0;
        int tickTimer = 0;
        boolean damaged = false;
        private static List<Vec3d> computeSpikePositions(PlayerEntity attacker, LivingEntity target) {
            List<Vec3d> positions = new ArrayList<>();
            Vec3d start = attacker.getEntityPos();
            Vec3d end = target.getEntityPos();
            Vec3d direction = end.subtract(start).normalize();
            double totalDistance = start.distanceTo(end);
            double traveled = SPIKE_SPACING;
            while (traveled <= totalDistance + SPIKE_SPACING && positions.size() < SPIKES_COUNT) {
                positions.add(start.add(direction.multiply(traveled)));
                traveled += SPIKE_SPACING;
            }
            return positions;
        }
        SpikeAnimation(ServerWorld world, PlayerEntity attacker, LivingEntity target) {
            this.world = world;
            this.attacker = attacker;
            this.target = target;
            this.spikePositions = computeSpikePositions(attacker, target);
        }
        boolean tick(long currentTick) {
            if (tickTimer > 0) {
                tickTimer--;
                return false;
            }
            if (currentSpike >= spikePositions.size()) return true;
            Vec3d position = spikePositions.get(currentSpike);
            for (int i = 0; i < 12; i++) {
                double offsetX = (world.getRandom().nextDouble() - 0.5) * 0.3;
                double offsetZ = (world.getRandom().nextDouble() - 0.5) * 0.3;
                world.spawnParticles(
                        new BlockStateParticleEffect(
                                ParticleTypes.BLOCK,
                                Blocks.PACKED_ICE.getDefaultState()),
                        position.x + offsetX,
                        position.y + 0.5,
                        position.z + offsetZ,
                        4,
                        0.05,
                        0.3,
                        0.05,
                        0.15
                );
            }
            world.spawnParticles(
                    ParticleTypes.SNOWFLAKE,
                    position.x,
                    position.y + 0.5,
                    position.z,
                    6,
                    0.1,
                    0.4,
                    0.1,
                    0.02
            );
            currentSpike++;
            tickTimer = SPIKE_INTERVAL;
            if (!damaged && currentSpike >= spikePositions.size()) {
                if (!target.isRemoved() && !target.isDead()) {
                    target.damage(world, world.getDamageSources().magic(), DAMAGE);
                    target.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOWNESS,
                            SLOWNESS_DURATION,
                            SLOWNESS_AMPLIFIER,
                            false,
                            true,
                            true
                    ));
                    world.spawnParticles(
                            new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BLUE_ICE.getDefaultState()),
                            target.getX(),
                            target.getY() + 1.0,
                            target.getZ(),
                            20,
                            0.3,
                            0.6,
                            0.3,
                            0.05
                    );
                }
                damaged = true;
            }
            return currentSpike >= spikePositions.size() && damaged;
        }
    }
    private static void onTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            long currentTick = world.getTime();
            activeAnimations.removeIf(animation -> animation.world == world && animation.tick(currentTick));
        }
    }
    private static final List<SpikeAnimation> activeAnimations = new ArrayList<>();
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(IceWandComboHandler::onTick);
    }
    public static void onHit(PlayerEntity attacker, LivingEntity target) {
        if (attacker.getEntityWorld().isClient()) return;
        UUID uuid = attacker.getUuid();
        int hits = hitCounters.getOrDefault(uuid, 0) + 1;
        if (hits >= COMBO_HITS) {
            hitCounters.put(uuid, 0);
            if (attacker.getEntityWorld() instanceof ServerWorld serverWorld) {
                activeAnimations.add(new SpikeAnimation(serverWorld, attacker, target));
            }
        } else {
            hitCounters.put(uuid, hits);
        }
    }
}
