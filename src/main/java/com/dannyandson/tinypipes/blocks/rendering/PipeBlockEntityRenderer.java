package com.dannyandson.tinypipes.blocks.rendering;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.components.full.AbstractCapFullPipe;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.components.full.RedstonePipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity, PipeBlockEntityRenderState> {

    private static ModelBlockRenderer cachedModelRenderer;

    /**
     * When non-null, renderGeometry uses this RenderType instead of Sheets.cutoutBlockSheet().
     * Set by PiP renderers to bypass the lightmap.
     */
    public static RenderType overrideRenderType = null;

    public PipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PipeBlockEntityRenderState createRenderState() {
        return new PipeBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(PipeBlockEntity be, PipeBlockEntityRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumblingOverlay);
        state.pipeBlockEntity = be;
        state.cachedRenderer = be.getCachedRenderer();
    }

    @Override
    public void submit(PipeBlockEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        PipeBlockEntity pipeBlockEntity = state.pipeBlockEntity;
        if (pipeBlockEntity == null) return;

        CachedPipeRenderer cache = state.cachedRenderer;
        int combinedLight = state.lightCoords;

        // Use the immediate buffer source for custom vertex rendering
        MultiBufferSource.BufferSource bufferSource =
                Minecraft.getInstance().renderBuffers().bufferSource();

        // Check if we need to rebuild the cache
        if (cache.isDirty() || cache.lightChanged(combinedLight)) {
            cache.rebuild(
                    (capturePoseStack, captureBuffer) ->
                            renderGeometry(pipeBlockEntity, capturePoseStack, captureBuffer, combinedLight, 0),
                    combinedLight
            );
        }

        // Replay cached vertices with the real PoseStack
        cache.replay(poseStack, bufferSource, combinedLight);

        // BER: No endBatch needed — the level renderer manages buffer lifecycle.
    }

    /**
     * The actual geometry generation logic.
     * Called during cache rebuild with a capture PoseStack (identity) and capture buffer.
     */
    public static void renderGeometry(PipeBlockEntity pipeBlockEntity, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        renderGeometry(pipeBlockEntity, poseStack, buffer, combinedLight, combinedOverlay, 1.0f);
    }

    public static void renderGeometry(PipeBlockEntity pipeBlockEntity, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, float alpha) {
        RenderType rt = overrideRenderType != null ? overrideRenderType : Sheets.cutoutBlockSheet();
        VertexConsumer builder = buffer.getBuffer(rt);

        if (pipeBlockEntity.getCamouflageBlockState() != null) {
            BlockState camouflageState = pipeBlockEntity.getCamouflageBlockState();
            renderCamouflageBlock(pipeBlockEntity, camouflageState, builder, poseStack, alpha);
            return;
        }

        TextureAtlasSprite sprite = pipeBlockEntity.getCenterSprite();
        AbstractFullPipe[] pipes = pipeBlockEntity.getPipes();
        boolean single = pipes.length == 1;

        poseStack.pushPose();

        //draw center cube
        if (single)
            RenderHelper.drawCube(poseStack, builder, sprite, 0.4f, 0.6f, 0.4f, 0.6f, 0.4f, 0.6f, combinedLight, 0xFFFFFFFF, alpha);
        else
            RenderHelper.drawCube(poseStack, builder, sprite, 0.3125f, 0.6875f, 0.3125f, 0.6875f, 0.3125f, 0.6875f, combinedLight, 0xFFFFFFFF, alpha);

        for (AbstractFullPipe pipe : pipes) {
            RedstonePipe rsPipe = (pipe instanceof RedstonePipe) ? (RedstonePipe) pipe : null;
            int color = pipe.getColor();
            int upgrades = (pipe instanceof AbstractCapFullPipe) ? ((AbstractCapFullPipe<?>) pipe).getSpeedUpgradeCount() : 0;
            Integer upgradeColor = (upgrades > 0) ? 0xFF226600 + 0xFF / Config.SPEED_UPGRADE_MAX.get() * upgrades : 0xFF222222;
            poseStack.pushPose();

            int slot = (single) ? -1 : pipe.slotPos();
            sprite = pipe.getSprite();
            for (Direction direction : new Direction[]{Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST}) {
                Integer connectionColor = (rsPipe != null) ? rsPipe.getColor(direction) : (!pipe.getNeighborHasSamePipeType(direction)) ? upgradeColor : null;
                drawSide(pipe.getPipeSideStatus(direction), slot, poseStack, builder, sprite, combinedLight, direction.getAxisDirection(), color, connectionColor, pipe.getNeighborIsPipeCluster(direction), alpha);
                poseStack.translate(0, 0, 1);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }

            Integer connectionColor = (rsPipe != null) ? rsPipe.getColor(Direction.UP) : (!pipe.getNeighborHasSamePipeType(Direction.UP)) ? upgradeColor : null;
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.translate(0, 0, -1);
            drawSide(pipe.getPipeSideStatus(Direction.UP), slot, poseStack, builder, sprite, combinedLight, Direction.AxisDirection.POSITIVE, color, connectionColor, pipe.getNeighborIsPipeCluster(Direction.UP), alpha);

            connectionColor = (rsPipe != null) ? rsPipe.getColor(Direction.DOWN) : (!pipe.getNeighborHasSamePipeType(Direction.DOWN)) ? upgradeColor : null;
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-1, 0, -1);
            drawSide(pipe.getPipeSideStatus(Direction.DOWN), slot, poseStack, builder, sprite, combinedLight, Direction.AxisDirection.NEGATIVE, color, connectionColor, pipe.getNeighborIsPipeCluster(Direction.DOWN), alpha);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * Renders a camouflage block using the tesselateBlock adapter pattern.
     * Uses UP normals to prevent the Sheets shader from double-applying face shading,
     * since tesselateBlock already bakes shading into vertex colors.
     * PoseStack transform is applied to each vertex for PiP compatibility.
     */
    private static void renderCamouflageBlock(PipeBlockEntity tile, BlockState camoState,
                                              VertexConsumer builder, PoseStack poseStack, float alpha) {
        if (tile.getLevel() == null) return;

        var modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        var model = modelSet.get(camoState);
        if (model == null) return;

        ModelBlockRenderer modelRenderer = getOrCreateModelRenderer();
        Matrix4f matrix = poseStack.last().pose();
        int alphaInt = (int)(alpha * 255f);

        modelRenderer.tesselateBlock(
                (var x, var y, var z, var quad, var instance) -> {
                    int lightEmission = quad.materialInfo().lightEmission();
                    for (int vertex = 0; vertex < 4; vertex++) {
                        Vector3fc pos = quad.position(vertex);
                        long packedUv = quad.packedUV(vertex);
                        int vertexColor = ARGB.multiply(
                                instance.getColor(vertex),
                                quad.bakedColors().color(vertex));
                        // Apply alpha
                        vertexColor = ARGB.color(alphaInt,
                                ARGB.red(vertexColor), ARGB.green(vertexColor), ARGB.blue(vertexColor));
                        int light = instance.getLightCoordsWithEmission(vertex, lightEmission);
                        float u = UVPair.unpackU(packedUv);
                        float v = UVPair.unpackV(packedUv);
                        // Transform through PoseStack; UP normal prevents double-shading
                        int overlay = instance.overlayCoords();
                        builder.addVertex(matrix, pos.x() + x, pos.y() + y, pos.z() + z)
                                .setColor(vertexColor)
                                .setUv(u, v)
                                .setUv1(overlay & 0xFFFF, (overlay >> 16) & 0xFFFF)
                                .setUv2(light & 0xFFFF, (light >> 16) & 0xFFFF)
                                .setNormal(0f, 1f, 0f);
                    }
                },
                0f, 0f, 0f,
                (BlockAndTintGetter) tile.getLevel(), tile.getBlockPos(),
                camoState, model,
                camoState.getSeed(tile.getBlockPos())
        );
    }

    private static ModelBlockRenderer getOrCreateModelRenderer() {
        if (cachedModelRenderer == null) {
            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            cachedModelRenderer = new ModelBlockRenderer(true, false, blockColors);
        }
        return cachedModelRenderer;
    }

    private static void drawSide(PipeConnectionState sideStatus, int slot, PoseStack poseStack, VertexConsumer builder, TextureAtlasSprite sprite, int combinedLight, Direction.AxisDirection dir, int pipeColor, Integer connectionColor, Boolean clusterNeighbor, float alpha) {
        boolean alt = dir == Direction.AxisDirection.NEGATIVE;
        boolean xRight = (slot == 0 && alt) || (slot == 1 && !alt) || (slot == 2 && alt) || (slot == 3 && !alt);
        boolean yUpper = slot == 0 || slot == 1;

        float xmin = slot == -1 ? 0.4296875f : xRight ? 0.5f : 0.359375f;
        float xmax = slot == -1 ? 0.5703125f : xRight ? 0.640625f : 0.5f;
        float zmin = slot == -1 ? 0.4296875f : yUpper ? 0.5f : 0.359375f;
        float zmax = slot == -1 ? 0.5703125f : yUpper ? 0.640625f : 0.5f;
        float ymax = slot == -1 ? 0.4296875f : 0.3125f;

        if (sideStatus == PipeConnectionState.ENABLED) {
            if (slot == -1 && clusterNeighbor != null && clusterNeighbor) {
                RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0.0625f, ymax, zmin, zmax, combinedLight, pipeColor, alpha);
                RenderHelper.drawCube(poseStack, builder, sprite, 0.359375f, 0.640625f, 0, 0.0625f, 0.359375f, 0.640625f, combinedLight, pipeColor, alpha);
            } else if (connectionColor == null) {
                RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0, ymax, zmin, zmax, combinedLight, pipeColor, alpha);
            } else {
                RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0, ymax, zmin, zmax, combinedLight, pipeColor, alpha);
                RenderHelper.drawCube(poseStack, builder, PipeBlockEntity.getWhitePipeSprite(), xmin - .005f, xmax + .005f, 0.0625f, 0.125f, zmin - .005f, zmax + .005f, combinedLight, connectionColor, alpha);
            }
        } else if (sideStatus == PipeConnectionState.PULLING) {
            RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0.125f, ymax, zmin, zmax, combinedLight, pipeColor, alpha);
            RenderHelper.drawCube(poseStack, builder, PipeBlockEntity.getPullSprite(), xmin, xmax, 0, 0.125f, zmin, zmax, combinedLight, (connectionColor == null) ? 0xFFFFFFFF : connectionColor, alpha);
        } else if (slot != -1) {
            RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0.28125f, 0.3125f, zmin, zmax, combinedLight, pipeColor, alpha);
        }
    }
}