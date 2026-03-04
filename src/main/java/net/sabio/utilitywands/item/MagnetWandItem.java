package net.sabio.utilitywands.item;

import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MagnetWandItem extends Item {
    public static final int COOLDOWN = 600; // 30 seconds (in ticks)
    private static final double RANGE = 30.0; // 30 blocks (i think)

    public MagnetWandItem(Settings settings) {
        super(ToolMaterial.IRON.applyToolSettings(
                settings,
                BlockTags.PICKAXE_MINEABLE, // magnet wand item can mine any iron pickaxe-only minable stuff
                1.0f,
                -2.8f,
                0.0f
        ));
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.FAIL;
        }
        if (!world.isClient()) {
            Box searchBox = player.getBoundingBox().expand(RANGE);
            List<Entity> targets = new ArrayList<>();
            targets.addAll(world.getEntitiesByClass(ItemEntity.class, searchBox, entity -> !entity.isRemoved()));
            targets.addAll(world.getEntitiesByClass(ExperienceOrbEntity.class, searchBox, entity -> !entity.isRemoved()));
            MagnetPullManager.startPull(player, targets);
            player.getItemCooldownManager().set(stack, COOLDOWN);
        }

        return ActionResult.SUCCESS;
    }
}
