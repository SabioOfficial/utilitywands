package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
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
    private static final int WIND_UP_DURATION = 40;
    private static final Map<UUID, Integer> hitCounters = new HashMap<>();
    private static final Map<UUID, Long> windUpStartTick = new HashMap<>();
    private static final Map<UUID, Long> windUpEndTick = new HashMap<>();
    private static final Map<UUID, LivingEntity> windUpTarget = new HashMap<>();
    private static class SpikeAnimation {
        final ServerWorld world;
        final LivingEntity target;
        final PlayerEntity attacker;
        List<Vec3d> spikePositions;
        final Set<UUID> damagedEntities = new HashSet<>();
        int currentSpike = 0;
        int tickTimer = 0;
        int startDelay = 8;
        boolean started = false;
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
            this.spikePositions = new ArrayList<>();
        }
        boolean tick() {
            if (startDelay > 0) {
                startDelay--;
                return false;
            }
            if (!started) {
                spikePositions.addAll(computeSpikePositions(attacker, target));
                started = true;
            }
            if (tickTimer > 0) {
                tickTimer--;
                return false;
            }
            if (currentSpike >= spikePositions.size()) return true;
            Vec3d position = spikePositions.get(currentSpike);
            for (int i = 0; i < 10; i++) {
                double offsetX = (world.getRandom().nextDouble() - 0.5) * 0.4;
                double offsetZ = (world.getRandom().nextDouble() - 0.5) * 0.4;
                world.spawnParticles(
                        new DustParticleEffect(0x80D9FF, 2.5f),
                        position.x + offsetX,
                        position.y + 0.5,
                        position.z + offsetZ,
                        1,
                        0,
                        0.3,
                        0,
                        0
                );
            }
            world.spawnParticles(ParticleTypes.SNOWFLAKE,
                    position.x,
                    position.y + 0.5,
                    position.z,
                    4,
                    0.15,
                    0.4,
                    0.15,
                    0.02
            );
            world.spawnParticles(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
                    position.x,
                    position.y + 0.3,
                    position.z,
                    6,
                    0.1,
                    0.3,
                    0.1,
                    0.1
            );
            Box hitBox = new Box(
                    position.x - 0.8,
                    position.y - 0.3,
                    position.z - 0.8,
                    position.x + 0.8,
                    position.y + 1.8,
                    position.z + 0.8);
            List<LivingEntity> hit = world.getEntitiesByClass(LivingEntity.class, hitBox, entity -> entity != attacker && !entity.isRemoved() && !entity.isDead()&& !damagedEntities.contains(entity.getUuid()));
            for (LivingEntity entity : hit) {
                damagedEntities.add(entity.getUuid());
                entity.damage(world, world.getDamageSources().magic(), DAMAGE);
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS,
                        SLOWNESS_DURATION,
                        SLOWNESS_AMPLIFIER,
                        false,
                        true,
                        true
                ));
                world.spawnParticles(
                        new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BLUE_ICE.getDefaultState()),
                        entity.getX(),
                        entity.getY() + 1.0,
                        entity.getZ(),
                        20,
                        0.3,
                        0.5,
                        0.3,
                        0.15
                );
            }
            currentSpike++;
            tickTimer = SPIKE_INTERVAL;
            return currentSpike >= spikePositions.size();
        }
    }
    private static void onTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            long currentTick = world.getTime();
            activeAnimations.removeIf(anim -> anim.world == world && anim.tick());
            List<UUID> windUpDone = new ArrayList<>();
            for (PlayerEntity player : world.getPlayers()) {
                UUID uuid = player.getUuid();
                Long endTick = windUpEndTick.get(uuid);
                if (endTick == null) continue;
                long startTick = windUpStartTick.getOrDefault(uuid, currentTick);
                long elapsed = currentTick - startTick;
                double progress = (double) elapsed / WIND_UP_DURATION;
                player.setVelocity(0, 0, 0);
                player.velocityDirty = true;
                if (player instanceof ServerPlayerEntity serverPlayerEntity) {
                    serverPlayerEntity.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayerEntity));
                }
                int ringCount = (int)(2 * Math.exp(3.0 * progress));
                double radius = 1.2 + progress * 0.5;
                for (int i = 0; i < ringCount; i++) {
                    double angle = (2.0 * Math.PI / Math.max(ringCount, 1)) * i + (elapsed * 0.2);
                    double px = player.getX() + radius * Math.cos(angle);
                    double pz = player.getZ() + radius * Math.sin(angle);
                    double py = player.getY() + world.getRandom().nextDouble() * 2.2;
                    world.spawnParticles(
                            new DustParticleEffect(0x80D9FF, 1.5f),
                            px,
                            py,
                            pz,
                            1,
                            0,
                            0,
                            0,
                            0
                    );
                }
                world.spawnParticles(ParticleTypes.SNOWFLAKE,
                        player.getX(),
                        player.getY() + 1.0,
                        player.getZ(),
                        (int)(progress * 10),
                        1.2,
                        0.6,
                        1.2,
                        0.01);
                if (currentTick >= endTick) windUpDone.add(uuid);
            }
            for (UUID uuid : windUpDone) {
                windUpEndTick.remove(uuid);
                windUpStartTick.remove(uuid);
                LivingEntity target = windUpTarget.remove(uuid);
                if (target == null || target.isRemoved() || target.isDead()) continue;
                for (PlayerEntity player : world.getPlayers()) {
                    if (player.getUuid().equals(uuid)) {
                        activeAnimations.add(new SpikeAnimation(world, player, target));
                        Box knockbackBox = player.getBoundingBox().expand(4.0);
                        List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class, knockbackBox, entity -> entity != player && !entity.isRemoved());
                        for (LivingEntity entity : nearby) {
                            Vec3d away = entity.getEntityPos().subtract(player.getEntityPos());
                            if (away.horizontalLength() < 0.01) away = new Vec3d(1, 0, 0);
                            away = away.normalize();
                            entity.setVelocity(away.x * 1.1, 0.1, away.z * 1.1);
                            entity.velocityDirty = true;
                            if (entity instanceof ServerPlayerEntity sp) {
                                sp.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(sp));
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
    private static final List<SpikeAnimation> activeAnimations = new ArrayList<>();
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(IceWandComboHandler::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof PlayerEntity p)) return true;
            if (!windUpEndTick.containsKey(p.getUuid())) return true;
            if (amount > 0) {
                entity.damage((ServerWorld) entity.getEntityWorld(), source, amount * 0.1f);
                return false;
            }
            return true;
        });
    }
    public static void onHit(PlayerEntity attacker, LivingEntity target) {
        if (attacker.getEntityWorld().isClient()) return;
        if (!(attacker.getEntityWorld() instanceof ServerWorld serverWorld)) return;
        UUID uuid = attacker.getUuid();
        if (windUpEndTick.containsKey(uuid)) return;
        long currentTick = serverWorld.getTime();
        int hits = hitCounters.getOrDefault(uuid, 0) + 1;
        if (hits >= COMBO_HITS) {
            hitCounters.put(uuid, 0);
            windUpStartTick.put(uuid, currentTick);
            windUpEndTick.put(uuid, currentTick + WIND_UP_DURATION);
            windUpTarget.put(uuid, target);
        } else {
            hitCounters.put(uuid, hits);
        }
    }
}
