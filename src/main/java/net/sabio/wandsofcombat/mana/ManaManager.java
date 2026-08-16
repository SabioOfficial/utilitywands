package net.sabio.wandsofcombat.mana;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.sabio.wandsofcombat.effect.ModEffects;
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
            nextRegenTick.put(uuid, player.level().getGameTime() + effectiveRegenInterval(player));
            syncToClient(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            ManaState.get(server).setPoints(uuid, manaPoints.getOrDefault(uuid, MAX_MANA_POINTS));
            manaPoints.remove(uuid);
            nextRegenTick.remove(uuid);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ManaState state = ManaState.get(server);
            for (Map.Entry<UUID, Integer> entry : manaPoints.entrySet()) {
                state.setPoints(entry.getKey(), entry.getValue());
            }
        });
    }

    private static void onTick(MinecraftServer server) {
        long currentTick = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            int current = manaPoints.getOrDefault(uuid, MAX_MANA_POINTS);
            if (current >= MAX_MANA_POINTS) continue;
            long next = nextRegenTick.getOrDefault(uuid, currentTick + effectiveRegenInterval(player));
            if (currentTick >= next) {
                setPoints(player, current + 1);
                nextRegenTick.put(uuid, currentTick + effectiveRegenInterval(player));
            }
        }
    }

    public static int getPoints(Player player) {
        return manaPoints.getOrDefault(player.getUUID(), MAX_MANA_POINTS);
    }

    public static boolean hasEnough(Player player, int costPoints) {
        if (player.isCreative()) {
            return true;
        }
        return manaPoints.getOrDefault(player.getUUID(), MAX_MANA_POINTS) >= costPoints;
    }

    public static void playInsufficientManaSound(Player player) {
        player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
    }

    public static boolean tryConsume(Player player, int costPoints) {
        if (player.isCreative()) {
            return true;
        }
        UUID uuid = player.getUUID();
        int current = manaPoints.getOrDefault(uuid, MAX_MANA_POINTS);
        if (current < costPoints) return false;
        setPoints(player, current - costPoints);
        nextRegenTick.put(uuid, player.level().getGameTime() + effectiveRegenInterval(player));
        return true;
    }

    private static void setPoints(Player player, int points) {
        int clamped = Math.clamp(points, 0, MAX_MANA_POINTS);
        manaPoints.put(player.getUUID(), clamped);
        if (player instanceof ServerPlayer serverPlayer) syncToClient(serverPlayer);
    }

    private static int effectiveRegenInterval(Player player) {
        var effect = player.getEffect(ModEffects.MANA_REGENERATION);
        if (effect == null) return REGEN_INTERVAL_TICKS;
        int amplifier = effect.getAmplifier();
        int divisor = amplifier + 2;
        return Math.max(1, REGEN_INTERVAL_TICKS / divisor);
    }

    private static void syncToClient(ServerPlayer player) {
        ServerPlayNetworking.send(player, new ManaSyncPacket(
                manaPoints.getOrDefault(player.getUUID(), MAX_MANA_POINTS),
                MAX_MANA_POINTS
        ));
    }
}
