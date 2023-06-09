package com.molybdenum.alloyed.mixin;

import com.molybdenum.alloyed.client.registry.ModPartialModels;
import com.molybdenum.alloyed.common.content.extensions.BeltBlockEntityExtension;
import com.molybdenum.alloyed.common.content.extensions.BeltModelExtension;
import com.molybdenum.alloyed.common.registry.ModSpriteShifts;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltModel;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin(BeltModel.class)
public class BeltModelMixin implements BeltModelExtension {

    @Inject(
            method = "getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Ljava/util/Random;Lnet/minecraftforge/client/model/data/ModelData;)Ljava/util/List;",
            at = @At(value = "RETURN", ordinal = 1),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true,
            remap = false
    )
    private void handleAlloyedCasingRendering(BlockState state, Direction side, Random rand, ModelData extraData, CallbackInfoReturnable<List<BakedQuad>> cir, List<BakedQuad> quads, boolean cover, BeltBlockEntity.CasingType type, boolean brassCasing) {
        BeltBlockEntityExtension.AlloyedCasingType alloyedType = extraData.get(ALLOYED_CASING_PROPERTY);
        if (alloyedType == BeltBlockEntityExtension.AlloyedCasingType.NONE) return;

        List<BakedQuad> newQuads = new ArrayList<>(quads);

        if (cover) {
            boolean alongX = state.getValue(BeltBlock.HORIZONTAL_FACING)
                    .getAxis() == Direction.Axis.X;
            BakedModel coverModel =
                    (alongX ? ModPartialModels.STEEL_BELT_COVER_X : ModPartialModels.STEEL_BELT_COVER_Z).get();
            newQuads.addAll(coverModel.getQuads(state, side, rand, extraData));
        }

        for (int i = 0; i < newQuads.size(); i++) {
            BakedQuad quad = newQuads.get(i);
            TextureAtlasSprite original = quad.getSprite();
            if (original != ModSpriteShifts.STEEL_BELT_CASING.getOriginal())
                continue;

            BakedQuad newQuad = BakedQuadHelper.clone(quad);
            int[] vertexData = newQuad.getVertices();

            for (int vertex = 0; vertex < 4; vertex++) {
                float u = BakedQuadHelper.getU(vertexData, vertex);
                float v = BakedQuadHelper.getV(vertexData, vertex);
                BakedQuadHelper.setU(vertexData, vertex, ModSpriteShifts.STEEL_BELT_CASING.getTargetU(u));
                BakedQuadHelper.setV(vertexData, vertex, ModSpriteShifts.STEEL_BELT_CASING.getTargetV(v));
            }

            newQuads.set(i, newQuad);
        }

        cir.setReturnValue(newQuads);
    }

    @Inject(
            method = "getParticleIcon(Lnet/minecraftforge/client/model/data/ModelData;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void returnAlloyedSpritesIfNeeded(ModelData data, CallbackInfoReturnable<TextureAtlasSprite> cir) {
        if (!data.has(ALLOYED_CASING_PROPERTY)) return;
        if (data.get(ALLOYED_CASING_PROPERTY) == BeltBlockEntityExtension.AlloyedCasingType.STEEL) {
            cir.setReturnValue(ModSpriteShifts.STEEL_CASING.getOriginal());
            cir.cancel();
        }
    }
}
