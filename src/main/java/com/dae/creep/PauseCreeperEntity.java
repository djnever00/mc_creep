package com.dae.creep;

import java.lang.reflect.Field;
import java.util.EnumSet;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

public class PauseCreeperEntity extends Creeper {
    private static final int EXPLOSION_RADIUS = 5;
    private static final int HISS_MIN_INTERVAL_TICKS = 600;
    private static final int HISS_MAX_INTERVAL_TICKS = 2400;
    private static final double HISS_TRIGGER_DISTANCE_SQR = 16.0D;
    private static final double DOUBLE_AGGRO_RANGE = 32.0D;
    private static final double PAUSE_DISTANCE_SQR = 4.0D;
    private static final Field CREEPER_MAX_SWELL_FIELD = findCreeperMaxSwellField();
    private static final Field CREEPER_EXPLOSION_RADIUS_FIELD = findCreeperExplosionRadiusField();
    private static final EntityDataAccessor<Integer> DATA_SKIN_TYPE =
        SynchedEntityData.defineId(PauseCreeperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKIN_MODE =
        SynchedEntityData.defineId(PauseCreeperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_AGGRO =
        SynchedEntityData.defineId(PauseCreeperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_PRIMED =
        SynchedEntityData.defineId(PauseCreeperEntity.class, EntityDataSerializers.BOOLEAN);

    private int nextAmbientHissTick;
    private boolean fusePrimedByTargetDamage;
    private CreepStareMode assignedStareMode;

    public PauseCreeperEntity(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.scheduleNextAmbientHiss();
        this.syncRuntimeConfig();
        this.setExplosionRadius(EXPLOSION_RADIUS);
        this.assignRandomStareMode();
        this.setSkinType(CreepBehaviorConfig.getDefaultSkinType());
        this.setSkinMode(CreepBehaviorConfig.getDefaultSkinMode());
    }

    public static AttributeSupplier.Builder createPauseCreeperAttributes() {
        return Creeper.createAttributes()
            .add(Attributes.FOLLOW_RANGE, DOUBLE_AGGRO_RANGE)
            .add(Attributes.MOVEMENT_SPEED, CreepBehaviorConfig.getWalkSpeed());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new PauseInsteadOfExplodeGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN_TYPE, CreepSkinType.BASE.ordinal());
        builder.define(DATA_SKIN_MODE, CreepSkinMode.STATIC.ordinal());
        builder.define(DATA_HAS_AGGRO, false);
        builder.define(DATA_IS_PRIMED, false);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("DaeSkinType", this.getSkinType().id());
        output.putString("DaeSkinMode", this.getSkinMode().id());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setSkinType(CreepSkinType.fromId(input.getStringOr("DaeSkinType", CreepSkinType.BASE.id())));
        this.setSkinMode(CreepSkinMode.fromId(input.getStringOr("DaeSkinMode", CreepSkinMode.STATIC.id())));
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = this.getTarget();
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_HAS_AGGRO, target != null && target.isAlive());
            this.entityData.set(DATA_IS_PRIMED, this.fusePrimedByTargetDamage && CreepBehaviorConfig.isPermitDetonate());
        }
        if (this.fusePrimedByTargetDamage && CreepBehaviorConfig.isPermitDetonate()) {
            this.setSwellDir(1);
        } else {
            if (!CreepBehaviorConfig.isPermitDetonate()) {
                this.fusePrimedByTargetDamage = false;
            }
            this.setSwellDir(-1);
        }
        this.updateMirrorSpeed();
        this.tickAmbientHiss();
    }

    public void syncRuntimeConfig() {
        this.setFuseTicks(CreepBehaviorConfig.getFuseTicks());
    }

    public void setAssignedStareMode(CreepStareMode mode) {
        this.assignedStareMode = mode;
    }

    public CreepStareMode getAssignedStareMode() {
        if (this.assignedStareMode == null) {
            this.assignRandomStareMode();
        }
        return this.assignedStareMode;
    }

    private void assignRandomStareMode() {
        this.assignedStareMode = this.random.nextBoolean() ? CreepStareMode.SCOPE : CreepStareMode.MIRROR;
    }

