package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayer;
import net.minecraft.server.world.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PullAttackScheduler {
    private static class ScheduledAttack {
        final ServerPlayer attacker;
        final LivingEntity target;
        final long executeTick;
        ScheduledAttack(ServerPlayer attacker, LivingEntity target, long executeTick) {
            this.attacker = attacker;
            this.target = target;
            this.executeTick = executeTick;
        }
    }
    private static final List<ScheduledAttack> pending = new ArrayList<>();
    private static void onTick(MinecraftServer server) {
        List<ScheduledAttack> toExecute = new ArrayList<>();
        List<ScheduledAttack> toRemove = new ArrayList<>();
        for (ScheduledAttack attack : pending) {
            for (ServerLevel world : server.getWorlds()) {
                if (world.getTime() >= attack.executeTick) {
                    toExecute.add(attack);
                    toRemove.add(attack);
                    break;
                }
            }
        }
        pending.removeAll(toRemove);
        for (ScheduledAttack attack : toExecute) {
            if (!attack.attacker.isRemoved() && !attack.target.isRemoved()) {
                attack.attacker.attack(attack.target);
            }
        }
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PullAttackScheduler::onTick);
    }
    public static void schedule(ServerPlayer attacker, LivingEntity target, long executeTick) {
        pending.add(new ScheduledAttack(attacker, target, executeTick));
    }
}
