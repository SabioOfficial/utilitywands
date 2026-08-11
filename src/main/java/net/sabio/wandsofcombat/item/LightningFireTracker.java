package net.sabio.wandsofcombat.item;

import net.minecraft.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class LightningFireTracker {
    private static final Set<UUID> lightningStruckPlayers = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>())
    );

    public static void markLightningFire(Player player) {
        lightningStruckPlayers.add(player.getUUID());
    }

    public static boolean isLightningFire(ServerPlayer player) {
        return lightningStruckPlayers.contains(player.getUUID());
    }

    public static void clear(ServerPlayer player) {
        lightningStruckPlayers.remove(player.getUUID());
    }
}