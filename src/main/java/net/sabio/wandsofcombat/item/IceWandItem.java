package net.sabio.wandsofcombat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;

import java.util.List;
import java.util.function.Consumer;

public class IceWandItem extends Item {
    private static final double LITE_RANGE = 8.0;
    private static final double FULL_RANGE = 12.0;
    private static final int FREEZE_DURATION = 80; // 4 seconds
    private static final int POWDER_SNOW_FREEZE_DURATION = 300; // 15 seconds; power snow visuals kick in at >140 ticks
    private static final int LITE_SLOWNESS_AMPLIFIER = 126; // slowness 127
    private static final int FULL_SLOWNESS_AMPLIFIER = 1; // slowness 2
    private static final float ATTACK_SPEED = -3.3f;
    private static final float ATTACK_DAMAGE_BONUS = 8.0f; // 12 total attack damage

    private boolean applyAbility(Level world, Player player) {
        boolean hitPlayer = false;
        if (world instanceof ServerLevel castSoundLevel) {
            castSoundLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f, castSoundLevel.getRandom().nextLong());
            castSoundLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.6f, 1.6f, castSoundLevel.getRandom().nextLong());
        }
        AABB mobBox = player.getBoundingBox().inflate(LITE_RANGE);
        List<LivingEntity> nearbyMobs = world.getEntitiesOfClass(LivingEntity.class, mobBox, entity -> !(entity instanceof Player) && !entity.isRemoved());
        for (LivingEntity mob : nearbyMobs) {
            mob.setTicksFrozen(POWDER_SNOW_FREEZE_DURATION);
            mob.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    FREEZE_DURATION,
                    LITE_SLOWNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            mob.setDeltaMovement(Vec3.ZERO);
            mob.hurtMarked = true;
            mob.setLastHurtByPlayer(player, mob.tickCount);
            if (mob instanceof Mob livingMob) {
                livingMob.setTarget(player);
            }

            if (world instanceof ServerLevel ServerLevel) {
                ServerLevel.playSeededSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.POWDER_SNOW_HIT, SoundSource.PLAYERS, 0.8f, 1.0f, ServerLevel.getRandom().nextLong());
                ServerLevel.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        mob.getX(),
                        mob.getY() + 1.0,
                        mob.getZ(),
                        20,
                        0.4,
                        0.6,
                        0.4,
                        0.05
                );
                ServerLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                        mob.getX(),
                        mob.getY() + 0.5,
                        mob.getZ(),
                        15,
                        0.4,
                        0.4,
                        0.4,
                        0.1
                );
                ServerLevel.sendParticles(
                        new DustParticleOptions(0x80D9FF, 2.0f),
                        mob.getX(),
                        mob.getY() + 1.0,
                        mob.getZ(),
                        10,
                        0.3,
                        0.5,
                        0.3,
                        0
                );
            }
        }
        AABB playerBox = player.getBoundingBox().inflate(FULL_RANGE);
        List<Player> nearbyPlayers = world.getEntitiesOfClass(
                Player.class,
                playerBox,
                entity -> entity != player && !entity.isRemoved()
        );
        for (Player target : nearbyPlayers) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    120,
                    FULL_SLOWNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            target.setTicksFrozen(POWDER_SNOW_FREEZE_DURATION);
            hitPlayer = true;
            if (world instanceof ServerLevel ServerLevel) {
                ServerLevel.playSeededSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.POWDER_SNOW_HIT, SoundSource.PLAYERS, 0.8f, 0.9f, ServerLevel.getRandom().nextLong());
                ServerLevel.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        target.getX(),
                        target.getY() + 1.0,
                        target.getZ(),
                        20,
                        0.4,
                        0.6,
                        0.4,
                        0.05
                );
                ServerLevel.sendParticles(
                        new DustParticleOptions(0x80D9FF, 2.0f),
                        target.getX(),
                        target.getY() + 1.0,
                        target.getZ(),
                        10,
                        0.3,
                        0.5,
                        0.3,
                        0
                );
            }
        }

        if (world instanceof ServerLevel ServerLevel) {
            for (int i = 0; i < 24; i++) {
                double angle = (2.0 * Math.PI / 24) * i;
                double range = hitPlayer ? FULL_RANGE : LITE_RANGE;
                double posX = player.getX() + range * Math.cos(angle);
                double posZ = player.getZ() + range * Math.sin(angle);
                ServerLevel.sendParticles(
                        new DustParticleOptions(0x80D9FF, 1.5f),
                        posX,
                        player.getY() + 0.5,
                        posZ,
                        2,
                        0,
                        0.3,
                        0,
                        0
                );
                ServerLevel.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        posX,
                        player.getY() + 0.5,
                        posZ,
                        1,
                        0,
                        0.2,
                        0,
                        0.01
                );
            }
        }

        return hitPlayer;
    }

    public IceWandItem(Properties settings) {
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
        if (!ManaManager.hasEnough(player, ManaCosts.ICE_WAND_LITE)) {
            ManaManager.playInsufficientManaSound(player);
            return InteractionResult.FAIL;
        }
        if (!world.isClientSide()) {
            if (!ManaManager.tryConsume(player, ManaCosts.ICE_WAND_LITE)) {
                return InteractionResult.FAIL;
            }
            boolean hitPlayer = applyAbility(world, player);
            if (hitPlayer) {
                int extraCost = ManaCosts.ICE_WAND_FULL - ManaCosts.ICE_WAND_LITE;
                ManaManager.tryConsume(player, extraCost);
            }
        }

        return InteractionResult.SUCCESS;
    }


    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !attacker.level().isClientSide()) {
            IceWandComboHandler.onHit(player, target);
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, displayComponent, tooltip, flag);

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_COMBO))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Spiked Ice", ChatFormatting.GOLD))
                .append(Component.literal(" \uD83D\uDDE16").withStyle(ChatFormatting.WHITE)));
        tooltip.accept(Component.literal("A powerful building energy releases").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("from your body, doing ❤11 to the enemy").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("if the ice successfully hits them. It has").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("a ~7 block range.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_ABILITY))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.RIGHT_CLICK_ICON))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Freeze", ChatFormatting.RED))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.MANA_ICON))
                .append(Component.literal(ManaCosts.ICE_WAND_LITE + "-" + ManaCosts.ICE_WAND_FULL).withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("For mobs, this ability has an 8 block radius").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("and gives them Slowness 127 for 4 seconds.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("For players, this ability has a 12 block").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("radius and gives them Slowness 2 for 4").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("seconds. Both damages the enemies.").withStyle(ChatFormatting.GRAY));
    }
}
