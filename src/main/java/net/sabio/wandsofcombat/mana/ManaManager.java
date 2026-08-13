package net.sabio.wandsofcombat.mana;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.sabio.wandsofcombat.network.ManaSyncPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManaManager {
    public static final int MAX_MANA_POINTS = 12;
    public static final int REGEN_INTERVAL_TICKS = 200;
    private static final int REGEN_PAUSE_AFTER_SPEND_TICKS = 20;

    private static final Map<UUID, Integer> manaPoints = new HashMap<>();
    private static final Map<UUID, Long> nextRegenTick = new HashMap<>();

    private ManaManager() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(ManaManager::onTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            UUID uuid = player.getUUID();
            int loaded = ManaState.get(server).getPoints(uuid);
            manaPoints.put(uuid, loaded);
            nextRegenTick.put(uuid, player.level().getGameTime() + REGEN_INTERVAL_TICKS);
            syncToClient(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            ManaState.get(server).setPoints(uuid, manaPoints.getOrDefault(uuid, MAX_MANA_POINTS));
            manaPoints.remove(uuid);
            nextRegenTick.remove(uuid);
        });
    }

    private static void onTick(MinecraftServer server) {
        long currentTick = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            int current = manaPoints.getOrDefault(uuid, MAX_MANA_POINTS);
            if (current >= MAX_MANA_POINTS) continue;
            long next = nextRegenTick.getOrDefault(uuid, currentTick + REGEN_INTERVAL_TICKS);
            if (currentTick >= next) {
                setPoints(player, current + 1);
                nextRegenTick.put(uuid, currentTick + REGEN_INTERVAL_TICKS);
            }
        }
    }

    public static int getPoints(Player player) {
        return manaPoints.getOrDefault(player.getUUID(), MAX_MANA_POINTS);
    }

    public static boolean hasEnough(Player player, int costPoints) {
        return manaPoints.getOrDefault(player.getUUID(), MAX_MANA_POINTS) >= costPoints;
    }

    public static boolean tryConsume(Player player, int costPoints) {
        UUID uuid = player.getUUID();
        int current = manaPoints.getOrDefault(uuid, MAX_MANA_POINTS);
        if (current < costPoints) return false;
        setPoints(player, current - costPoints);
        nextRegenTick.put(uuid, player.level().getGameTime() + REGEN_INTERVAL_TICKS);
        return true;
    }

    private static void setPoints(Player player, int points) {
        int clamped = Math.clamp(points, 0, MAX_MANA_POINTS);
        manaPoints.put(player.getUUID(), clamped);
        if (player instanceof ServerPlayer serverPlayer) syncToClient(serverPlayer);
    }

    private static void syncToClient(ServerPlayer player) {
        ServerPlayNetworking.send(player, new ManaSyncPacket(
                manaPoints.getOrDefault(player.getUUID(), MAX_MANA_POINTS),
                MAX_MANA_POINTS
        ));
    }
}
