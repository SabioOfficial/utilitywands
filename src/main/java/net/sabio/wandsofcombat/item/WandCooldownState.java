package net.sabio.wandsofcombat.item;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.sabio.wandsofcombat.Wandsofcombat;

import java.util.*;

public class WandCooldownState extends SavedData {
    private final Map<String, Long> expiryMs;
    private WandCooldownState(Map<String, Long> expiryMs) {
        this.expiryMs = new HashMap<>(expiryMs);
    }
    public WandCooldownState() {
        this.expiryMs = new HashMap<>();
    }
    private static final Codec<WandCooldownState> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.LONG).xmap(
                    WandCooldownState::new,
                    state -> state.expiryMs
            );
    public static final SavedDataType<WandCooldownState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "wandsofcombat_cooldowns"),
            WandCooldownState::new,
            CODEC,
            null
    );
    public static WandCooldownState get(MinecraftServer server) {
        return server.getLevel(ServerLevel.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
    }
    public void save(UUID player, String key, int remainingTicks) {
        expiryMs.put(player + "_" + key, System.currentTimeMillis() + remainingTicks * 50L);
        setDirty();
    }
    public int getRemainingTicks(UUID player, String key) {
        long remaining = expiryMs.getOrDefault(player + "_" + key, 0L) - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 50L) : 0;
    }
}