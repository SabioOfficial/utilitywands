package net.sabio.wandsofcombat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;

import java.util.function.Consumer;

public class MagnetWandItem extends Item {
    private static final float ATTACK_DAMAGE_BONUS = 2.0f;
    private static final float ATTACK_SPEED = -2.7f;
    private static final double ABILITY_PULL_RANGE = 12.0;
    private static final double REPEL_RANGE = 8.0;
    private static final float REPEL_DAMAGE = 6.0f;

    public MagnetWandItem(Properties settings) {
        super(ToolMaterial.DIAMOND.applyToolProperties(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !attacker.level().isClientSide()) {
            MagnetPullManager.recordHit(player, target);
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!ManaManager.hasEnough(player, ManaCosts.MAGNET_WAND)) return InteractionResult.FAIL;
        if (!world.isClientSide()) {
            if (!ManaManager.tryConsume(player, ManaCosts.MAGNET_WAND)) return InteractionResult.FAIL;
            if (MagnetPullManager.isRepelMode(player)) {
                MagnetPullManager.doRepel(player, REPEL_RANGE, REPEL_DAMAGE);
            } else {
                MagnetPullManager.doAbilityPull(player, ABILITY_PULL_RANGE);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, displayComponent, tooltip, flag);

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_PASSIVE))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Magnification", ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Pull dropped items and XP orbs").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("towards you in an " + (int) ABILITY_PULL_RANGE + " block radius.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_COMBO))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Magnetization", ChatFormatting.GOLD))
                .append(Component.literal(" \uD83D\uDDE11").withStyle(ChatFormatting.WHITE)));
        tooltip.accept(Component.literal("Increases pull speed by 8% for one entity,").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("which resets every 2 minutes.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_ABILITY))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.RIGHT_CLICK_ICON))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Attraction", ChatFormatting.RED))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.MANA_ICON))
                .append(Component.literal(String.valueOf(ManaCosts.MAGNET_WAND)).withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Pulls entities in a " + (int) ABILITY_PULL_RANGE + " block radius").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("towards you.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_ALT_ABILITY))
                .append(Component.literal(" "))
                .append(Component.literal("R").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.WHITE))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.RIGHT_CLICK_ICON))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Repulsion", ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.MANA_ICON))
                .append(Component.literal(String.valueOf(ManaCosts.MAGNET_WAND)).withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Knocks back entities in an " + (int) REPEL_RANGE + " block radius,").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("doing ❤" + ((int) (REPEL_DAMAGE / 2)) + " to the repelled entities.").withStyle(ChatFormatting.GRAY));
    }
}