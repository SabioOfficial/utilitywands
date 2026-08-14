package net.sabio.wandsofcombat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.sabio.wandsofcombat.Wandsofcombat;

public final class TooltipIcons {
    private TooltipIcons() {
    }

    public static final Identifier FONT = Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "tooltip_icons");

    public static final String BADGE_PASSIVE = "\uE000\uE00F\uE001\uE00F\uE002\uE00F\uE003\uE00F\uE006";
    public static final String BADGE_COMBO = "\uE010\uE00F\uE011\uE00F\uE012\uE00F\uE013\uE00F\uE014";
    public static final String BADGE_ABILITY = "\uE020\uE00F\uE021\uE00F\uE022\uE00F\uE023\uE00F\uE024";
    public static final String BADGE_ALT_ABILITY = "\uE030\uE00F\uE031\uE00F\uE032";
    public static final String MANA_ICON = "\uE004";
    public static final String RIGHT_CLICK_ICON = "\uE005";
    public static final String SKELETON_ICON = "\uE007";

    public static Component icon(String glyphs) {
        return Component.literal(glyphs).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(FONT)));
    }

    public static Component title(String name, ChatFormatting color) {
        return Component.literal(name).withStyle(ChatFormatting.BOLD, color);
    }
}