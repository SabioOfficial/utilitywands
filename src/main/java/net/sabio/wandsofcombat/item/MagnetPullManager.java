package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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

    private static void spawnRing(ServerLevel world, Vec3 center, double radius, int color) {
        int points = 32;
        DustParticleOptions dustParticleEffect = new DustParticleOptions(color, 1.2f);
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2 / points) * i;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.sendParticles(dustParticleEffect, x, center.y, z, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnSpiralAround(ServerLevel world, Vec3 from, Vec3 to) {
        int steps = 12;
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            double x = from.x + (to.x - from.x) * t;
            double y = from.y + (to.y - from.y) * t;
            double z = from.z + (to.z - from.z) * t;
            double angle = t * Math.PI * 4;
            double offset = 0.4 * (1 - t);
            world.sendParticles(
                    ParticleTypes.WITCH,
                    x + Math.cos(angle) * offset,
                    y,
                    z + Math.sin(angle) * offset,
                    1, 0, 0, 0, 0
            );
        }
    }

    public static boolean isRepelMode(Player player) {
        return repelModeActive.contains(player.getUUID());
    }
    public static void toggleRepelMode(Player player) {
        UUID uuid = player.getUUID();
        boolean nowRepel = !repelModeActive.remove(uuid);
        if (nowRepel) repelModeActive.add(uuid);
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof MagnetWandItem) {
            if (nowRepel) {
                stack.set(ModItems.MAGNET_REPEL_MODE, true);
            } else {
                stack.remove(ModItems.MAGNET_REPEL_MODE);
            }
        }
    }
    public static void recordHit(Player attacker, LivingEntity target) {
        UUID attackerUuid = attacker.getUUID();
        UUID targetUuid = target.getUUID();
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
    private static float getSpeedMultiplier(Player player, LivingEntity entity) {
        Map<UUID, Float> multipliers = speedMultipliers.get(player.getUUID());
        if (multipliers == null) return 1.0f;
        return multipliers.getOrDefault(entity.getUUID(), 1.0f);
    }
    public static void doAbilityPull(Player player, double range) {
        if (!(player.level() instanceof ServerLevel world)) return;
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && !entity.isRemoved());
        Vec3 playerPos = player.position();
        spawnRing(world, playerPos.add(0, player.getBbHeight() / 2.0, 0), range, 0x4488FF);
        for (LivingEntity entity : entities) {
            float multiplier = getSpeedMultiplier(player, entity);
            Vec3 toward = new Vec3(playerPos.x - entity.getX(), 0, playerPos.z - entity.getZ()).normalize();
            entity.setDeltaMovement(toward.scale(1.5 * multiplier).add(0, 0.2, 0));
            entity.hurtMarked = true;
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
            }
            Vec3 entityPosition = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                world.sendParticles(
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
            spawnSpiralAround(world, entity.position(), playerPos);
        }
    }

    public static void doRepel(Player player, double range, float damage) {
        if (!(player.level() instanceof ServerLevel world)) return;
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && !entity.isRemoved());
        Vec3 playerPosition = player.position();
        spawnRing(world, playerPosition.add(0, player.getBbHeight() / 2.0, 0), range, 0xFF4422);
        world.sendParticles(ParticleTypes.EXPLOSION, playerPosition.x, playerPosition.y + 1, playerPosition.z, 3, 0.3, 0.3, 0.3, 0.1);
        world.sendParticles(ParticleTypes.FLAME, playerPosition.x, playerPosition.y + 1, playerPosition.z, 20, 0.5, 0.5, 0.5, 0.15);
        for (LivingEntity entity : entities) {
            float multiplier = getSpeedMultiplier(player, entity);
            Vec3 away = new Vec3(entity.getX() - playerPosition.x, 0, entity.getZ() - playerPosition.z);
            if (away.horizontalDistance() < 0.01) away = new Vec3(1, 0, 0);
            away = away.normalize();
            entity.hurtServer(world, world.damageSources().magic(), damage);
            entity.setDeltaMovement(away.x * 2.0 * multiplier, 0.4, away.z * 2.0 * multiplier);
            entity.hurtMarked = true;
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
            }
            Vec3 entityPosition = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
            Vec3 trail = away.scale(-0.3);
            for (int i = 0; i < 6; i++) {
                world.sendParticles(
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
        for (ServerLevel world : server.getAllLevels()) {
            long currentTick = world.getGameTime();
            if (currentTick - lastResetTick >= COMBO_RESET_INTERVAL) {
                lastResetTick = currentTick;
                comboHits.clear();
                speedMultipliers.clear();
            }
            for (Player player : world.players()) {
                boolean holdingWand = player.getMainHandItem().getItem() instanceof MagnetWandItem || player.getOffhandItem().getItem() instanceof MagnetWandItem;
                if (!holdingWand) continue;
                Vec3 playerPos = player.position().add(0, player.getBbHeight() / 2.0, 0);
                AABB box = player.getBoundingBox().inflate(PASSIVE_RANGE);
                world.getEntitiesOfClass(ItemEntity.class, box, entity -> !entity.isRemoved()).forEach(item -> {
                    item.setPickUpDelay(0);
                    Vec3 toward = playerPos.subtract(item.position());
                    double dist = toward.length();
                    if (dist < 0.5) return;
                    double speed = Math.min(PASSIVE_SPEED, 0.15 + dist * 0.05);
                    item.setDeltaMovement(toward.normalize().scale(speed));
                    item.hurtMarked = true;
                    if (currentTick % 3 == 0) {
                        world.sendParticles(
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
                world.getEntitiesOfClass(ExperienceOrb.class, box, entity -> !entity.isRemoved()).forEach(orb -> {
                    Vec3 toward = playerPos.subtract(orb.position());
                    double dist = toward.length();
                    if (dist < 0.5) return;
                    double speed = Math.min(PASSIVE_SPEED, 0.15 + dist * 0.05);
                    orb.setDeltaMovement(toward.normalize().scale(speed));
                    orb.hurtMarked = true;
                    if (currentTick % 3 == 0) {
                        world.sendParticles(
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
            UUID uuid = handler.player.getUUID();
            repelModeActive.remove(uuid);
            comboHits.remove(uuid);
            speedMultipliers.remove(uuid);
        });
    }
}