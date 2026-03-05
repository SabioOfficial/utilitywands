package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

// may i speak to your manager please
public class MagnetPullManager {
    private static final double MAX_SPEED = 1.5; // max velocity magnitude applied each tick (blocks per tick)
    private static final int MAX_PULL_DURATION = 100; // max time in ticks before it gives up if not pulled yet (5 seconds)
    private static class TrackedEntity {
        final UUID uuid;
        final ServerWorld world;
        TrackedEntity(UUID uuid, ServerWorld world) {
            this.uuid = uuid;
            this.world = world;
        }
    }
    private static class PullSession {
        final UUID playerUuid;
        final List<TrackedEntity> entities;
        int remainingTicks;
        PullSession(UUID playerUuid, List<TrackedEntity> entities, int remainingTicks) {
            this.playerUuid = playerUuid;
            this.entities = entities;
            this.remainingTicks = remainingTicks;
        }
    }
    private static final List<PullSession> activeSessions = new ArrayList<>();
    private static void cleanupSession(PullSession session) {
        for (TrackedEntity trackedEntity : session.entities) {
            Entity entity = trackedEntity.world.getEntity(trackedEntity.uuid);
            if (entity != null && !entity.isRemoved()) {
                entity.noClip = false;
                entity.setNoGravity(false);
            }
        }
    }
    private static boolean tickSession(PullSession session, MinecraftServer server) {
        PlayerEntity player = null;
        for (ServerWorld world : server.getWorlds()) {
            player = world.getPlayerByUuid(session.playerUuid);
            if (player != null) break;
        }
        if (player == null || session.remainingTicks <= 0) {
            cleanupSession(session);
            return true;
        }
        session.remainingTicks--;
        Vec3d targetPosition = player.getEntityPos().add(0.0, player.getHeight() / 2.0, 0.0);
        Iterator<TrackedEntity> iterator = session.entities.iterator();
        while (iterator.hasNext()) {
            TrackedEntity trackedEntity = iterator.next();
            Entity entity = trackedEntity.world.getEntity(trackedEntity.uuid);
            if (entity == null || entity.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (entity instanceof ItemEntity itemEntity && itemEntity.getStack().isEmpty()) {
                iterator.remove();
                continue;
            }
            Vec3d toPlayer = targetPosition.subtract(entity.getEntityPos());
            double distance = toPlayer.length();
            if (distance < 0.75) {
                entity.noClip = false;
                entity.setNoGravity(false);
                iterator.remove();
                continue;
            }
            double speed = Math.min(MAX_SPEED, 0.3 + distance * 0.08);
            entity.noClip = true;
            entity.setNoGravity(true);
            entity.setVelocity(toPlayer.normalize().multiply(speed));
            entity.velocityDirty = true;
        }
        return session.entities.isEmpty();
    }
    private static void onTick(MinecraftServer server) {
        activeSessions.removeIf(session -> tickSession(session, server));
    }

    public static void startPull(PlayerEntity player, List<Entity> entities) {
        if (!(player.getEntityWorld() instanceof ServerWorld serverWorld)) return;
        List<TrackedEntity> tracked = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.setPickupDelay(0);
            }
            tracked.add(new TrackedEntity(entity.getUuid(), serverWorld));
        }
        activeSessions.add(new PullSession(player.getUuid(), tracked, MAX_PULL_DURATION));
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(MagnetPullManager::onTick);
    }
}
