package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class MagmaWandHandler {
    private static final Map<UUID, Long> lastDamageTick = new HashMap<>();
    private static final Map<UUID, Float> passiveAbsorptionGiven = new HashMap<>();
    private static final Map<UUID, Long> abilityEndTimes = new HashMap<>();
    private static final Map<UUID, Long> ultimateEndTimes = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> fireRingBlocks = new HashMap<>();
    private static final Set<UUID> wandFireballEntities = new HashSet<>();
    private static final Map<UUID, Long> fireballExpiryTimes = new HashMap<>();
    private static final Map<UUID, Long> ultimateCooldownEndTimes = new HashMap<>();
    private static final Map<UUID, Long> absorptionGrantTick = new HashMap<>();
    private static final Map<UUID, Long> absorptionGrantedAt = new HashMap<>();
    private static final int NO_DAMAGE_DURATION = 600; // how many ticks you have to not have taken damage for the absorption hearts
    private static final double KNOCKBACK_RADIUS = 6.0; // 6 blocks
    private static final double FIRE_RING_RADIUS = 4.0; // how far the fire ring extends
    private static final int GROUND_SCAN_RANGE = 5;

    private static void triggerPassiveKnockback(PlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;
        Box box = player.getBoundingBox().expand(KNOCKBACK_RADIUS);
        List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class, box, entity -> entity != player && !entity.isRemoved());
        for (LivingEntity entity : nearby) {
            Vec3d away = entity.getEntityPos().subtract(player.getEntityPos());
            if (away.horizontalLength() < 0.01) {
                away = new Vec3d(1, 0, 0);
            }
            away = away.normalize();
            entity.takeKnockback(
                    1.5,
                    -away.x,
                    -away.z
            );
            entity.addVelocity(0, 0.3, 0);
            entity.velocityDirty = true;
            if (entity instanceof ServerPlayerEntity serverTarget) {
                serverTarget.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(serverTarget));
            }
        }
        for (int i = 0; i < 40; i++) {
            double angle = world.getRandom().nextDouble() * 2 * Math.PI;
            double distance = world.getRandom().nextDouble() * KNOCKBACK_RADIUS;
            double posX = player.getX() + distance * Math.cos(angle);
            double posY = player.getY() + world.getRandom().nextDouble() * 2.5;
            double posZ = player.getZ() + distance * Math.sin(angle);
            world.spawnParticles(
                    new DustParticleEffect(0xF7803D, 2.5f),
                    posX,
                    posY,
                    posZ,
                    1,
                    0,
                    0,
                    0,
                    0
            );
        }
        world.spawnParticles(
                ParticleTypes.LAVA,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                20,
                0.8,
                0.5,
                0.8,
                0.3
        );
        world.spawnParticles(
                ParticleTypes.LARGE_SMOKE,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                10,
                0.5,
                0.4,
                0.5,
                0.05
        );
    }
    private static void applyPassiveBuffs(PlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.FIRE_RESISTANCE,
                1200,
                0,
                false,
                true,
                true
        ));
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.STRENGTH,
                1200,
                0,
                false,
                true,
                true
        ));
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE,
                1800,
                0,
                false,
                true,
                true
        ));
    }
    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof PlayerEntity player)) return true;
        UUID uuid = player.getUuid();
        if (source.getSource() != null && wandFireballEntities.contains(source.getSource().getUuid())) {
            return false;
        }
        if (amount >= 1.0f) {
            lastDamageTick.put(uuid, entity.getEntityWorld().getTime());
            absorptionGrantTick.remove(uuid);
        }
        return true;
    }
    private static void checkPassiveAbsorption(PlayerEntity player, long currentTick) {
        UUID uuid = player.getUuid();
        if (passiveAbsorptionGiven.containsKey(uuid)) return;
        if (!(player.getMainHandStack().getItem() instanceof MagmaWandItem) && !(player.getOffHandStack().getItem() instanceof MagmaWandItem)) return;
        long lastHit = lastDamageTick.containsKey(uuid) ? Math.min(lastDamageTick.get(uuid), currentTick) : currentTick;
        if (currentTick - lastHit >= NO_DAMAGE_DURATION) {
            Long grantAt = absorptionGrantTick.get(uuid);
            if (grantAt == null) {
                absorptionGrantTick.put(uuid, currentTick + 40);
                return;
            }
            if (currentTick < grantAt) return;
            absorptionGrantTick.remove(uuid);
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
            serverPlayer.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.ABSORPTION,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    false
            ));
            passiveAbsorptionGiven.put(uuid, 4.0f);
            absorptionGrantedAt.put(uuid, currentTick);
        } else {
            absorptionGrantTick.remove(uuid);
        }
    }
    private static void buildFireRing(PlayerEntity player, ServerWorld world) {
        UUID uuid = player.getUuid();
        Set<BlockPos> desired = computeRingPositions(player, world);
        Set<BlockPos> current = fireRingBlocks.getOrDefault(uuid, new HashSet<>());
        for (BlockPos position : current) {
            if (!desired.contains(position) && world.getBlockState(position).isOf(Blocks.FIRE)) {
                world.removeBlock(position, false);
            }
        }
        for (BlockPos position : desired) {
            if (!current.contains(position)) {
                var state = world.getBlockState(position);
                if (!state.isSolidBlock(world, position)) {
                    world.setBlockState(position, Blocks.FIRE.getDefaultState(), 3);
                }
            }
        }
        fireRingBlocks.put(uuid, desired);
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(MagmaWandHandler::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(MagmaWandHandler::onDamage);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID uuid = handler.player.getUuid();
            long joinTick = handler.player.getEntityWorld().getTime();
            lastDamageTick.put(uuid, joinTick + 40);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            lastDamageTick.remove(uuid);
            passiveAbsorptionGiven.remove(uuid);
            abilityEndTimes.remove(uuid);
            ultimateEndTimes.remove(uuid);
            removeFireRing(handler.player, handler.player.getEntityWorld());
            fireRingBlocks.remove(uuid);
            ultimateCooldownEndTimes.remove(uuid);
            MagmaWandItem.hitCounters.remove(uuid);
            absorptionGrantTick.remove(uuid);
            absorptionGrantedAt.remove(uuid);
        });
    }
    public static void launchFireball(PlayerEntity player, LivingEntity target) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;
        Vec3d direction = target.getEntityPos()
                .add(0, target.getHeight() / 2.0, 0)
                .subtract(player.getEyePos())
                .normalize();
        MagmaWandFireballEntity fireball = new MagmaWandFireballEntity(world, player, direction);
        fireball.setPosition(player.getEyePos().x, player.getEyePos().y, player.getEyePos().z);
        wandFireballEntities.add(fireball.getUuid());
        world.spawnEntity(fireball);
        fireballExpiryTimes.put(fireball.getUuid(), world.getTime() + 120);
    }
    public static void tryActivateAbility(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (serverPlayer.getItemCooldownManager().isCoolingDown(player.getMainHandStack()) || serverPlayer.getItemCooldownManager().isCoolingDown(player.getOffHandStack())) return;
        UUID uuid = player.getUuid();
        ServerWorld world = serverPlayer.getEntityWorld();
        abilityEndTimes.put(uuid, world.getTime() + MagmaWandItem.ABILITY_DURATION);
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.FIRE_RESISTANCE,
                MagmaWandItem.ABILITY_DURATION,
                0,
                false,
                true,
                true
        ));
        ItemStack wandStack = player.getMainHandStack().getItem() instanceof MagmaWandItem ? player.getMainHandStack() : player.getOffHandStack();
        serverPlayer.getItemCooldownManager().set(wandStack, MagmaWandItem.ABILITY_COOLDOWN + MagmaWandItem.ABILITY_DURATION);
    }
    public static void tryActivateUltimate(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        UUID uuid = player.getUuid();
        if (ultimateEndTimes.containsKey(uuid)) return;
        long currentTick = serverPlayer.getEntityWorld().getTime();
        Long cooldownEnd = ultimateCooldownEndTimes.get(uuid);
        if (cooldownEnd != null && currentTick < cooldownEnd) return;
        ServerWorld world = serverPlayer.getEntityWorld();
        ultimateEndTimes.put(uuid, world.getTime() + MagmaWandItem.ULTIMATE_DURATION);
        buildFireRing(player, world);
        ItemStack wandStack = player.getMainHandStack().getItem() instanceof MagmaWandItem ? player.getMainHandStack() : player.getOffHandStack();
        serverPlayer.getItemCooldownManager().set(wandStack, MagmaWandItem.ULTIMATE_DURATION + MagmaWandItem.ULTIMATE_COOLDOWN);
    }
    private static Set<BlockPos> computeRingPositions(PlayerEntity player, ServerWorld world) {
        Set<BlockPos> positions = new HashSet<>();
        int steps = 36;
        for (int i = 0; i < steps; i++) {
            double angle = (2 * Math.PI / steps) * i;
            double dx = FIRE_RING_RADIUS * Math.cos(angle);
            double dz = FIRE_RING_RADIUS * Math.sin(angle);
            int blockX = (int) Math.round(player.getX() + dx);
            int blockZ = (int) Math.round(player.getZ() + dz);
            BlockPos ground = findGround(world, blockX, (int) player.getY(), blockZ);
            if (ground != null) {
                positions.add(ground.up());
            }
        }
        return positions;
    }
    private static void updateFireRing(PlayerEntity player, ServerWorld world) {
        buildFireRing(player, world);
        Set<BlockPos> ring = fireRingBlocks.get(player.getUuid());
        if (ring != null) {
            removeSpreadFire(world, ring);
        }
    }
    private static void removeFireRing(PlayerEntity player, ServerWorld world) {
        UUID uuid = player.getUuid();
        Set<BlockPos> ring = fireRingBlocks.get(uuid);
        if (ring == null) return;
        for (BlockPos position : ring) {
            if (world.getBlockState(position).isOf(Blocks.FIRE)) {
                world.removeBlock(position, false);
            }
        }
        fireRingBlocks.remove(uuid);
    }
    private static void removeSpreadFire(ServerWorld world, Set<BlockPos> ringPositions) {
        int[] offsets = {-1, 0, 1};
        for (BlockPos pos : ringPositions) {
            for (int dx : offsets) {
                for (int dy : offsets) {
                    for (int dz : offsets) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = pos.add(dx, dy, dz);
                        if (!ringPositions.contains(neighbor) && world.getBlockState(neighbor).isOf(Blocks.FIRE)) {
                            world.removeBlock(neighbor, false);
                        }
                    }
                }
            }
        }
    }
    private static BlockPos findGround(ServerWorld world, int x, int startY, int z) {
        for (int y = startY; y <= startY + GROUND_SCAN_RANGE; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos above = pos.up();
            if (world.getBlockState(pos).isSolidBlock(world, pos)
                    && !world.getBlockState(above).isSolidBlock(world, above)) {
                return pos;
            }
        }
        for (int y = startY - 1; y >= startY - GROUND_SCAN_RANGE; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos above = pos.up();
            if (world.getBlockState(pos).isSolidBlock(world, pos)
                    && !world.getBlockState(above).isSolidBlock(world, above)) {
                return pos;
            }
        }
        return null;
    }
    private static void onTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            long currentTick = world.getTime();
            fireballExpiryTimes.entrySet().removeIf(entry -> {
                if (currentTick >= entry.getValue()) {
                    wandFireballEntities.remove(entry.getKey());
                    return true;
                }
                return false;
            });
            for (PlayerEntity player : world.getPlayers()) {
                UUID uuid = player.getUuid();
                boolean holdingWand = player.getMainHandStack().getItem() instanceof MagmaWandItem || player.getOffHandStack().getItem() instanceof MagmaWandItem;
                if (holdingWand) {
                    checkPassiveAbsorption(player, currentTick);
                    if (passiveAbsorptionGiven.containsKey(uuid)) {
                        Long grantedAt = absorptionGrantedAt.getOrDefault(uuid, 0L);
                        if (currentTick - grantedAt >= 5) {
                            if (player.getAbsorptionAmount() <= 0 || !player.hasStatusEffect(StatusEffects.ABSORPTION)) {
                                passiveAbsorptionGiven.remove(uuid);
                                absorptionGrantedAt.remove(uuid);
                                player.removeStatusEffect(StatusEffects.ABSORPTION);
                                lastDamageTick.put(uuid, currentTick);
                                absorptionGrantTick.remove(uuid);
                                triggerPassiveKnockback(player);
                                applyPassiveBuffs(player);
                            }
                        }
                    }
                }
                Long abilityEnd = abilityEndTimes.get(uuid);
                if (abilityEnd != null && currentTick >= abilityEnd) {
                    abilityEndTimes.remove(uuid);
                    player.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
                } else if (abilityEnd != null) {
                    if (player.isOnFire() && player.getFireTicks() > 0) {
                        player.heal(0.1f);
                        player.setFireTicks(0);
                    }
                }
                Long ultimateEnd = ultimateEndTimes.get(uuid);
                if (ultimateEnd != null) {
                    if (currentTick >= ultimateEnd) {
                        ultimateEndTimes.remove(uuid);
                        removeFireRing(player, world);
                        ultimateCooldownEndTimes.put(uuid, currentTick + MagmaWandItem.ULTIMATE_COOLDOWN + MagmaWandItem.ULTIMATE_DURATION);
                    } else if (currentTick % 5 == 0) {
                        updateFireRing(player, world);
                    }
                }
            }
        }
    }
}