package net.sabio.wandsofcombat.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.sabio.wandsofcombat.Wandsofcombat;
import net.sabio.wandsofcombat.mana.ManaCosts;

@Environment(EnvType.CLIENT)
public final class ManaHudRenderer {
    private static final int ICON_SIZE = 9;
    private static final int ICON_SPACING = 8;
    private static final int ICONS_PER_ROW = 10;
    private static final int ROW_HEIGHT = 10;
    private static final int POINTS_PER_ICON = 4;
    private static final int HEALTH_PER_HEART_ROW = 20;
    private static final int BLINK_INTERVAL_TICKS = 10;

    private static final Identifier TEX_FULL = tex("mana_full");
    private static final Identifier TEX_GIBBOUS = tex("mana_gibbous");
    private static final Identifier TEX_HALF = tex("mana_half");
    private static final Identifier TEX_CRESCENT = tex("mana_crescent");
    private static final Identifier TEX_EMPTY = tex("mana_empty");

    private static final Identifier TEX_FULL_BLINK = tex("mana_full_blinking");
    private static final Identifier TEX_GIBBOUS_BLINK = tex("mana_gibbous_blinking");
    private static final Identifier TEX_HALF_BLINK = tex("mana_half_blinking");
    private static final Identifier TEX_CRESCENT_BLINK = tex("mana_crescent_blinking");
    private static final Identifier TEX_EMPTY_BLINK = tex("mana_empty_blinking");

    private ManaHudRenderer() {
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "textures/gui/sprites/hud/" + name + ".png");
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.FOOD_BAR,
                Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "mana_bar"),
                ManaHudRenderer::render
        );
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) {
            return;
        }

        int points = ManaClientState.getPoints();
        int maxPoints = ManaClientState.getMaxPoints();
        if (maxPoints <= 0) {
            return;
        }

        int iconCount = (maxPoints + POINTS_PER_ICON - 1) / POINTS_PER_ICON;
        int manaRows = (iconCount + ICONS_PER_ROW - 1) / ICONS_PER_ROW;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int left = screenWidth / 2 - 91;
        int healthBarRows = healthBarRowCount(player);
        int extraHealthRows = healthBarRows - 1;
        int bottom = screenHeight - 39 - (extraHealthRows * ROW_HEIGHT) - (manaRows * ROW_HEIGHT);

        int cost = ManaCosts.nextCastCost(player);
        boolean blinkOn = cost > 0 && (player.tickCount / BLINK_INTERVAL_TICKS) % 2 == 0;
        int previewPoints = cost > 0 ? Math.max(0, points - cost) : points;

        for (int icon = 0; icon < iconCount; icon++) {
            int row = icon / ICONS_PER_ROW;
            int col = icon % ICONS_PER_ROW;
            int x = left + col * ICON_SPACING;
            int y = bottom - row * ROW_HEIGHT;

            int iconStart = icon * POINTS_PER_ICON;
            int currentFill = Math.clamp(points - iconStart, 0, POINTS_PER_ICON);
            int previewFill = Math.clamp(previewPoints - iconStart, 0, POINTS_PER_ICON);
            boolean affected = currentFill != previewFill;

            int displayFill = (affected && blinkOn) ? previewFill : currentFill;
            boolean showBlinkTexture = affected && blinkOn;

            Identifier texture = showBlinkTexture
                ? switch (displayFill) {
                    case 4 -> TEX_FULL_BLINK;
                    case 3 -> TEX_GIBBOUS_BLINK;
                    case 2 -> TEX_HALF_BLINK;
                    case 1 -> TEX_CRESCENT_BLINK;
                    default -> TEX_EMPTY_BLINK;
                }
                : switch (displayFill) {
                    case 4 -> TEX_FULL;
                    case 3 -> TEX_GIBBOUS;
                    case 2 -> TEX_HALF;
                    case 1 -> TEX_CRESCENT;
                    default -> TEX_EMPTY;
                };

            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }

    private static int healthBarRowCount(Player player) {
        float health = player.getHealth();
        float absorption = player.getAbsorptionAmount();
        int total = Math.round(health + absorption);
        int rows = (total + HEALTH_PER_HEART_ROW - 1) / HEALTH_PER_HEART_ROW;
        return Math.max(1, rows);
    }
}