    public CreepSkinType getSkinType() {
        int ordinal = this.entityData.get(DATA_SKIN_TYPE);
        CreepSkinType[] values = CreepSkinType.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return CreepSkinType.BASE;
        }
        return values[ordinal];
    }

    public void setSkinType(CreepSkinType type) {
        this.entityData.set(DATA_SKIN_TYPE, type.ordinal());
    }

    public CreepSkinMode getSkinMode() {
        int ordinal = this.entityData.get(DATA_SKIN_MODE);
        CreepSkinMode[] values = CreepSkinMode.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return CreepSkinMode.STATIC;
        }
        return values[ordinal];
    }

    public void setSkinMode(CreepSkinMode mode) {
        this.entityData.set(DATA_SKIN_MODE, mode.ordinal());
    }

    public boolean hasAggro() {
        return this.entityData.get(DATA_HAS_AGGRO);
    }

    public boolean isPrimedForDetonation() {
        return this.entityData.get(DATA_IS_PRIMED);
    }

    @Override
    public void ignite() {
        // Suppress ignition so this mob pauses at close range instead of exploding.
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (!hurt || !CreepBehaviorConfig.isPermitDetonate()) {
            return hurt;
        }

        LivingEntity target = this.getTarget();
        if (source.getEntity() instanceof LivingEntity attacker && target != null && attacker == target) {
            this.fusePrimedByTargetDamage = true;
            this.setFuseTicks(CreepBehaviorConfig.getFuseTicks());
            this.setSwellDir(1);
            this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 1.0F);
        }

        return hurt;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    private void updateMirrorSpeed() {
        AttributeInstance speedAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) {
            return;
        }

        LivingEntity target = this.getTarget();
        double speed = CreepBehaviorConfig.getWalkSpeed();
        if (target instanceof Player player && player.isSprinting()) {
            speed = CreepBehaviorConfig.getRunSpeed();
        }

        if (Math.abs(speedAttribute.getBaseValue() - speed) > 0.0001D) {
            speedAttribute.setBaseValue(speed);
        }
    }

    private void tickAmbientHiss() {
        if (this.level().isClientSide()) {
            return;
        }

        if (this.tickCount < this.nextAmbientHissTick) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.distanceToSqr(target) <= HISS_TRIGGER_DISTANCE_SQR) {
            this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 1.0F);
        }

        this.scheduleNextAmbientHiss();
    }

    private void scheduleNextAmbientHiss() {
        int interval = HISS_MIN_INTERVAL_TICKS + this.random.nextInt((HISS_MAX_INTERVAL_TICKS - HISS_MIN_INTERVAL_TICKS) + 1);
        this.nextAmbientHissTick = this.tickCount + interval;
    }

    private static Field findCreeperMaxSwellField() {
        try {
            Field field = Creeper.class.getDeclaredField("maxSwell");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Field findCreeperExplosionRadiusField() {
        try {
            Field field = Creeper.class.getDeclaredField("explosionRadius");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private void setFuseTicks(int ticks) {
        if (CREEPER_MAX_SWELL_FIELD == null) {
            return;
        }

        try {
            CREEPER_MAX_SWELL_FIELD.setInt(this, ticks);
        } catch (IllegalAccessException ignored) {
        }
    }

    private void setExplosionRadius(int radius) {
        if (CREEPER_EXPLOSION_RADIUS_FIELD == null) {
            return;
        }

        try {
            CREEPER_EXPLOSION_RADIUS_FIELD.setInt(this, radius);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static final class PauseInsteadOfExplodeGoal extends Goal {
        private static final int MIRROR_UPDATE_TICKS = 5;

        private final PauseCreeperEntity creeper;
        private int nextHeadJoltTick;
        private double headOffsetY;
        private int scopeBand;
        private int nextMirrorUpdateTick;

        private PauseInsteadOfExplodeGoal(PauseCreeperEntity creeper) {
            this.creeper = creeper;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.creeper.getTarget();
            return target != null && target.isAlive() && this.creeper.distanceToSqr(target) <= PAUSE_DISTANCE_SQR;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.creeper.getNavigation().stop();
            this.scheduleNextHeadJolt(0);
            this.nextMirrorUpdateTick = this.creeper.tickCount;
        }

        @Override
        public void stop() {
            this.headOffsetY = 0.0D;
            this.scopeBand = 0;
            this.nextMirrorUpdateTick = this.creeper.tickCount;
        }

        @Override
        public void tick() {
            LivingEntity target = this.creeper.getTarget();
            if (target != null) {
                this.creeper.getNavigation().stop();
                if (this.creeper.getAssignedStareMode() == CreepStareMode.MIRROR) {
                    if (this.creeper.tickCount >= this.nextMirrorUpdateTick) {
                        this.applyMirrorStare(target);
                        this.nextMirrorUpdateTick = this.creeper.tickCount + MIRROR_UPDATE_TICKS;
                    }
                    return;
                }

                if (this.creeper.tickCount >= this.nextHeadJoltTick) {
                    this.scopeBand = this.creeper.getRandom().nextInt(3);
                    this.headOffsetY = -1.65D + (this.creeper.getRandom().nextDouble() * 3.30D);
                    this.scheduleNextHeadJolt(16 + this.creeper.getRandom().nextInt(65));
                }

                double lookY;
                if (this.scopeBand == 0) {
                    lookY = target.getY() - 0.80D;
                } else if (this.scopeBand == 1) {
                    lookY = target.getEyeY() + this.headOffsetY;
                } else {
                    lookY = target.getEyeY() + 1.60D;
                }
                this.forceSnapLookAt(target, lookY);
            }
        }

        private void scheduleNextHeadJolt(int delayTicks) {
            this.nextHeadJoltTick = this.creeper.tickCount + delayTicks;
        }

        private void forceSnapLookAt(LivingEntity target, double lookY) {
            double dx = target.getX() - this.creeper.getX();
            double dz = target.getZ() - this.creeper.getZ();
            double dy = lookY - this.creeper.getEyeY();
            double horizontal = Math.sqrt((dx * dx) + (dz * dz));

            float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            float pitch = (float) (-(Mth.atan2(dy, horizontal) * (180.0D / Math.PI)));
            pitch = Mth.clamp(pitch, -89.0F, 89.0F);

            this.creeper.setYRot(yaw);
            this.creeper.setYHeadRot(yaw);
            this.creeper.setYBodyRot(yaw);
            this.creeper.setXRot(pitch);
        }

        private void applyMirrorStare(LivingEntity target) {
            float yaw = Mth.wrapDegrees(target.getYHeadRot() + 180.0F);
            float pitch = Mth.clamp(target.getXRot(), -89.0F, 89.0F);
            this.creeper.setYRot(yaw);
            this.creeper.setYHeadRot(yaw);
            this.creeper.setYBodyRot(yaw);
            this.creeper.setXRot(pitch);
        }
    }
}
