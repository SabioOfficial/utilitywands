package net.sabio.wandsofcombat.item;

import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.*;

public class WandCooldownState extends PersistentState {
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
    public static final PersistentStateType<WandCooldownState> TYPE = new PersistentStateType<>(
            "wandsofcombat_cooldowns",
            WandCooldownState::new,
            CODEC,
            null
    );
    public static WandCooldownState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE);
    }
    public void save(UUID player, String key, int remainingTicks) {
        expiryMs.put(player + "_" + key, System.currentTimeMillis() + remainingTicks * 50L);
        markDirty();
    }
    public int getRemainingTicks(UUID player, String key) {
        long remaining = expiryMs.getOrDefault(player + "_" + key, 0L) - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 50L) : 0;
    }
}