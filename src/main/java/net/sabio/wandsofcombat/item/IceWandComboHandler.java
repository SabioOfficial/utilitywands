package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
        final ServerLevel world;
        final LivingEntity target;
        final Player attacker;
        List<Vec3> spikePositions;
        final Set<UUID> damagedEntities = new HashSet<>();
        int currentSpike = 0;
        int tickTimer = 0;
        int startDelay = 8;
        boolean started = false;
        private static List<Vec3> computeSpikePositions(Player attacker, LivingEntity target) {
            List<Vec3> positions = new ArrayList<>();
            Vec3 start = attacker.position();
            Vec3 end = target.position();
            Vec3 direction = end.subtract(start).normalize();
            double totalDistance = start.distanceTo(end);
            double traveled = SPIKE_SPACING;
            while (traveled <= totalDistance + SPIKE_SPACING && positions.size() < SPIKES_COUNT) {
                positions.add(start.add(direction.scale(traveled)));
                traveled += SPIKE_SPACING;
            }
            return positions;
        }
        SpikeAnimation(ServerLevel world, Player attacker, LivingEntity target) {
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
            Vec3 position = spikePositions.get(currentSpike);
            for (int i = 0; i < 10; i++) {
                double offsetX = (world.getRandom().nextDouble() - 0.5) * 0.4;
                double offsetZ = (world.getRandom().nextDouble() - 0.5) * 0.4;
                world.sendParticles(
                        new DustParticleOptions(0x80D9FF, 2.5f),
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
            world.sendParticles(ParticleTypes.SNOWFLAKE,
                    position.x,
                    position.y + 0.5,
                    position.z,
                    4,
                    0.15,
                    0.4,
                    0.15,
                    0.02
            );
            world.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                    position.x,
                    position.y + 0.3,
                    position.z,
                    6,
                    0.1,
                    0.3,
                    0.1,
                    0.1
            );
            AABB hitAABB = new AABB(
                    position.x - 0.8,
                    position.y - 0.3,
                    position.z - 0.8,
                    position.x + 0.8,
                    position.y + 1.8,
                    position.z + 0.8);
            List<LivingEntity> hit = world.getEntitiesOfClass(LivingEntity.class, hitAABB, entity -> entity != attacker && !entity.isRemoved() && entity.isAlive() && !damagedEntities.contains(entity.getUUID()));
            for (LivingEntity entity : hit) {
                damagedEntities.add(entity.getUUID());
                entity.hurtServer(world, world.damageSources().indirectMagic(attacker, attacker), DAMAGE);
                entity.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS,
                        SLOWNESS_DURATION,
                        SLOWNESS_AMPLIFIER,
                        false,
                        true,
                        true
                ));
                world.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()),
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
        for (ServerLevel world : server.getAllLevels()) {
            long currentTick = world.getGameTime();
            activeAnimations.removeIf(anim -> anim.world == world && anim.tick());
            List<UUID> windUpDone = new ArrayList<>();
            for (Player player : world.players()) {
                UUID uuid = player.getUUID();
                Long endTick = windUpEndTick.get(uuid);
                if (endTick == null) continue;
                long startTick = windUpStartTick.getOrDefault(uuid, currentTick);
                long elapsed = currentTick - startTick;
                double progress = (double) elapsed / WIND_UP_DURATION;
                player.setDeltaMovement(0, 0, 0);
                player.hurtMarked = true;
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
                }
                int ringCount = (int)(2 * Math.exp(3.0 * progress));
                double radius = 1.2 + progress * 0.5;
                for (int i = 0; i < ringCount; i++) {
                    double angle = (2.0 * Math.PI / Math.max(ringCount, 1)) * i + (elapsed * 0.2);
                    double px = player.getX() + radius * Math.cos(angle);
                    double pz = player.getZ() + radius * Math.sin(angle);
                    double py = player.getY() + world.getRandom().nextDouble() * 2.2;
                    world.sendParticles(
                            new DustParticleOptions(0x80D9FF, 1.5f),
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
                world.sendParticles(ParticleTypes.SNOWFLAKE,
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
                if (target == null || target.isRemoved() || !target.isAlive()) continue;
                for (Player player : world.players()) {
                    if (player.getUUID().equals(uuid)) {
                        activeAnimations.add(new SpikeAnimation(world, player, target));
                        AABB knockbackAABB = player.getBoundingBox().inflate(4.0);
                        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, knockbackAABB, entity -> entity != player && !entity.isRemoved());
                        for (LivingEntity entity : nearby) {
                            Vec3 away = entity.position().subtract(player.position());
                            if (away.horizontalDistance() < 0.01) away = new Vec3(1, 0, 0);
                            away = away.normalize();
                            entity.setDeltaMovement(away.x * 1.1, 0.1, away.z * 1.1);
                            entity.hurtMarked = true;
                            if (entity instanceof ServerPlayer sp) {
                                sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
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
            if (!(entity instanceof Player p)) return true;
            if (!windUpEndTick.containsKey(p.getUUID())) return true;
            if (amount > 0) {
                entity.hurtServer((ServerLevel) entity.level(), source, amount * 0.1f);
                return false;
            }
            return true;
        });
    }
    public static void onHit(Player attacker, LivingEntity target) {
        if (attacker.level().isClientSide()) return;
        if (!(attacker.level() instanceof ServerLevel ServerLevel)) return;
        UUID uuid = attacker.getUUID();
        if (windUpEndTick.containsKey(uuid)) return;
        long currentTick = ServerLevel.getGameTime();
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
