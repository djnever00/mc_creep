package com.example.creep;

import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.monster.Creeper;

public class CreepEntityRenderer extends CreeperRenderer {
    private static final float AGGRO_TINT_RANGE_BLOCKS = 32.0F;
    private static final float MAX_TINT_STRENGTH = 0.42F;
    private static final int CALM_TINT = ARGB.color(255, 130, 255, 130);
    private static final int MID_TINT = ARGB.color(255, 240, 235, 120);
    private static final int AGGRO_TINT = ARGB.color(255, 255, 170, 75);

    private static final Identifier BASE_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep.png");
    private static final Identifier TINT_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep_tint.png");
    private static final Identifier DIM_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep_dim.png");
    private static final Identifier DARK_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep_dark.png");
    private static final Identifier SPOT_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep_spot.png");
    private static final Identifier BLU_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep_blu.png");
    private static final Identifier CAMO_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep_camo.png");
    private static final Identifier ORA_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep_ora.png");
    private static final Identifier RED_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep_red.png");
    private static final Identifier SNOW_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep_snow.png");
    private static final Identifier YEL_TEXTURE = Identifier.fromNamespaceAndPath(CreepMod.MOD_ID, "textures/entity/creep/creep_yel.png");

    public CreepEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public CreepSkinRenderState createRenderState() {
        return new CreepSkinRenderState();
    }

    @Override
    public void extractRenderState(Creeper creeper, CreeperRenderState state, float partialTick) {
        super.extractRenderState(creeper, state, partialTick);
        if (state instanceof CreepSkinRenderState skinState && creeper instanceof PauseCreeperEntity daeCreep) {
            skinState.skinType = daeCreep.getSkinType();
            skinState.skinMode = daeCreep.getSkinMode();
            skinState.hasAggro = daeCreep.hasAggro();
            skinState.isPrimed = daeCreep.getSwellDir() > 0 || state.swelling > 0.01F;
            skinState.aggroBlend = 0.0F;
            if (daeCreep.getTarget() != null && daeCreep.getTarget().isAlive()) {
                float distance = (float) daeCreep.distanceTo(daeCreep.getTarget());
                float proximity = 1.0F - (distance / AGGRO_TINT_RANGE_BLOCKS);
                skinState.aggroBlend = Math.clamp(proximity, 0.0F, 1.0F);
            } else if (daeCreep.level() != null) {
                var nearest = daeCreep.level().getNearestPlayer(daeCreep, AGGRO_TINT_RANGE_BLOCKS);
                if (nearest != null) {
                    float distance = (float) daeCreep.distanceTo(nearest);
                    float proximity = 1.0F - (distance / AGGRO_TINT_RANGE_BLOCKS);
                    skinState.aggroBlend = Math.clamp(proximity, 0.0F, 1.0F);
                    skinState.hasAggro = true;
                }
            }
        }
    }

    @Override
    public Identifier getTextureLocation(CreeperRenderState state) {
        if (state instanceof CreepSkinRenderState skinState) {
            if (skinState.skinMode == CreepSkinMode.FADE) {
                return skinState.skinType == CreepSkinType.TINT ? TINT_TEXTURE : BASE_TEXTURE;
            }

            // Default dynamic behavior for "creep" skin: idle=creep, aggro=camo, primed=red.
            if (skinState.skinType == CreepSkinType.BASE) {
                if (skinState.isPrimed) {
                    return RED_TEXTURE;
                }
                if (skinState.hasAggro) {
                    return CAMO_TEXTURE;
                }
                return BASE_TEXTURE;
            }

            return switch (skinState.skinType) {
                case BASE -> BASE_TEXTURE;
                case TINT -> TINT_TEXTURE;
                case DIM -> DIM_TEXTURE;
                case DARK -> DARK_TEXTURE;
                case SPOT -> SPOT_TEXTURE;
                case BLU -> BLU_TEXTURE;
                case CAMO -> CAMO_TEXTURE;
                case ORA -> ORA_TEXTURE;
                case RED -> RED_TEXTURE;
                case SNOW -> SNOW_TEXTURE;
                case YEL -> YEL_TEXTURE;
            };
        }
        return BASE_TEXTURE;
    }

    @Override
    protected int getModelTint(CreeperRenderState state) {
        int baseTint = super.getModelTint(state);
        if (state instanceof CreepSkinRenderState skinState) {
            if (skinState.skinMode != CreepSkinMode.FADE) {
                return baseTint;
            }
            // Keep fade visually obvious even when aggro blend is low.
            float pulse = (float) ((Math.sin(state.ageInTicks * 0.35F) + 1.0D) * 0.5D);
            float effectiveBlend = Math.clamp(skinState.aggroBlend + (pulse * 0.20F), 0.0F, 1.0F);
            int gradientTint = effectiveBlend < 0.60F
                ? ARGB.srgbLerp(effectiveBlend / 0.60F, CALM_TINT, MID_TINT)
                : ARGB.srgbLerp((effectiveBlend - 0.60F) / 0.40F, MID_TINT, AGGRO_TINT);

            // Bias tint toward white so texture detail survives instead of crushing to red/black.
            int softenedTint = ARGB.srgbLerp(MAX_TINT_STRENGTH, ARGB.white(255), gradientTint);
            return ARGB.multiply(baseTint, softenedTint);
        }
        return baseTint;
    }

    public static final class CreepSkinRenderState extends CreeperRenderState {
        private CreepSkinType skinType = CreepSkinType.BASE;
        private CreepSkinMode skinMode = CreepSkinMode.STATIC;
        private boolean hasAggro;
        private boolean isPrimed;
        private float aggroBlend;
    }
}
