package com.dae.creep;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class CreepMod implements ModInitializer {
    public static final String MOD_ID = "dae_creep";

    @Override
    public void onInitialize() {
        CreepMobRegistry.register();
        CreepSpawnReplacement.register();
        registerCommands();
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                Commands.literal("dae")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(
                        Commands.literal("creep")
                            .then(
                                Commands.literal("stare")
                                    .executes(context -> reportStare(context.getSource()))
                                    .then(Commands.literal("random")
                                        .executes(context -> setStareRandom(context.getSource())))
                                    .then(Commands.literal("scope")
                                        .executes(context -> setStareMode(context.getSource(), CreepStareMode.SCOPE)))
                                    .then(Commands.literal("mirror")
                                        .executes(context -> setStareMode(context.getSource(), CreepStareMode.MIRROR)))
                            )
                            .then(
                                Commands.literal("speed")
                                    .then(Commands.literal("walk")
                                        .executes(context -> getSpeed(context.getSource(), "walk"))
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.05D, 1.00D))
                                            .executes(context -> setSpeed(context.getSource(), "walk", DoubleArgumentType.getDouble(context, "value")))))
                                    .then(Commands.literal("run")
                                        .executes(context -> getSpeed(context.getSource(), "run"))
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.05D, 1.00D))
                                            .executes(context -> setSpeed(context.getSource(), "run", DoubleArgumentType.getDouble(context, "value")))))
                            )
                            .then(
                                Commands.literal("blow")
                                    .executes(context -> getBlow(context.getSource()))
                                    .then(Commands.argument("value", IntegerArgumentType.integer(0, 1))
                                        .executes(context -> setBlow(context.getSource(), IntegerArgumentType.getInteger(context, "value"))))
                            )
                            .then(
                                Commands.literal("tick")
                                    .executes(context -> getTick(context.getSource()))
                                    .then(Commands.argument("value", IntegerArgumentType.integer(1, 200))
                                        .executes(context -> setTick(context.getSource(), IntegerArgumentType.getInteger(context, "value"))))
                            )
                            .then(
                                Commands.literal("respawn")
                                    .executes(context -> getRespawn(context.getSource()))
                                    .then(Commands.argument("value", IntegerArgumentType.integer(10, 200))
                                        .executes(context -> setRespawn(context.getSource(), IntegerArgumentType.getInteger(context, "value"))))
                            )
                            .then(
                                Commands.literal("profile")
                                    .executes(context -> getProfile(context.getSource()))
                            )
                            .then(
                                Commands.literal("counter")
                                    .executes(context -> getSpawnCounter(context.getSource()))
                            )
                            .then(
                                Commands.literal("skin")
                                    .executes(context -> getSkin(context.getSource()))
                                    .then(Commands.literal("creep")
                                        .executes(context -> setSkin(context.getSource(), CreepSkinType.BASE)))
                                    .then(Commands.literal("dim")
                                        .executes(context -> setSkin(context.getSource(), CreepSkinType.DIM)))
                                    .then(Commands.literal("dark")
                                        .executes(context -> setSkin(context.getSource(), CreepSkinType.DARK)))
                                    .then(Commands.literal("spot")
                                        .executes(context -> setSkin(context.getSource(), CreepSkinType.SPOT)))
                                    .then(Commands.literal("blu")
                                        .executes(context -> setSkin(context.getSource(), CreepSkinType.BLU)))
                                    .then(Commands.literal("camo")
                                        .executes(context -> setSkin(context.getSource(), CreepSkinType.CAMO)))
                                    .then(Commands.literal("ora")
                                        .executes(context -> setSkin(context.getSource(), CreepSkinType.ORA)))
                                    .then(Commands.literal("red")
                                        .executes(context -> setSkin(context.getSource(), CreepSkinType.RED)))
                                    .then(Commands.literal("snow")
                                        .executes(context -> setSkin(context.getSource(), CreepSkinType.SNOW)))
                                    .then(Commands.literal("yel")
                                        .executes(context -> setSkin(context.getSource(), CreepSkinType.YEL)))
                            )
                    )
            );
        });
    }

    private static int setStareMode(CommandSourceStack source, CreepStareMode mode) {
        applyStareModeToLiveCreeps(source.getServer(), mode);
        source.sendSuccess(() -> Component.literal("Dae Creep live stare trait override set to " + mode.name().toLowerCase()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setStareRandom(CommandSourceStack source) {
        applyRandomStareToLiveCreeps(source.getServer());
        source.sendSuccess(() -> Component.literal("Dae Creep live stare traits rerolled random (50/50 scope|mirror)"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int reportStare(CommandSourceStack source) {
        List<PauseCreeperEntity> creeps = getLiveCreeps(source.getServer());
        long scopeCount = creeps.stream().filter(c -> c.getAssignedStareMode() == CreepStareMode.SCOPE).count();
        long mirrorCount = creeps.size() - scopeCount;

        source.sendSuccess(() -> Component.literal("Live creeps: " + creeps.size() + " (scope=" + scopeCount + ", mirror=" + mirrorCount + ")"), false);

        if (creeps.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }

        int index = 1;
        for (PauseCreeperEntity creep : creeps) {
            Player nearest = creep.level().getNearestPlayer(creep, 256.0D);
            String proximity = nearest == null
                ? "no player in 256 blocks"
                : String.format("%.1f blocks to %s", Math.sqrt(creep.distanceToSqr(nearest)), nearest.getName().getString());

            String line = "#" + index
                + " trait=" + creep.getAssignedStareMode().name().toLowerCase()
                + ", pos=(" + String.format("%.1f", creep.getX()) + ", " + String.format("%.1f", creep.getY()) + ", " + String.format("%.1f", creep.getZ()) + ")"
                + ", proximity=" + proximity;
            source.sendSuccess(() -> Component.literal(line), false);
            index++;
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int getSpeed(CommandSourceStack source, String type) {
        double speed = type.equals("run") ? CreepBehaviorConfig.getRunSpeed() : CreepBehaviorConfig.getWalkSpeed();
        source.sendSuccess(() -> Component.literal("Dae Creep " + type + " speed is " + String.format("%.2f", speed)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setSpeed(CommandSourceStack source, String type, double value) {
        if (type.equals("run")) {
            CreepBehaviorConfig.setRunSpeed(value);
        } else {
            CreepBehaviorConfig.setWalkSpeed(value);
        }

        double speed = type.equals("run") ? CreepBehaviorConfig.getRunSpeed() : CreepBehaviorConfig.getWalkSpeed();
        source.sendSuccess(() -> Component.literal("Dae Creep " + type + " speed set to " + String.format("%.2f", speed)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int getBlow(CommandSourceStack source) {
        int value = CreepBehaviorConfig.isPermitDetonate() ? 1 : 0;
        source.sendSuccess(() -> Component.literal("Dae Creep blow is " + value), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setBlow(CommandSourceStack source, int value) {
        boolean enabled = value == 1;
        CreepBehaviorConfig.setPermitDetonate(enabled);
        source.sendSuccess(() -> Component.literal("Dae Creep blow set to " + value), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int getTick(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Dae Creep fuse ticks is " + CreepBehaviorConfig.getFuseTicks()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setTick(CommandSourceStack source, int value) {
        CreepBehaviorConfig.setFuseTicks(value);
        applyFuseTicksToLiveCreeps(source.getServer());
        source.sendSuccess(() -> Component.literal("Dae Creep fuse ticks set to " + CreepBehaviorConfig.getFuseTicks()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int getRespawn(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Dae Creep respawn ratio is " + CreepBehaviorConfig.getRespawnRatio()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setRespawn(CommandSourceStack source, int value) {
        CreepBehaviorConfig.setRespawnRatio(value);
        source.sendSuccess(() -> Component.literal("Dae Creep respawn ratio set to " + CreepBehaviorConfig.getRespawnRatio()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int getProfile(CommandSourceStack source) {
        int counter = CreepSpawnCounterData.get(source.getServer().overworld()).getCount();
        String profile = "Dae Creep profile:"
            + ", walk=" + String.format("%.2f", CreepBehaviorConfig.getWalkSpeed())
            + ", run=" + String.format("%.2f", CreepBehaviorConfig.getRunSpeed())
            + ", blow=" + (CreepBehaviorConfig.isPermitDetonate() ? "1" : "0")
            + ", tick=" + CreepBehaviorConfig.getFuseTicks()
            + ", respawn=" + CreepBehaviorConfig.getRespawnRatio()
            + ", counter=" + counter
            + ", skin=" + CreepBehaviorConfig.getDefaultSkinType().id()
            + ", skin_mode=" + CreepBehaviorConfig.getDefaultSkinMode().id();
        source.sendSuccess(() -> Component.literal(profile), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int getSpawnCounter(CommandSourceStack source) {
        int counter = CreepSpawnCounterData.get(source.getServer().overworld()).getCount();
        source.sendSuccess(() -> Component.literal("Dae Creep spawn counter is " + counter), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int getSkin(CommandSourceStack source) {
        source.sendSuccess(
            () -> Component.literal("Dae Creep skin is " + CreepBehaviorConfig.getDefaultSkinType().id()
                + " (" + CreepBehaviorConfig.getDefaultSkinMode().id() + " mode)"),
            false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int setSkin(CommandSourceStack source, CreepSkinType skin) {
        CreepBehaviorConfig.setDefaultSkinType(skin);
        applySkinToLiveCreeps(source.getServer(), skin);
        CreepSkinMode mode = CreepSkinMode.STATIC;
        CreepBehaviorConfig.setDefaultSkinMode(mode);
        applySkinModeToLiveCreeps(source.getServer(), mode);
        source.sendSuccess(() -> Component.literal("Dae Creep skin set to " + skin.id() + " (" + mode.id() + " mode)"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setSkinMode(CommandSourceStack source, CreepSkinMode mode) {
        CreepBehaviorConfig.setDefaultSkinMode(mode);
        applySkinModeToLiveCreeps(source.getServer(), mode);
        source.sendSuccess(() -> Component.literal("Dae Creep skin mode set to " + mode.id()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static void applyStareModeToLiveCreeps(MinecraftServer server, CreepStareMode mode) {
        forEachLiveCreep(server, creep -> creep.setAssignedStareMode(mode));
    }

    private static void applyRandomStareToLiveCreeps(MinecraftServer server) {
        forEachLiveCreep(server, creep -> creep.setAssignedStareMode(creep.getRandom().nextBoolean() ? CreepStareMode.SCOPE : CreepStareMode.MIRROR));
    }

    private static void applyFuseTicksToLiveCreeps(MinecraftServer server) {
        forEachLiveCreep(server, PauseCreeperEntity::syncRuntimeConfig);
    }

    private static void applySkinToLiveCreeps(MinecraftServer server, CreepSkinType skin) {
        forEachLiveCreep(server, creep -> creep.setSkinType(skin));
    }

    private static void applySkinModeToLiveCreeps(MinecraftServer server, CreepSkinMode mode) {
        forEachLiveCreep(server, creep -> creep.setSkinMode(mode));
    }

    private static void forEachLiveCreep(MinecraftServer server, java.util.function.Consumer<PauseCreeperEntity> consumer) {
        for (PauseCreeperEntity creep : getLiveCreeps(server)) {
            consumer.accept(creep);
        }
    }

    private static List<PauseCreeperEntity> getLiveCreeps(MinecraftServer server) {
        List<PauseCreeperEntity> creeps = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof PauseCreeperEntity creep) {
                    creeps.add(creep);
                }
            }
        }
        return creeps;
    }
}
