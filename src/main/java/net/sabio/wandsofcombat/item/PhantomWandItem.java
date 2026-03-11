package net.sabio.wandsofcombat.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PhantomWandItem extends SwordItem  {
    public static final int ABILITY_DURATION = 120; // 6 seconds
    public static final int ABILITY_COOLDOWN = 1200 + ABILITY_DURATION; // 1 minute
    private static final float ATTACK_DAMAGE_BONUS = 2.0f; // 6 total attack damage
    private static final float ATTACK_SPEED = -1.5f;
    public static final Set<UUID> phantomPlayers = new HashSet<>();
    public static final Set<UUID> pullingPlayers = new HashSet<>();
    public PhantomWandItem(Settings settings) {
        super(ToolMaterials.DIAMOND, settings.attributeModifiers(
                SwordItem.createAttributeModifiers(ToolMaterials.DIAMOND, (int) ATTACK_DAMAGE_BONUS, ATTACK_SPEED)
        ));
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(player.getStackInHand(hand));
        }
        if (!world.isClient()) {
            player.getItemCooldownManager().set(this, ABILITY_COOLDOWN);
            assert ((ServerWorld) world).getServer() != null;
            WandCooldownState.get(((ServerWorld)world).getServer()).save(player.getUuid(), "phantom", ABILITY_COOLDOWN);
            PhantomModeHandler.activatePhantomMode(player, (ServerWorld) world);
        }

        return TypedActionResult.success(player.getStackInHand(hand));
    }
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return super.postHit(stack, target, attacker);
    }
}
