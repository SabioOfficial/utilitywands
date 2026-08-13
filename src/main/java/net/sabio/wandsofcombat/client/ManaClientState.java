package net.sabio.wandsofcombat.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.sabio.wandsofcombat.mana.ManaManager;

@Environment(EnvType.CLIENT)
public final class ManaClientState {
    private static int points = ManaManager.MAX_MANA_POINTS;
    private static int maxPoints = ManaManager.MAX_MANA_POINTS;

    private ManaClientState() {
    }

    public static void update(int newPoints, int newMaxPoints) {
        points = newPoints;
        maxPoints = newMaxPoints;
    }

    public static int getPoints() {
        return points;
    }

    public static int getMaxPoints() {
        return maxPoints;
    }
}
