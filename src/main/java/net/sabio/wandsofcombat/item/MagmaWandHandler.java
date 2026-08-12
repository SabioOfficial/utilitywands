package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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

    private static void triggerPassiveKnockback(Player player) {
        if (!(player.level() instanceof ServerLevel world)) return;
        AABB box = player.getBoundingBox().inflate(KNOCKBACK_RADIUS);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && !entity.isRemoved());
        for (LivingEntity entity : nearby) {
            Vec3 away = entity.position().subtract(player.position());
            if (away.horizontalDistance() < 0.01) {
                away = new Vec3(1, 0, 0);
            }
            away = away.normalize();
            entity.knockback(
                    1.5,
                    -away.x,
                    -away.z,
                    world.damageSources().magic(),
                    0
            );
            Vec3 currentMotion = entity.getDeltaMovement();
            entity.setDeltaMovement(currentMotion.x, 0.3, currentMotion.z);
            entity.hurtMarked = true;
            if (entity instanceof ServerPlayer serverTarget) {
                serverTarget.connection.send(new ClientboundSetEntityMotionPacket(serverTarget));
            }
        }
        for (int i = 0; i < 40; i++) {
            double angle = world.getRandom().nextDouble() * 2 * Math.PI;
            double distance = world.getRandom().nextDouble() * KNOCKBACK_RADIUS;
            double posX = player.getX() + distance * Math.cos(angle);
            double posY = player.getY() + world.getRandom().nextDouble() * 2.5;
            double posZ = player.getZ() + distance * Math.sin(angle);
            world.sendParticles(
                    new DustParticleOptions(0xF7803D, 2.5f),
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
        world.sendParticles(
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
        world.sendParticles(
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
    private static void applyPassiveBuffs(Player player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                1200,
                0,
                false,
                true,
                true
        ));
        player.addEffect(new MobEffectInstance(
                MobEffects.STRENGTH,
                1200,
                0,
                false,
                true,
                true
        ));
        player.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE,
                1800,
                0,
                false,
                true,
                true
        ));
    }
    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof Player player)) return true;
        UUID uuid = player.getUUID();
        if (source.getEntity() != null && wandFireballEntities.contains(source.getEntity().getUUID())) {
            return false;
        }
        if (amount >= 1.0f) {
            lastDamageTick.put(uuid, entity.level().getGameTime());
            absorptionGrantTick.remove(uuid);
        }
        return true;
    }
    private static void checkPassiveAbsorption(Player player, long currentTick) {
        UUID uuid = player.getUUID();
        if (passiveAbsorptionGiven.containsKey(uuid)) return;
        if (!(player.getMainHandItem().getItem() instanceof MagmaWandItem) && !(player.getOffhandItem().getItem() instanceof MagmaWandItem)) return;
        long lastHit = lastDamageTick.containsKey(uuid) ? Math.min(lastDamageTick.get(uuid), currentTick) : currentTick;
        if (currentTick - lastHit >= NO_DAMAGE_DURATION) {
            Long grantAt = absorptionGrantTick.get(uuid);
            if (grantAt == null) {
                absorptionGrantTick.put(uuid, currentTick + 40);
                return;
            }
            if (currentTick < grantAt) return;
            absorptionGrantTick.remove(uuid);
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            serverPlayer.addEffect(new MobEffectInstance(
                    MobEffects.ABSORPTION,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    false
            ));
            passiveAbsorptionGiven.put(uuid, 4.0f);
            absorptionGrantedAt.put(uuid, currentTick);
            if (player.level() instanceof ServerLevel ServerLevel) {
                for (int i = 0; i < 16; i++) {
                    double angle = (2.0 * Math.PI / 16) * i;
                    double posX = player.getX() + 1.0 * Math.cos(angle);
                    double posZ = player.getZ() + 1.0 * Math.sin(angle);
                    ServerLevel.sendParticles(
                            new DustParticleOptions(0xAB421C, 1.8f),
                            posX,
                            player.getY() + 1.0,
                            posZ,
                            1,
                            0,
                            0.2,
                            0,
                            0
                    );
                }
                ServerLevel.sendParticles(
                        ParticleTypes.FLAME,
                        player.getX(),
                        player.getY() + 1.0,
                        player.getZ(),
                        12,
                        0.3,
                        0.4,
                        0.3,
                        0.05
                );
            }
        } else {
            absorptionGrantTick.remove(uuid);
        }
    }
    private static void buildFireRing(Player player, ServerLevel world) {
        UUID uuid = player.getUUID();
        Set<BlockPos> desired = computeRingPositions(player, world);
        Set<BlockPos> current = fireRingBlocks.getOrDefault(uuid, new HashSet<>());
        for (BlockPos position : current) {
            if (!desired.contains(position) && world.getBlockState(position).is(Blocks.FIRE)) {
                world.removeBlock(position, false);
            }
        }
        for (BlockPos position : desired) {
            if (!current.contains(position)) {
                var state = world.getBlockState(position);
                if (!state.isCollisionShapeFullBlock(world, position)) {
                    world.setBlock(position, Blocks.FIRE.defaultBlockState(), 3);
                }
            }
        }
        fireRingBlocks.put(uuid, desired);
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(MagmaWandHandler::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(MagmaWandHandler::onDamage);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID uuid = handler.player.getUUID();
            long joinTick = handler.player.level().getGameTime();
            lastDamageTick.put(uuid, joinTick + 40);
            WandCooldownState state = WandCooldownState.get(server);
            int abilityTicks = state.getRemainingTicks(uuid, "magma_ability");
            int ultimateTicks = state.getRemainingTicks(uuid, "magma_ultimate");
            int ticks = Math.max(abilityTicks, ultimateTicks);
            if (ticks > 0) {
                ItemStack wandStack = handler.player.getMainHandItem().getItem() instanceof MagmaWandItem ? handler.player.getMainHandItem() : handler.player.getOffhandItem();
                handler.player.getCooldowns().addCooldown(wandStack, ticks);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            lastDamageTick.remove(uuid);
            passiveAbsorptionGiven.remove(uuid);
            abilityEndTimes.remove(uuid);
            ultimateEndTimes.remove(uuid);
            if (handler.player.level() instanceof ServerLevel serverLevel) {
                removeFireRing(handler.player, serverLevel);
            }
            fireRingBlocks.remove(uuid);
            ultimateCooldownEndTimes.remove(uuid);
            MagmaWandItem.hitCounters.remove(uuid);
            absorptionGrantTick.remove(uuid);
            absorptionGrantedAt.remove(uuid);
        });
    }
    public static void launchFireball(Player player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel world)) return;
        Vec3 direction = target.position()
                .add(0, target.getBbHeight() / 2.0, 0)
                .subtract(player.getEyePosition())
                .normalize();
        MagmaWandFireballEntity fireball = new MagmaWandFireballEntity(world, player, direction);
        fireball.setPos(player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z);
        wandFireballEntities.add(fireball.getUUID());
        world.addFreshEntity(fireball);
        fireballExpiryTimes.put(fireball.getUUID(), world.getGameTime() + 120);
    }
    public static void tryActivateAbility(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.getCooldowns().isOnCooldown(player.getMainHandItem()) || serverPlayer.getCooldowns().isOnCooldown(player.getOffhandItem())) return;
        UUID uuid = player.getUUID();
        ServerLevel world = serverPlayer.level();
        abilityEndTimes.put(uuid, world.getGameTime() + MagmaWandItem.ABILITY_DURATION);
        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                MagmaWandItem.ABILITY_DURATION,
                0,
                false,
                true,
                true
        ));
        ItemStack wandStack = player.getMainHandItem().getItem() instanceof MagmaWandItem ? player.getMainHandItem() : player.getOffhandItem();
        serverPlayer.getCooldowns().addCooldown(wandStack, MagmaWandItem.ABILITY_COOLDOWN + MagmaWandItem.ABILITY_DURATION);
        WandCooldownState.get(Objects.requireNonNull(serverPlayer.level().getServer())).save(uuid, "magma_ability", MagmaWandItem.ABILITY_COOLDOWN + MagmaWandItem.ABILITY_DURATION);
    }
    public static void tryActivateUltimate(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        UUID uuid = player.getUUID();
        if (ultimateEndTimes.containsKey(uuid)) return;
        long currentTick = serverPlayer.level().getGameTime();
        Long cooldownEnd = ultimateCooldownEndTimes.get(uuid);
        if (cooldownEnd != null && currentTick < cooldownEnd) return;
        ServerLevel world = serverPlayer.level();
        ultimateEndTimes.put(uuid, world.getGameTime() + MagmaWandItem.ULTIMATE_DURATION);
        buildFireRing(player, world);
        ItemStack wandStack = player.getMainHandItem().getItem() instanceof MagmaWandItem ? player.getMainHandItem() : player.getOffhandItem();
        serverPlayer.getCooldowns().addCooldown(wandStack, MagmaWandItem.ULTIMATE_DURATION + MagmaWandItem.ULTIMATE_COOLDOWN);
        WandCooldownState.get(Objects.requireNonNull(serverPlayer.level().getServer())).save(uuid, "magma_ultimate", MagmaWandItem.ULTIMATE_DURATION + MagmaWandItem.ULTIMATE_COOLDOWN);
    }
    private static Set<BlockPos> computeRingPositions(Player player, ServerLevel world) {
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
                positions.add(ground.above());
            }
        }
        return positions;
    }
    private static void updateFireRing(Player player, ServerLevel world) {
        buildFireRing(player, world);
        Set<BlockPos> ring = fireRingBlocks.get(player.getUUID());
        if (ring != null) {
            removeSpreadFire(world, ring);
        }
    }
    private static void removeFireRing(Player player, ServerLevel world) {
        UUID uuid = player.getUUID();
        Set<BlockPos> ring = fireRingBlocks.get(uuid);
        if (ring == null) return;
        for (BlockPos position : ring) {
            if (world.getBlockState(position).is(Blocks.FIRE)) {
                world.removeBlock(position, false);
            }
        }
        fireRingBlocks.remove(uuid);
    }
    private static void removeSpreadFire(ServerLevel world, Set<BlockPos> ringPositions) {
        int[] offsets = {-1, 0, 1};
        for (BlockPos pos : ringPositions) {
            for (int dx : offsets) {
                for (int dy : offsets) {
                    for (int dz : offsets) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = pos.offset(dx, dy, dz);
                        if (!ringPositions.contains(neighbor) && world.getBlockState(neighbor).is(Blocks.FIRE)) {
                            world.removeBlock(neighbor, false);
                        }
                    }
                }
            }
        }
    }
    private static BlockPos findGround(ServerLevel world, int x, int startY, int z) {
        for (int y = startY; y <= startY + GROUND_SCAN_RANGE; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos above = pos.above();
            if (world.getBlockState(pos).isCollisionShapeFullBlock(world, pos)
                    && !world.getBlockState(above).isCollisionShapeFullBlock(world, above)) {
                return pos;
            }
        }
        for (int y = startY - 1; y >= startY - GROUND_SCAN_RANGE; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos above = pos.above();
            if (world.getBlockState(pos).isCollisionShapeFullBlock(world, pos)
                    && !world.getBlockState(above).isCollisionShapeFullBlock(world, above)) {
                return pos;
            }
        }
        return null;
    }
    private static void onTick(MinecraftServer server) {
        for (ServerLevel world : server.getAllLevels()) {
            long currentTick = world.getGameTime();
            fireballExpiryTimes.entrySet().removeIf(entry -> {
                if (currentTick >= entry.getValue()) {
                    wandFireballEntities.remove(entry.getKey());
                    return true;
                }
                return false;
            });
            for (Player player : world.players()) {
                UUID uuid = player.getUUID();
                boolean holdingWand = player.getMainHandItem().getItem() instanceof MagmaWandItem || player.getOffhandItem().getItem() instanceof MagmaWandItem;
                if (holdingWand) {
                    checkPassiveAbsorption(player, currentTick);
                    if (passiveAbsorptionGiven.containsKey(uuid)) {
                        Long grantedAt = absorptionGrantedAt.getOrDefault(uuid, 0L);
                        if (currentTick - grantedAt >= 5) {
                            if (player.getAbsorptionAmount() <= 0 || !player.hasEffect(MobEffects.ABSORPTION)) {
                                passiveAbsorptionGiven.remove(uuid);
                                absorptionGrantedAt.remove(uuid);
                                player.removeEffect(MobEffects.ABSORPTION);
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
                    player.removeEffect(MobEffects.FIRE_RESISTANCE);
                } else if (abilityEnd != null) {
                    if (player.isOnFire() && player.getRemainingFireTicks() > 0) {
                        player.heal(0.1f);
                        player.setRemainingFireTicks(0);
                        if (currentTick % 5 == 0) {
                            world.sendParticles(
                                    new DustParticleOptions(0xED541C, 1.5f),
                                    player.getX(),
                                    player.getY() + 1.0,
                                    player.getZ(),
                                    6,
                                    0.3,
                                    0.4,
                                    0.3,
                                    0
                            );
                            world.sendParticles(
                                    ParticleTypes.FLAME,
                                    player.getX(),
                                    player.getY() + 0.5,
                                    player.getZ(),
                                    4,
                                    0.2,
                                    0.3,
                                    0.2,
                                    0.02
                            );
                        }
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