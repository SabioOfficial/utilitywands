package net.sabio.wandsofcombat.item;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class LightningFireTracker {
    private static final Set<UUID> lightningStruckPlayers = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>())
    );

    public static void markLightningFire(PlayerEntity player) {
        lightningStruckPlayers.add(player.getUuid());
    }

    public static boolean isLightningFire(PlayerEntity player) {
        return lightningStruckPlayers.contains(player.getUuid());
    }

    public static void clear(PlayerEntity player) {
        lightningStruckPlayers.remove(player.getUuid());
    }
}