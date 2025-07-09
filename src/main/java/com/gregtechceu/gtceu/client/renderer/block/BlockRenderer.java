package com.gregtechceu.gtceu.client.renderer.block;

import com.lowdragmc.lowdraglib.client.model.ModelFactory;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BlockRenderer {

    public static final Map<BlockState, BakedModel> BLOCK_MODEL_CACHE = new HashMap<>();

    public static void drawBlock(VertexConsumer consumer, BlockState state, PoseStack.Pose pose,
                                 RenderType renderType) {
        final var model = BLOCK_MODEL_CACHE.computeIfAbsent(state, (_state) -> {
            final var location = BlockModelShaper.stateToModelLocation(state);
            final var unbaked = ModelFactory.getUnBakedModel(location);
            return Objects.requireNonNull(unbaked.bake(ModelFactory.getModeBaker(), Material::sprite,
                    ModelFactory.getRotation(Direction.NORTH), location));
        });
        drawFace(consumer, model, null, state, renderType, pose, LightTexture.FULL_BRIGHT);
        for (final var face : Direction.values()) {
            drawFace(consumer, model, face, state, renderType, pose, LightTexture.FULL_BRIGHT);
        }
    }

    private static void drawFace(VertexConsumer consumer, BakedModel model, Direction direction, BlockState state,
                                 RenderType renderType, PoseStack.Pose pose, int light) {
        final var quads = Objects
                .requireNonNull(model.getQuads(state, direction, RandomSource.create(42), ModelData.EMPTY, renderType));
        for (final var quad : quads) {
            consumer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, light, OverlayTexture.NO_OVERLAY);
        }
    }
}
