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
    private final Map<UUID, Integer> manaPoints;

    private ManaState(Map<UUID, Integer> manaPoints) {
        this.manaPoints = new HashMap<>(manaPoints);
    }

    public ManaState() {
        this.manaPoints = new HashMap<>();
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
                manaState.manaPoints.forEach((key, value) -> result.put(key.toString(), value));
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

    public int getPoints(UUID player) {
        return manaPoints.getOrDefault(player, ManaManager.MAX_MANA_POINTS);
    }

    public void setPoints(UUID player, int points) {
        manaPoints.put(player, Math.clamp(points, 0, ManaManager.MAX_MANA_POINTS));
        setDirty();
    }

    public void remove(UUID player) {
        manaPoints.remove(player);
        setDirty();
    }
}
