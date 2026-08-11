package net.sabio.wandsofcombat.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.Player;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerLevel;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PhantomWandItem extends Item {
    public static final int ABILITY_DURATION = 120; // 6 seconds
    public static final int ABILITY_COOLDOWN = 1200 + ABILITY_DURATION; // 1 minute
    private static final float ATTACK_DAMAGE_BONUS = 2.0f; // 6 total attack damage
    private static final float ATTACK_SPEED = -1.5f;
    public static final Set<UUID> phantomPlayers = new HashSet<>();
    public static final Set<UUID> pullingPlayers = new HashSet<>();
    public PhantomWandItem(Settings settings) {
        super(ToolMaterial.DIAMOND.applyToolSettings(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }
    @Override
    public ActionResult use(World world, Player player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.FAIL;
        }
        if (!world.isClient()) {
            player.getItemCooldownManager().set(stack, ABILITY_COOLDOWN);
            assert ((ServerLevel) world).getServer() != null;
            WandCooldownState.get(((ServerLevel)world).getServer()).save(player.getUUID(), "phantom", ABILITY_COOLDOWN);
            PhantomModeHandler.activatePhantomMode(player, (ServerLevel) world);
        }

        return ActionResult.SUCCESS;
    }
    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHit(stack, target, attacker);
    }
}
