package net.sabio.wandsofcombat.item;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandCooldownState extends PersistentState {
    private final Map<String, Long> expiryMs = new HashMap<>();

    public static WandCooldownState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(new PersistentState.Type<>(
                        WandCooldownState::new,
                        WandCooldownState::fromNbt,
                        null
                ), "wandsofcombat_cooldowns");
    }

    public void save(UUID player, String key, int remainingTicks) {
        expiryMs.put(player + "_" + key, System.currentTimeMillis() + remainingTicks * 50L);
        markDirty();
    }

    public int getRemainingTicks(UUID player, String key) {
        long remaining = expiryMs.getOrDefault(player + "_" + key, 0L) - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 50L) : 0;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound entries = new NbtCompound();
        for (Map.Entry<String, Long> e : expiryMs.entrySet()) {
            if (e.getValue() > System.currentTimeMillis()) {
                entries.putLong(e.getKey(), e.getValue());
            }
        }
        nbt.put("entries", entries);
        return nbt;
    }

    public static WandCooldownState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        WandCooldownState state = new WandCooldownState();
        NbtCompound entries = nbt.getCompound("entries");
        for (String key : entries.getKeys()) {
            long val = entries.getLong(key);
            if (val > System.currentTimeMillis()) state.expiryMs.put(key, val);
        }
        return state;
    }
}