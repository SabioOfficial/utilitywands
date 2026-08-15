package net.sabio.wandsofcombat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class MagmaWandItem extends Item {
    private static final float ATTACK_DAMAGE_BONUS = 5.5f; // total: 9.5
    private static final float ATTACK_SPEED = -3.1f;
    public static final int ABILITY_COOLDOWN = 1200; // 1 min
    public static final int ULTIMATE_COOLDOWN = 1800; // 1:30mins; yeah we having these now
    public static final int ABILITY_DURATION = 900; // 45 seconds
    public static final int ULTIMATE_DURATION = 600; // 30 seconds
    public static final Map<UUID, Integer> hitCounters = new HashMap<>();
    public MagmaWandItem(Properties settings) {
        super(ToolMaterial.DIAMOND.applyToolProperties(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }
    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        int cost = player.isCrouching() ? ManaCosts.MAGMA_ULTIMATE : ManaCosts.MAGMA_ABILITY;
        if (!ManaManager.hasEnough(player, cost)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof MagmaWandItem) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide()) {
            if (player.isCrouching()) {
                MagmaWandHandler.tryActivateUltimate(player);
            } else {
                MagmaWandHandler.tryActivateAbility(player);
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !attacker.level().isClientSide()) {
            UUID uuid = player.getUUID();
            int hits = hitCounters.getOrDefault(uuid, 0) + 1;
            if (hits >= 5) {
                hitCounters.put(uuid, 0);
                if (attacker.level() instanceof ServerLevel ServerLevel) {
                    ServerLevel.sendParticles(
                            new DustParticleOptions(0xDC4810, 2.0f),
                            attacker.getX(),
                            attacker.getY() + 1.0,
                            attacker.getZ(),
                            10,
                            0.3,
                            0.3,
                            0.3,
                            0
                    );
                    ServerLevel.sendParticles(
                            ParticleTypes.FLAME,
                            attacker.getX(),
                            attacker.getY() + 1.0,
                            attacker.getZ(),
                            8,
                            0.2,
                            0.3,
                            0.2,
                            0.05
                    );
                }
                MagmaWandHandler.launchFireball(player, target);
            } else {
                hitCounters.put(uuid, hits);
            }
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, displayComponent, tooltip, flag);

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_PASSIVE))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Magma Shield", ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Gain 2 absorption hearts (non-stacking)").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("after 30 seconds of not taking any damage.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("Once the absorption hearts have been used,").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("it causes all nearby entities within 6").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("block radius to be knocked away from the").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("player. Then, the player will receive").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("multiple positive effects.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_COMBO))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Charged-up Fury", ChatFormatting.GOLD))
                .append(Component.literal(" \uD83D\uDDE15").withStyle(ChatFormatting.WHITE)));
        tooltip.accept(Component.literal("Launches a fireball that does ❤4 to").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("one enemy.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_ABILITY))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.RIGHT_CLICK_ICON))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Ablazed Healing", ChatFormatting.RED))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.MANA_ICON))
                .append(Component.literal(String.valueOf(ManaCosts.MAGMA_ABILITY)).withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Every tick on fire, heal the player").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("by ❤0.05 and extinguishes fire.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("Lasts for 45 seconds.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_ULTIMATE))
                .append(Component.literal(" "))
                .append(Component.literal("Crouch").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.WHITE))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.RIGHT_CLICK_ICON))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Fire Ring", ChatFormatting.YELLOW))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.MANA_ICON))
                .append(Component.literal(String.valueOf(ManaCosts.MAGMA_ULTIMATE)).withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Spawns a ring of fire that moves").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("with you. Lasts for 30 seconds.").withStyle(ChatFormatting.GRAY));
    }
}
