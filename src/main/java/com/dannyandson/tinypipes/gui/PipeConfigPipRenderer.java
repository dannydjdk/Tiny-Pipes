package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.blocks.rendering.PipeBlockEntityRenderer;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import com.dannyandson.tinypipes.blocks.PipeBlock;

/**
 * PiP renderer for the 3D pipe configuration scene.
 * <p>
 * Renders the pipe geometry, face indicators, direction labels,
 * and adjacent block previews into an offscreen texture that the
 * GUI system composites onto the screen.
 */
public class PipeConfigPipRenderer extends PictureInPictureRenderer<PipeConfigPipRenderState> {

    // Mirror the constants from PipeConfigGUI
    private static final float SCALE = 40f;
    private static final int FULL_BRIGHT = 15728880;
    private static final float INDICATOR_SIZE = 0.15f;
    private static final float INDICATOR_OFFSET = 1.005f;
    private static final float NEG_INDICATOR_OFFSET = -0.005f;
    // Inner-facing quads sit just inside the block space so they aren't
    // occluded by adjacent block geometry at the 1.0 boundary.
    private static final float INNER_INDICATOR_OFFSET = 0.995f;
    private static final float INNER_NEG_INDICATOR_OFFSET = 0.005f;

    private static final int COLOR_ENABLED  = 0xFF00CC00;
    private static final int COLOR_PULLING  = 0xFF0066FF;
    private static final int COLOR_DISABLED = 0xFF666666;
    private static final int COLOR_HOVERED  = 0xFFFFFF00;

    private static ModelBlockRenderer cachedModelRenderer;

    public PipeConfigPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<PipeConfigPipRenderState> getRenderStateClass() {
        return PipeConfigPipRenderState.class;
    }

    @Override
    protected String getTextureLabel() {
        return "tinypipes:pipe_config";
    }

