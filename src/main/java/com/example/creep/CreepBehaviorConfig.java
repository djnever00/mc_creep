package com.example.creep;

public final class CreepBehaviorConfig {
    private static final double DEFAULT_WALK_SPEED = 0.32D;
    private static final double DEFAULT_RUN_SPEED = 0.36D;
    private static final int DEFAULT_FUSE_TICKS = 4;
    private static final double MIN_SPEED = 0.05D;
    private static final double MAX_SPEED = 1.00D;
    private static final int MIN_FUSE_TICKS = 1;
    private static final int MAX_FUSE_TICKS = 200;

    private static double walkSpeed = DEFAULT_WALK_SPEED;
    private static double runSpeed = DEFAULT_RUN_SPEED;
    private static int fuseTicks = DEFAULT_FUSE_TICKS;
    private static boolean permitDetonate = true;
    private static CreepSkinType defaultSkinType = CreepSkinType.BASE;
    private static CreepSkinMode defaultSkinMode = CreepSkinMode.STATIC;

    private CreepBehaviorConfig() {
    }

    public static double getWalkSpeed() {
        return walkSpeed;
    }

    public static double getRunSpeed() {
        return runSpeed;
    }

    public static void setWalkSpeed(double speed) {
        walkSpeed = clampSpeed(speed);
    }

    public static void setRunSpeed(double speed) {
        runSpeed = clampSpeed(speed);
    }

    public static int getFuseTicks() {
        return fuseTicks;
    }

    public static void setFuseTicks(int ticks) {
        fuseTicks = clampFuseTicks(ticks);
    }

    public static boolean isPermitDetonate() {
        return permitDetonate;
    }

    public static void setPermitDetonate(boolean enabled) {
        permitDetonate = enabled;
    }

    public static CreepSkinType getDefaultSkinType() {
        return defaultSkinType;
    }

    public static void setDefaultSkinType(CreepSkinType type) {
        defaultSkinType = type;
    }

    public static CreepSkinMode getDefaultSkinMode() {
        return defaultSkinMode;
    }

    public static void setDefaultSkinMode(CreepSkinMode mode) {
        defaultSkinMode = mode;
    }

    private static double clampSpeed(double speed) {
        if (speed < MIN_SPEED) {
            return MIN_SPEED;
        }
        if (speed > MAX_SPEED) {
            return MAX_SPEED;
        }
        return speed;
    }

    private static int clampFuseTicks(int ticks) {
        if (ticks < MIN_FUSE_TICKS) {
            return MIN_FUSE_TICKS;
        }
        if (ticks > MAX_FUSE_TICKS) {
            return MAX_FUSE_TICKS;
        }
        return ticks;
    }
}
