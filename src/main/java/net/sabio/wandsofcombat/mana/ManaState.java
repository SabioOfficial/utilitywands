package net.sabio.wandsofcombat.mana;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.sabio.wandsofcombat.Wandsofcombat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManaState extends SavedData {
    private final Map<UUID, Integer> manaQuarters;

    private ManaState(Map<UUID, Integer> manaQuarters) {
        this.manaQuarters = new HashMap<>(manaQuarters);
    }

    public ManaState() {
        this.manaQuarters = new HashMap<>();
    }

    private static final Codec<ManaState> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(
            stringIntegerMap -> {
                Map<UUID, Integer> result = new HashMap<>();
                stringIntegerMap.forEach((key, value) -> {
                    try {
                        result.put(UUID.fromString(key), value);
                    } catch (IllegalArgumentException ignored) {}
                });
                return new ManaState(result);
            },
            manaState -> {
                Map<String, Integer> result = new HashMap<>();
                manaState.manaQuarters.forEach((key, value) -> result.put(key.toString(), value));
                return result;
            }
    );

    public static final SavedDataType<ManaState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "wandsofcombat_mana"),
            ManaState::new,
            CODEC,
            null
    );

    public static ManaState get(MinecraftServer server) {
        return server.getLevel(ServerLevel.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
    }

    public int getQuarters(UUID player) {
        return manaQuarters.getOrDefault(player, ManaManager.MAX_MANA_QUARTERS);
    }

    public void setQuarters(UUID player, int quarters) {
        manaQuarters.put(player, Math.clamp(quarters, 0, ManaManager.MAX_MANA_QUARTERS));
        setDirty();
    }

    public void remove(UUID player) {
        manaQuarters.remove(player);
        setDirty();
    }
}
