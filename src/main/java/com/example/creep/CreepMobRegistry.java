package com.example.creep;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class CreepMobRegistry {
    private static final Identifier CREEP_ID = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "creep");
    private static final ResourceKey<EntityType<?>> CREEP_KEY = ResourceKey.create(Registries.ENTITY_TYPE, CREEP_ID);

    public static final EntityType<PauseCreeperEntity> CREEP = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        CREEP_ID,
        FabricEntityTypeBuilder.create(MobCategory.MONSTER, PauseCreeperEntity::new)
            .dimensions(EntityDimensions.fixed(0.6F, 1.7F))
            .build(CREEP_KEY)
    );

    private CreepMobRegistry() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(CREEP, PauseCreeperEntity.createPauseCreeperAttributes());
    }
}
