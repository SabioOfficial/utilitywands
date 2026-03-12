package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

// may i speak to your manager please
public class MagnetPullManager {
    private static final double PASSIVE_RANGE = 8.0;
    private static final double PASSIVE_SPEED = 0.4;
    private static final long COMBO_RESET_INTERVAL = 2400; // 2 mins
    private static final Set<UUID> repelModeActive = new HashSet<>();
    private static final Map<UUID, Map<UUID, Integer>> comboHits = new HashMap<>();
    private static final Map<UUID, Map<UUID, Float>> speedMultipliers = new HashMap<>();
    private static long lastResetTick = 0;

    private static void spawnRing(ServerWorld world, Vec3d center, double radius, int color) {
        int points = 32;
        DustParticleEffect dustParticleEffect = new DustParticleEffect(color, 1.2f);
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2 / points) * i;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(dustParticleEffect, x, center.y, z, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnSpiralAround(ServerWorld world, Vec3d from, Vec3d to) {
        int steps = 12;
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            double x = from.x + (to.x - from.x) * t;
            double y = from.y + (to.y - from.y) * t;
            double z = from.z + (to.z - from.z) * t;
            double angle = t * Math.PI * 4;
            double offset = 0.4 * (1 - t);
            world.spawnParticles(
                    ParticleTypes.WITCH,
                    x + Math.cos(angle) * offset,
                    y,
                    z + Math.sin(angle) * offset,
                    1, 0, 0, 0, 0
            );
        }
    }

