package com.gregtechceu.gtceu.client.renderer.machine.impl;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.client.renderer.block.BlockRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.core.mixins.GrowingPlantBlockAccessor;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;

public class GreenhouseRenderer extends DynamicRender<IRecipeLogicMachine, GreenhouseRenderer> {

    public static final Codec<GreenhouseRenderer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.VECTOR3F.listOf().fieldOf("offsets").forGetter(GreenhouseRenderer::getOffsets))
            .apply(instance, GreenhouseRenderer::new));

    public static final DynamicRenderType<IRecipeLogicMachine, GreenhouseRenderer> TYPE = new DynamicRenderType<>(
            GreenhouseRenderer.CODEC);

    @Getter
    private final List<Vector3f> offsets;

    public GreenhouseRenderer(List<Vector3f> offsets) {
        this.offsets = offsets;
    }

    @Override
    public DynamicRenderType<IRecipeLogicMachine, GreenhouseRenderer> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 32;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void render(IRecipeLogicMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!ConfigHolder.INSTANCE.client.renderer.renderGreenhouse) return;
        if (!machine.isActive()) return;
        final var recipeLogic = machine.getRecipeLogic();
        final var recipe = recipeLogic.getLastRecipe();
        if (Objects.isNull(recipe)) return;
        final var growingOpt = findGrowing(recipe);
        if (growingOpt.isEmpty()) return;
        final var growing = growingOpt.get();
        final var progress = recipeLogic.getProgressPercent();
        final var mode = growthModeForBlock(growing);
        var state = growing.defaultBlockState();
        if (mode == GrowthMode.AGE_3) {
            state = state.trySetValue(BlockStateProperties.AGE_3, Math.max(3, (int) (progress * 4)));
        } else if (mode == GrowthMode.AGE_7) {
            state = state.trySetValue(BlockStateProperties.AGE_7, Math.max(3, (int) (progress * 7)));
        }
        if (mode == GrowthMode.GROWING_PLANT) {
            if (progress < 0.5) {
                state = ((GrowingPlantBlockAccessor) state.getBlock()).getHeadBlock().defaultBlockState();
                state = state.trySetValue(BlockStateProperties.AGE_25, (int) (progress * (25 - 1e-25)));
            } else {
                state = ((GrowingPlantBlockAccessor) state.getBlock()).getBodyBlock().defaultBlockState();
            }
            if (progress % 0.5 >= 0.25 && state.getBlock() instanceof CaveVines) {
                state = state.trySetValue(CaveVines.BERRIES, true);
            }
        }
        final var renderType = ItemBlockRenderTypes.getRenderType(state, false);
        final var consumer = buffer.getBuffer(renderType);
        for (final Vector3fc offset : this.getOffsets()) {
            poseStack.pushPose();
            final var rotated = new Vector3f(offset);
            rotated.rotateX((float) Math.PI / -2);
            machine.self().getFrontFacing().getRotation().transform(rotated);
            poseStack.last().pose().translate(rotated);
            poseStack.translate(0, 1e-25, 0);
            if (mode == GrowthMode.TRANSLATE) {
                poseStack.translate(0.0, progress - 1, 0.0);
            } else if (mode == GrowthMode.TALL_FLOWER || mode == GrowthMode.DOUBLE_TRANSLATE ||
                    mode == GrowthMode.GROWING_PLANT) {
                        poseStack.translate(0.0, (progress * 2) % (1 + 1e-25) - 1, 0.0);
                    } else
                if (mode == GrowthMode.SCALE) {
                    poseStack.last().pose().scaleAround((float) progress, 0.5f, 0.0f, 0.5f);
                }
            if (mode == GrowthMode.GROWING_PLANT && state.getBlock() instanceof GrowingPlantBlock gp) {
                poseStack.last().pose().rotateAround(
                        ((GrowingPlantBlockAccessor) gp).getGrowthDirection().getRotation(), 0.5f, 0.5f, 0.5f);
            }
            BlockRenderer.drawBlock(consumer, state, poseStack.last(), renderType);
            poseStack.popPose();
        }
        if ((mode == GrowthMode.TALL_FLOWER || mode == GrowthMode.DOUBLE_TRANSLATE ||
                mode == GrowthMode.GROWING_PLANT) && progress > 0.5) {
            var upperState = state;
            if (mode == GrowthMode.TALL_FLOWER) {
                upperState = state.trySetValue(DoublePlantBlock.HALF,
                        DoublePlantBlock.HALF.getValue("upper").orElseThrow());
            } else if (mode == GrowthMode.GROWING_PLANT) {
                upperState = ((GrowingPlantBlockAccessor) state.getBlock()).getHeadBlock().defaultBlockState();
                upperState = upperState.trySetValue(BlockStateProperties.AGE_25, (int) (progress * (25 - 1e-25)));
                if (state.getBlock() instanceof CaveVines) {
                    upperState = upperState.trySetValue(CaveVines.BERRIES, true);
                }
            }
            for (final Vector3fc offset : getOffsets()) {
                poseStack.pushPose();
                final var rotated = new Vector3f(offset);
                rotated.rotateX((float) Math.PI / -2);
                machine.self().getFrontFacing().getRotation().transform(rotated);
                poseStack.last().pose().translate(rotated);
                poseStack.translate(0, 1 + 1e-25, 0);
                poseStack.translate(0.0, (progress * 2) % (1 + 1e-25) - 1, 0.0);
                if (mode == GrowthMode.GROWING_PLANT && state.getBlock() instanceof GrowingPlantBlock gp) {
                    poseStack.last().pose().rotateAround(
                            ((GrowingPlantBlockAccessor) gp).getGrowthDirection().getRotation(), 0.5f, 0.5f, 0.5f);
                }
                BlockRenderer.drawBlock(consumer, upperState, poseStack.last(), renderType);
                poseStack.popPose();
            }
        }
    }

    protected Optional<Block> findGrowing(GTRecipe recipe) {
        return RECIPE_BLOCK_CACHE.apply(recipe);
    }

    private static final Function<GTRecipe, Optional<Block>> RECIPE_BLOCK_CACHE = GTMemoizer.memoizeFunctionWeakIdent(recipe -> {
        List<Content> allItemContents = new ArrayList<>();
        allItemContents.addAll(recipe.getInputContents(ItemRecipeCapability.CAP));
        allItemContents.addAll(recipe.getTickInputContents(ItemRecipeCapability.CAP));
        allItemContents.addAll(recipe.getOutputContents(ItemRecipeCapability.CAP));
        allItemContents.addAll(recipe.getTickOutputContents(ItemRecipeCapability.CAP));
        return allItemContents.stream()
                .map(Content::getContent).map(ItemRecipeCapability.CAP::of)
                .map(Ingredient::getItems).flatMap(Arrays::stream)
                .map(ItemStack::getItem)
                .filter(BlockItem.class::isInstance)
                .findFirst()
                .map(BlockItem.class::cast)
                .map(BlockItem::getBlock);
    });

    protected GrowthMode growthModeForBlock(Block block) {
        if (block instanceof CropBlock || block instanceof StemBlock) return GrowthMode.AGE_7;
        if (block instanceof DoublePlantBlock) return GrowthMode.TALL_FLOWER;
        if (block instanceof FlowerBlock || block instanceof MangrovePropaguleBlock) return GrowthMode.TRANSLATE;
        if (block instanceof SaplingBlock) return GrowthMode.SCALE;
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock) return GrowthMode.DOUBLE_TRANSLATE;
        if (block instanceof SweetBerryBushBlock) return GrowthMode.AGE_3;
        if (block instanceof GrowingPlantBlock) return GrowthMode.GROWING_PLANT;
        return GrowthMode.NONE;
    }

    public enum GrowthMode {
        NONE,
        SCALE,
        TRANSLATE,
        DOUBLE_TRANSLATE,
        AGE_3,
        AGE_7,
        TALL_FLOWER,
        GROWING_PLANT
    }
}
