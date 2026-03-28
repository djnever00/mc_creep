package com.example.creep;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

public final class CreepSpawnReplacement {
    private static final int SPAWN_RATIO = 250;

    private CreepSpawnReplacement() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(CreepSpawnReplacement::onEntityLoad);
    }

    private static void onEntityLoad(Entity entity, ServerLevel level) {
        if (entity.getType() != EntityType.CREEPER) {
            return;
        }

        // Prevent counting creepers loaded from chunk saves.
        if (entity.tickCount != 0) {
            return;
        }

        int currentCount = CreepSpawnCounterData.get(level).incrementAndGet();
        if (currentCount % SPAWN_RATIO != 0) {
            return;
        }

        PauseCreeperEntity replacement = CreepMobRegistry.CREEP.create(level, EntitySpawnReason.NATURAL);
        if (replacement == null) {
            return;
        }

        replacement.copyPosition(entity);
        replacement.setYRot(entity.getYRot());
        replacement.setYHeadRot(entity.getYRot());
        replacement.setYBodyRot(entity.getYRot());
        replacement.setXRot(entity.getXRot());

        if (level.addFreshEntity(replacement)) {
            entity.discard();
        }
    }
}
