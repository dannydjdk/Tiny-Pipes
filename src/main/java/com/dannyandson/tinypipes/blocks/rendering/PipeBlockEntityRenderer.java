package com.dannyandson.tinypipes.blocks.rendering;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.components.full.AbstractCapFullPipe;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.components.full.RedstonePipe;
import com.dannyandson.tinypipes.components.full.RefinedStorageCablePipe;
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
            if (pipe instanceof RefinedStorageCablePipe cable) {
                // RS cable renders as a small center core with nubs toward connected sides, not 2x2 quadrants.
                drawRsCable(poseStack, builder, cable, combinedLight, alpha);
                continue;
            }
            RedstonePipe rsPipe = (pipe instanceof RedstonePipe) ? (RedstonePipe) pipe : null;
            int color = pipe.getColor();
            int upgrades = (pipe instanceof AbstractCapFullPipe) ? ((AbstractCapFullPipe<?>) pipe).getSpeedUpgradeCount() : 0;
            // Reserved for the upcoming channel feature.
            Integer channelColor = 0xFF222222;
            int[] chevronColors = computeChevronColors(upgrades);
            poseStack.pushPose();

            int slot = (single) ? -1 : pipe.slotPos();
            sprite = pipe.getSprite();
            for (Direction direction : new Direction[]{Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST}) {
                Integer connectionColor = (rsPipe != null) ? rsPipe.getColor(direction) : (!pipe.getNeighborHasSamePipeType(direction)) ? channelColor : null;
                drawSide(pipe.getPipeSideStatus(direction), slot, poseStack, builder, sprite, combinedLight, direction.getAxisDirection(), color, connectionColor, pipe.getNeighborIsPipeCluster(direction), chevronColors, alpha);
                poseStack.translate(0, 0, 1);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }

            Integer connectionColor = (rsPipe != null) ? rsPipe.getColor(Direction.UP) : (!pipe.getNeighborHasSamePipeType(Direction.UP)) ? channelColor : null;
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.translate(0, 0, -1);
            drawSide(pipe.getPipeSideStatus(Direction.UP), slot, poseStack, builder, sprite, combinedLight, Direction.AxisDirection.POSITIVE, color, connectionColor, pipe.getNeighborIsPipeCluster(Direction.UP), chevronColors, alpha);

            connectionColor = (rsPipe != null) ? rsPipe.getColor(Direction.DOWN) : (!pipe.getNeighborHasSamePipeType(Direction.DOWN)) ? channelColor : null;
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-1, 0, -1);
            drawSide(pipe.getPipeSideStatus(Direction.DOWN), slot, poseStack, builder, sprite, combinedLight, Direction.AxisDirection.NEGATIVE, color, connectionColor, pipe.getNeighborIsPipeCluster(Direction.DOWN), chevronColors, alpha);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * Renders the Refined Storage cable: a small center core plus a thin nub toward each connected side.
     * Drawn in block-local space using the outer PoseStack (no per-pipe push/pop, like the center cube).
     */
    private static void drawRsCable(PoseStack poseStack, VertexConsumer builder, RefinedStorageCablePipe cable, int combinedLight, float alpha) {
        TextureAtlasSprite white = PipeBlockEntity.getWhitePipeSprite();
        int color = cable.getColor();
        float lo = 0.45f, hi = 0.55f;

        // center core
        RenderHelper.drawCube(poseStack, builder, white, lo, hi, lo, hi, lo, hi, combinedLight, color, alpha);

        // Thin nub from the core out to the face for each connected side.
        // drawCube's parameters are NOT world-aligned: its x-param maps to (1 - worldX), its y-param
        // maps to world Z (depth), and its z-param maps to world Y (height). Map each world Direction
        // onto the correct drawCube parameter accordingly.
        for (Direction d : Direction.values()) {
            if (!cable.isConnected(d)) continue;
            float x0 = lo, x1 = hi, y0 = lo, y1 = hi, z0 = lo, z1 = hi;
            switch (d) {
                case EAST  -> { x0 = 0f; x1 = lo; }   // world +X (x-param is flipped)
                case WEST  -> { x0 = hi; x1 = 1f; }   // world -X
                case UP    -> { z0 = hi; z1 = 1f; }   // world +Y (z-param is height)
                case DOWN  -> { z0 = 0f; z1 = lo; }   // world -Y
                case SOUTH -> { y0 = hi; y1 = 1f; }   // world +Z (y-param is depth)
                case NORTH -> { y0 = 0f; y1 = lo; }   // world -Z
            }
            RenderHelper.drawCube(poseStack, builder, white, x0, x1, y0, y1, z0, z1, combinedLight, color, alpha);
        }
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

    private static void drawSide(PipeConnectionState sideStatus, int slot, PoseStack poseStack, VertexConsumer builder, TextureAtlasSprite sprite, int combinedLight, Direction.AxisDirection dir, int pipeColor, Integer connectionColor, Boolean clusterNeighbor, int[] chevronColors, float alpha) {
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
            // Speed-upgrade chevrons point outward (in the output flow direction).
            if (connectionColor != null && chevronColors.length > 0)
                drawChevrons(poseStack, builder, combinedLight, slot, xmin, xmax, zmin, zmax, ymax, chevronColors, true, alpha);
        } else if (sideStatus == PipeConnectionState.PULLING) {
            RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0.125f, ymax, zmin, zmax, combinedLight, pipeColor, alpha);
            RenderHelper.drawCube(poseStack, builder, PipeBlockEntity.getPullSprite(), xmin, xmax, 0, 0.125f, zmin, zmax, combinedLight, (connectionColor == null) ? 0xFFFFFFFF : connectionColor, alpha);
            // Speed-upgrade chevrons point inward (in the pull flow direction).
            if (connectionColor != null && chevronColors.length > 0)
                drawChevrons(poseStack, builder, combinedLight, slot, xmin, xmax, zmin, zmax, ymax, chevronColors, false, alpha);
        } else if (slot != -1) {
            RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0.28125f, 0.3125f, zmin, zmax, combinedLight, pipeColor, alpha);
        }
    }

    // --- Speed-upgrade chevrons -------------------------------------------------------------
    // Speed upgrades are shown as chevron tiles along the active (business-end) arms:
    // up to CHEVRON_TIER_SIZE chevrons are displayed, and each completed tier of that many upgrades
    // advances the chevron color.

    private static final int CHEVRON_TIER_SIZE = 3;            // chevrons shown per color tier
    private static final int CHEVRON_COLOR_START = 0xFF29555E;
    private static final int CHEVRON_COLOR_END   = 0xFF4444FF;
    private static final float CHEVRON_COLOR_BIAS = 1.7f;      // >1 leans mid tiers toward the start color
    private static final int[] NO_CHEVRONS = new int[0];

    /**
     * Draws the chevron tiles for one arm just inside the cap/band zone.
     * When outward is true the chevrons are flipped to point toward the tip (output
     * flow); otherwise they point toward the center (pull flow).
     */
    private static void drawChevrons(PoseStack poseStack, VertexConsumer builder, int combinedLight,
                                     int slot, float xmin, float xmax, float zmin, float zmax, float ymax,
                                     int[] colors, boolean outward, float alpha) {
        TextureAtlasSprite chevron = PipeBlockEntity.getChevronSprite();
        boolean bundle = slot != -1;
        float len   = bundle ? 0.042f : 0.05f;    // length of one chevron tile along the arm
        float start = 0.125f;                      // butted against the cap/band, near the tip
        float raise = 0.004f;                      // sit just above the arm surface

        int n = colors.length;
        for (int i = 0; i < n; i++) {
            float y0 = start + i * (len);
            float y1 = y0 + len;
            if (y1 > ymax - 0.005f) break;         // ran out of arm (shouldn't happen at <= 3 tiles)

            // The current-tier (newest) color fills from the outer end (tip) inward
            int color = colors[i];

            poseStack.pushPose();
            if (outward) {
                // Flip end-for-end along the arm so the chevron points toward the tip.
                float pcX = 1f - (xmin + xmax) / 2f;
                float pcZ = (y0 + y1) / 2f;
                poseStack.translate(pcX, 0.5f, pcZ);
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.translate(-pcX, -0.5f, -pcZ);
            }
            RenderHelper.drawCube(poseStack, builder, chevron,
                    xmin - raise, xmax + raise, y0, y1, zmin - raise, zmax + raise,
                    combinedLight, color, alpha, false);
            poseStack.popPose();
        }
    }

    /**
     * Maps a speed-upgrade count to the chevron colors to display (leading-first). The number of
     * color tiers is derived from the configured maximum, so the full color range always spans the
     * configured start/end colors regardless of the cap.
     */
    private static int[] computeChevronColors(int upgrades) {
        if (upgrades <= 0) return NO_CHEVRONS;
        int max = Math.max(Config.SPEED_UPGRADE_MAX.get(), upgrades);
        int numTiers = (max + CHEVRON_TIER_SIZE - 1) / CHEVRON_TIER_SIZE;
        int tier = (upgrades - 1) / CHEVRON_TIER_SIZE;
        int within = upgrades - tier * CHEVRON_TIER_SIZE;          // 1..CHEVRON_TIER_SIZE
        int curColor = tierColor(tier, numTiers);
        if (tier == 0) {
            int[] out = new int[within];
            for (int i = 0; i < within; i++) out[i] = curColor;
            return out;
        }
        int prevColor = tierColor(tier - 1, numTiers);
        int[] out = new int[CHEVRON_TIER_SIZE];
        for (int i = 0; i < CHEVRON_TIER_SIZE; i++)
            out[i] = (i < within) ? curColor : prevColor;          // leading current tier, then previous
        return out;
    }

    /**
     * Interpolates between CHEVRON_COLOR_START and CHEVRON_COLOR_END (per RGB channel) for the
     * given tier, so the full color range spans the two endpoints regardless of the configured cap.
     */
    private static int tierColor(int tier, int numTiers) {
        float t = (numTiers <= 1) ? 0f : (float) tier / (numTiers - 1);
        t = (float) Math.pow(t, CHEVRON_COLOR_BIAS);   // bias mid tiers toward the start color
        int sr = (CHEVRON_COLOR_START >> 16) & 0xFF, sg = (CHEVRON_COLOR_START >> 8) & 0xFF, sb = CHEVRON_COLOR_START & 0xFF;
        int er = (CHEVRON_COLOR_END   >> 16) & 0xFF, eg = (CHEVRON_COLOR_END   >> 8) & 0xFF, eb = CHEVRON_COLOR_END   & 0xFF;
        int r = Math.round(sr + (er - sr) * t);
        int g = Math.round(sg + (eg - sg) * t);
        int b = Math.round(sb + (eb - sb) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}