    public static boolean isRepelMode(PlayerEntity player) {
        return repelModeActive.contains(player.getUuid());
    }
    public static void toggleRepelMode(PlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean nowRepel = !repelModeActive.remove(uuid);
        if (nowRepel) repelModeActive.add(uuid);
        ItemStack stack = player.getMainHandStack();
        if (stack.getItem() instanceof MagnetWandItem) {
            if (nowRepel) {
                stack.set(ModItems.MAGNET_REPEL_MODE, true);
            } else {
                stack.remove(ModItems.MAGNET_REPEL_MODE);
            }
        }
    }
    public static void recordHit(PlayerEntity attacker, LivingEntity target) {
        UUID attackerUuid = attacker.getUuid();
        UUID targetUuid = target.getUuid();
        Map<UUID, Integer> hits = comboHits.computeIfAbsent(attackerUuid, uuid -> new HashMap<>());
        int current = hits.getOrDefault(targetUuid, 0) + 1;
        if (current >= 3) {
            Map<UUID, Float> multipliers = speedMultipliers.computeIfAbsent(attackerUuid, uuid -> new HashMap<>());
            multipliers.put(targetUuid, multipliers.getOrDefault(targetUuid, 1.0f) + 0.08f);
            hits.put(targetUuid, 0);
        } else {
            hits.put(targetUuid, current);
        }
    }
    private static float getSpeedMultiplier(PlayerEntity player, LivingEntity entity) {
        Map<UUID, Float> multipliers = speedMultipliers.get(player.getUuid());
        if (multipliers == null) return 1.0f;
        return multipliers.getOrDefault(entity.getUuid(), 1.0f);
    }
    public static void doAbilityPull(PlayerEntity player, double range) {
        if (!(player.getWorld() instanceof ServerWorld world)) return;
        Box box = player.getBoundingBox().expand(range);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, entity -> entity != player && !entity.isRemoved());
        Vec3d playerPos = player.getPos();
        spawnRing(world, playerPos.add(0, player.getHeight() / 2.0, 0), range, 0x4488FF);
        for (LivingEntity entity : entities) {
            float multiplier = getSpeedMultiplier(player, entity);
            Vec3d toward = new Vec3d(playerPos.x - entity.getX(), 0, playerPos.z - entity.getZ()).normalize();
            entity.setVelocity(toward.multiply(1.5 * multiplier).add(0, 0.2, 0));
            entity.velocityDirty = true;
            if (entity instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
            }
            Vec3d entityPosition = entity.getPos().add(0, entity.getHeight() / 2.0, 0);
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                world.spawnParticles(
                        ParticleTypes.WITCH,
                        entityPosition.x,
                        entityPosition.y,
                        entityPosition.z,
                        1,
                        Math.cos(angle) * 0.15,
                        0.1,
                        Math.sin(angle) * 0.15,
                        0.05
                );
            }
            spawnSpiralAround(world, entity.getPos(), playerPos);
        }
    }

    public static void doRepel(PlayerEntity player, double range, float damage) {
        if (!(player.getWorld() instanceof ServerWorld world)) return;
        Box box = player.getBoundingBox().expand(range);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, entity -> entity != player && !entity.isRemoved());
        Vec3d playerPosition = player.getPos();
        spawnRing(world, playerPosition.add(0, player.getHeight() / 2.0, 0), range, 0xFF4422);
        world.spawnParticles(ParticleTypes.EXPLOSION, playerPosition.x, playerPosition.y + 1, playerPosition.z, 3, 0.3, 0.3, 0.3, 0.1);
        world.spawnParticles(ParticleTypes.FLAME, playerPosition.x, playerPosition.y + 1, playerPosition.z, 20, 0.5, 0.5, 0.5, 0.15);
        for (LivingEntity entity : entities) {
            float multiplier = getSpeedMultiplier(player, entity);
            Vec3d away = new Vec3d(entity.getX() - playerPosition.x, 0, entity.getZ() - playerPosition.z);
            if (away.horizontalLength() < 0.01) away = new Vec3d(1, 0, 0);
            away = away.normalize();
            entity.damage(world, world.getDamageSources().magic(), damage);
            entity.setVelocity(away.x * 2.0 * multiplier, 0.4, away.z * 2.0 * multiplier);
            entity.velocityDirty = true;
            if (entity instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
            }
            Vec3d entityPosition = entity.getPos().add(0, entity.getHeight() / 2.0, 0);
            Vec3d trail = away.multiply(-0.3);
            for (int i = 0; i < 6; i++) {
                world.spawnParticles(
                        ParticleTypes.FLAME,
                        entityPosition.x + trail.x * i,
                        entityPosition.y + trail.y * i,
                        entityPosition.z + trail.z * i,
                        1,
                        0.05,
                        0.05,
                        0.05,
                        0.01
                );
            }
        }
    }
    private static void onTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            long currentTick = world.getTime();
            if (currentTick - lastResetTick >= COMBO_RESET_INTERVAL) {
                lastResetTick = currentTick;
                comboHits.clear();
                speedMultipliers.clear();
            }
            for (PlayerEntity player : world.getPlayers()) {
                boolean holdingWand = player.getMainHandStack().getItem() instanceof MagnetWandItem || player.getOffHandStack().getItem() instanceof MagnetWandItem;
                if (!holdingWand) continue;
                Vec3d playerPos = player.getPos().add(0, player.getHeight() / 2.0, 0);
                Box box = player.getBoundingBox().expand(PASSIVE_RANGE);
                world.getEntitiesByClass(ItemEntity.class, box, entity -> !entity.isRemoved()).forEach(item -> {
                    item.setPickupDelay(0);
                    Vec3d toward = playerPos.subtract(item.getPos());
                    double dist = toward.length();
                    if (dist < 0.5) return;
                    double speed = Math.min(PASSIVE_SPEED, 0.15 + dist * 0.05);
                    item.setVelocity(toward.normalize().multiply(speed));
                    item.velocityDirty = true;
                    if (currentTick % 3 == 0) {
                        world.spawnParticles(
                                ParticleTypes.ENCHANT,
                                item.getX(),
                                item.getY() + 0.1,
                                item.getZ(),
                                1,
                                0.05,
                                0.05,
                                0.05,
                                0.01
                        );
                    }
                });
                world.getEntitiesByClass(ExperienceOrbEntity.class, box, entity -> !entity.isRemoved()).forEach(orb -> {
                    Vec3d toward = playerPos.subtract(orb.getPos());
                    double dist = toward.length();
                    if (dist < 0.5) return;
                    double speed = Math.min(PASSIVE_SPEED, 0.15 + dist * 0.05);
                    orb.setVelocity(toward.normalize().multiply(speed));
                    orb.velocityDirty = true;
                    if (currentTick % 3 == 0) {
                        world.spawnParticles(
                                ParticleTypes.HAPPY_VILLAGER,
                                orb.getX(),
                                orb.getY() + 0.1,
                                orb.getZ(),
                                1,
                                0.05,
                                0.05,
                                0.05,
                                0.01
                        );
                    }
                });
            }
        }
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(MagnetPullManager::onTick);
        MagnetTogglePacket.initializeServer();
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            repelModeActive.remove(uuid);
            comboHits.remove(uuid);
            speedMultipliers.remove(uuid);
        });
    }
}