    @Override
    protected void renderToTexture(PipeConfigPipRenderState state, PoseStack poseStack) {
        PipeBlockEntity pbe = state.pipeBlockEntity();

        poseStack.pushPose();

        // PiP positions origin at bottom-center. Shift up to true center.
        float viewHeight = state.y1() - state.y0();
        poseStack.translate(0, -viewHeight / 2.0, 0);

        // Uniform scale (no axis flips — those break face culling).
        poseStack.scale(SCALE, SCALE, SCALE);
        // 180° X rotation flips Y and Z without changing winding order,
        // converting from screen-down Y to model-up Y.
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.rotationX()));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotationY()));
        poseStack.translate(-0.5, -0.5, -0.5);

        // ── Pipe geometry ──
        // The PiP context doesn't bind the lightmap texture, so any render type
        // with .useLightmap() renders dark. We work around this with two passes:
        // Pass 1: cutoutBlockSheet — dark but writes depth correctly for occlusion.
        // Pass 2: eyes() — no lightmap, renders bright at the same Z positions.
        //         It passes the LEQUAL depth test but doesn't write depth itself.
        RenderHelper.disableDirectionalShading = true;
        PipeBlockEntityRenderer.renderGeometry(pbe, poseStack, this.bufferSource, FULL_BRIGHT, 0);
        this.bufferSource.endBatch();

        PipeBlockEntityRenderer.overrideRenderType =
                RenderTypes.eyes(TextureAtlas.LOCATION_BLOCKS);
        PipeBlockEntityRenderer.renderGeometry(pbe, poseStack, this.bufferSource, FULL_BRIGHT, 0);
        PipeBlockEntityRenderer.overrideRenderType = null;
        this.bufferSource.endBatch();

        // ── Translucent adjacent blocks ──
        // Use entityTranslucentEmissive: no depth WRITE so indicators/labels render
        // on top regardless of viewing angle. No lightmap either, but at 0.3 alpha
        // the PiP lightmap darkness is invisible. entityTranslucent wrote depth,
        // which occluded U/D indicators and hid indicators seen through neighbors.
        PipeBlockEntityRenderer.overrideRenderType =
                RenderTypes.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS);
        renderAdjacentBlocks(state, poseStack);
        PipeBlockEntityRenderer.overrideRenderType = null;
        RenderHelper.disableDirectionalShading = false;
        // Flush adjacent blocks BEFORE indicators — translucent types flush after
        // opaque within the same endBatch(), which would overdraw indicators.
        this.bufferSource.endBatch();

        // ── Face indicators (must render AFTER adjacent blocks to be visible) ──
        renderFaceIndicators(state, poseStack);
        this.bufferSource.endBatch();

        // ── 3D direction labels AFTER indicators ──
        // Labels must render last so they appear on top of the double-sided
        // indicator quads (which would otherwise occlude inward U/D labels).
        renderIndicatorLabels(state, poseStack);
        this.bufferSource.endBatch();

        poseStack.popPose();
    }

    // ── Adjacent block previews ──────────────────────────────────────────────────

    private void renderAdjacentBlocks(PipeConfigPipRenderState state, PoseStack poseStack) {
        PipeBlockEntity pbe = state.pipeBlockEntity();
        Level level = pbe.getLevel();
        if (level == null) return;

        BlockPos pos = pbe.getBlockPos();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState adjacent = level.getBlockState(neighborPos);
            if (adjacent.isAir()) continue;

            poseStack.pushPose();
            poseStack.translate(dir.getStepX(), dir.getStepY(), dir.getStepZ());

            if (adjacent.getBlock() instanceof PipeBlock
                    && level.getBlockEntity(neighborPos) instanceof PipeBlockEntity adjacentPBE) {
                PipeBlockEntityRenderer.renderGeometry(adjacentPBE, poseStack, this.bufferSource, FULL_BRIGHT, 0, 0.3f);
            } else if (adjacent.getBlock() instanceof ChestBlock) {
                VertexConsumer builder = this.bufferSource.getBuffer(
                        RenderTypes.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS));
                TextureAtlasSprite whiteSprite = PipeBlockEntity.getWhitePipeSprite();
                int chestBrown = 0xFF8B6914;
                RenderHelper.drawCube(poseStack, builder, whiteSprite,
                        0.0625f, 0.9375f, 0.0625f, 0.9375f, 0.0625f, 0.6875f,
                        FULL_BRIGHT, chestBrown, 0.3f);
                RenderHelper.drawCube(poseStack, builder, whiteSprite,
                        0.0625f, 0.9375f, 0.0625f, 0.9375f, 0.6875f, 0.875f,
                        FULL_BRIGHT, chestBrown, 0.25f);
                RenderHelper.drawCube(poseStack, builder, whiteSprite,
                        0.4375f, 0.5625f, 0.0f, 0.0625f, 0.5625f, 0.8125f,
                        FULL_BRIGHT, 0xFF3B2A0A, 0.4f);
            } else {
                // Try rendering the block model. If empty (BER-only blocks like
                // Tiny Redstone panels), fall back to a cube with the particle sprite.
                if (!renderBlockModel(adjacent, neighborPos, level, poseStack, 0.3f)) {
                    VertexConsumer builder = this.bufferSource.getBuffer(
                            RenderTypes.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS));
                    // Try to get the particle sprite from the block model — most blocks
                    // define one even if the model has no visible quads (BER-only blocks).
                    var model = Minecraft.getInstance().getModelManager()
                            .getBlockStateModelSet().get(adjacent);
                    TextureAtlasSprite sprite = (model != null) ? model.particleMaterial().sprite()
                            : PipeBlockEntity.getWhitePipeSprite();
                    RenderHelper.drawCube(poseStack, builder, sprite,
                            0.02f, 0.98f, 0.02f, 0.98f, 0.02f, 0.98f,
                            FULL_BRIGHT, 0xFFFFFFFF, 0.3f);
                }
            }

            poseStack.popPose();
        }
    }

    // ── Block model rendering (tesselateBlock with PoseStack + alpha) ────────────

    /**
     * Renders a block using tesselateBlock, applying the PoseStack transform
     * and alpha to each vertex. Same pattern as camouflage rendering in
     * PipeBlockEntityRenderer, but with PoseStack support for PiP positioning.
     * Returns true if any quads were emitted, false if the model was empty.
     */
    private boolean renderBlockModel(BlockState blockState, BlockPos blockPos, Level level,
                                     PoseStack poseStack, float alpha) {
        var modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        var model = modelSet.get(blockState);
        if (model == null) return false;

        VertexConsumer builder = this.bufferSource.getBuffer(
                RenderTypes.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS));
        Matrix4f matrix = poseStack.last().pose();
        int alphaInt = (int)(alpha * 255f);
        boolean[] emitted = {false};

        getOrCreateModelRenderer().tesselateBlock(
                (var x, var y, var z, var quad, var instance) -> {
                    emitted[0] = true;
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
                        builder.addVertex(matrix, pos.x() + x, pos.y() + y, pos.z() + z)
                                .setColor(vertexColor)
                                .setUv(u, v)
                                .setUv1(0, 10)
                                .setUv2(light & 0xFFFF, (light >> 16) & 0xFFFF)
                                .setNormal(0f, 1f, 0f);
                    }
                },
                0f, 0f, 0f,
                (BlockAndTintGetter) level, blockPos,
                blockState, model,
                blockState.getSeed(blockPos)
        );
        return emitted[0];
    }

    private static ModelBlockRenderer getOrCreateModelRenderer() {
        if (cachedModelRenderer == null) {
            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            cachedModelRenderer = new ModelBlockRenderer(true, false, blockColors);
        }
        return cachedModelRenderer;
    }

    // ── Face indicators (colored quads showing connection state) ─────────────────

    private void renderFaceIndicators(PipeConfigPipRenderState state, PoseStack poseStack) {
        AbstractFullPipe pipe = state.pipeBlockEntity().getPipe(state.slotPos());
        if (pipe == null) return;

        VertexConsumer builder = this.bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        Matrix4f matrix = poseStack.last().pose();
        TextureAtlasSprite sprite = PipeBlockEntity.getWhitePipeSprite();
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        for (Direction dir : Direction.values()) {
            PipeConnectionState connState = pipe.getPipeSideStatus(dir);
            boolean hovered = (dir == state.hoveredFace());

            if (hovered) {
                float inner = INDICATOR_SIZE;
                float outer = INDICATOR_SIZE + 0.05f;
                drawOutlineFrame(builder, matrix, dir, inner, outer, u0, u1, v0, v1, COLOR_HOVERED, 0.9f);
            }

            int color = getStateColor(connState);
            float alpha = hovered ? 0.9f : 0.7f;
            float[][] corners = getIndicatorCorners(dir, INDICATOR_SIZE);
            // Front face (outer, visible from outside)
            addQuad(builder, matrix,
                    corners[0][0], corners[0][1], corners[0][2],
                    corners[1][0], corners[1][1], corners[1][2],
                    corners[2][0], corners[2][1], corners[2][2],
                    corners[3][0], corners[3][1], corners[3][2],
                    u0, u1, v0, v1, color, alpha);
            // Back face (inner, just inside block space — not occluded by adjacent blocks)
            float[][] innerCorners = getInnerIndicatorCorners(dir, INDICATOR_SIZE);
            addQuad(builder, matrix,
                    innerCorners[3][0], innerCorners[3][1], innerCorners[3][2],
                    innerCorners[2][0], innerCorners[2][1], innerCorners[2][2],
                    innerCorners[1][0], innerCorners[1][1], innerCorners[1][2],
                    innerCorners[0][0], innerCorners[0][1], innerCorners[0][2],
                    u0, u1, v0, v1, color, alpha);
        }
    }

    // ── 3D text labels (N/S/E/W/U/D) on indicator faces ─────────────────────────

    private void renderIndicatorLabels(PipeConfigPipRenderState state, PoseStack poseStack) {
        Font font = Minecraft.getInstance().font;
        float ts = 0.018f;
        float offset = 0.01f;

        for (Direction dir : Direction.values()) {
            String label = getDirectionLabel(dir);
            int textWidth = font.width(label);

            for (int side = 0; side < 2; side++) {
                boolean inward = (side == 1);
                float d = inward ? -offset : offset;
                // The 180° X rotation inverts Z, swapping the inward/outward
                // offset direction for N/S faces. U/D (Y-axis) and E/W (X-axis)
                // keep the original offset direction.
                if (dir.getAxis() == Direction.Axis.Z) d = -d;

                // Inward labels use inner offsets (just inside block space)
                // to avoid being occluded by adjacent block geometry.
                float posOff = inward ? INNER_INDICATOR_OFFSET : INDICATOR_OFFSET;
                float negOff = inward ? INNER_NEG_INDICATOR_OFFSET : NEG_INDICATOR_OFFSET;

                poseStack.pushPose();

                switch (dir) {
                    case NORTH:
                        poseStack.translate(0.5, 0.5, negOff - d);
                        break;
                    case SOUTH:
                        poseStack.translate(0.5, 0.5, posOff + d);
                        poseStack.mulPose(Axis.YP.rotationDegrees(180));
                        break;
                    case EAST:
                        poseStack.translate(posOff + d, 0.5, 0.5);
                        poseStack.mulPose(Axis.YP.rotationDegrees(90));
                        break;
                    case WEST:
                        poseStack.translate(negOff - d, 0.5, 0.5);
                        poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                        break;
                    case UP:
                        poseStack.translate(0.5, posOff + d, 0.5);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                        break;
                    case DOWN:
                        poseStack.translate(0.5, negOff - d, 0.5);
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                        break;
                }

                // The YP(180°) flip reverses the
                // text quad's winding order (by flipping X positions), making it
                // visible from the opposite side. This works for ALL faces:
                // - N/S/E/W: flips X and Z, reversing winding and normal
                // - U/D: flips X and Z (which is model Y after XP rotation),
                //   reversing winding for the inward viewer
                // The d offset is NOT inverted for U/D (only Z-axis faces),
                // so labels stay at their correct inward/outward positions.
                boolean needsFlip = inward;
                if (needsFlip) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                }
                poseStack.scale(ts, -ts, ts);
                poseStack.translate(-textWidth / 2.0, -font.lineHeight / 2.0, 0);

                font.drawInBatch(label, 0, 0, 0xFFFFFFFF, true,
                        poseStack.last().pose(), this.bufferSource, Font.DisplayMode.NORMAL,
                        0, FULL_BRIGHT);

                poseStack.popPose();
            }
        }
    }

    // ── Static helpers (duplicated from PipeConfigGUI for self-containment) ──────

    static float[][] getIndicatorCorners(Direction dir, float size) {
        return getIndicatorCorners(dir, size, INDICATOR_OFFSET, NEG_INDICATOR_OFFSET);
    }

    static float[][] getInnerIndicatorCorners(Direction dir, float size) {
        return getIndicatorCorners(dir, size, INNER_INDICATOR_OFFSET, INNER_NEG_INDICATOR_OFFSET);
    }

    private static float[][] getIndicatorCorners(Direction dir, float size, float pos, float neg) {
        float min = 0.5f - size;
        float max = 0.5f + size;

        return switch (dir) {
            case UP ->    new float[][] {{min,pos,min}, {max,pos,min}, {max,pos,max}, {min,pos,max}};
            case DOWN ->  new float[][] {{min,neg,max}, {max,neg,max}, {max,neg,min}, {min,neg,min}};
            case SOUTH -> new float[][] {{min,min,pos}, {max,min,pos}, {max,max,pos}, {min,max,pos}};
            case NORTH -> new float[][] {{max,min,neg}, {min,min,neg}, {min,max,neg}, {max,max,neg}};
            case EAST ->  new float[][] {{pos,min,max}, {pos,min,min}, {pos,max,min}, {pos,max,max}};
            case WEST ->  new float[][] {{neg,min,min}, {neg,min,max}, {neg,max,max}, {neg,max,min}};
        };
    }

    private static void drawOutlineFrame(VertexConsumer builder, Matrix4f matrix, Direction dir,
                                         float innerSize, float outerSize,
                                         float u0, float u1, float v0, float v1,
                                         int color, float alpha) {
        float[][] outer = getIndicatorCorners(dir, outerSize);
        float[][] inner = getIndicatorCorners(dir, innerSize);
        float[][] outerInner = getInnerIndicatorCorners(dir, outerSize);
        float[][] innerInner = getInnerIndicatorCorners(dir, innerSize);

        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            // Front face (outer offset)
            addQuad(builder, matrix,
                    outer[i][0], outer[i][1], outer[i][2],
                    outer[j][0], outer[j][1], outer[j][2],
                    inner[j][0], inner[j][1], inner[j][2],
                    inner[i][0], inner[i][1], inner[i][2],
                    u0, u1, v0, v1, color, alpha);
            // Back face (inner offset, reversed winding)
            addQuad(builder, matrix,
                    innerInner[i][0], innerInner[i][1], innerInner[i][2],
                    innerInner[j][0], innerInner[j][1], innerInner[j][2],
                    outerInner[j][0], outerInner[j][1], outerInner[j][2],
                    outerInner[i][0], outerInner[i][1], outerInner[i][2],
                    u0, u1, v0, v1, color, alpha);
        }
    }

    private static void addQuad(VertexConsumer builder, Matrix4f matrix,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float u0, float u1, float v0, float v1,
                                int color, float alpha) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF, a = (int)(alpha * 255f);
        int lightU = FULL_BRIGHT & 0xFFFF, lightV = (FULL_BRIGHT >> 16) & 0xFFFF;
        builder.addVertex(matrix, x0,y0,z0).setColor(r,g,b,a).setUv(u0,v0).setUv1(0,10).setUv2(lightU,lightV).setNormal(0,1,0);
        builder.addVertex(matrix, x1,y1,z1).setColor(r,g,b,a).setUv(u1,v0).setUv1(0,10).setUv2(lightU,lightV).setNormal(0,1,0);
        builder.addVertex(matrix, x2,y2,z2).setColor(r,g,b,a).setUv(u1,v1).setUv1(0,10).setUv2(lightU,lightV).setNormal(0,1,0);
        builder.addVertex(matrix, x3,y3,z3).setColor(r,g,b,a).setUv(u0,v1).setUv1(0,10).setUv2(lightU,lightV).setNormal(0,1,0);
    }

    private static int getStateColor(PipeConnectionState state) {
        return switch (state) {
            case ENABLED  -> COLOR_ENABLED;
            case PULLING  -> COLOR_PULLING;
            case DISABLED -> COLOR_DISABLED;
        };
    }

    private static String getDirectionLabel(Direction dir) {
        return switch (dir) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST  -> "E";
            case WEST  -> "W";
            case UP    -> "U";
            case DOWN  -> "D";
        };
    }
}