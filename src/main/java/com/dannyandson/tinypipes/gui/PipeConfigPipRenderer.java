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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

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

    private static final int COLOR_ENABLED  = 0xFF00CC00;
    private static final int COLOR_PULLING  = 0xFF0066FF;
    private static final int COLOR_DISABLED = 0xFF666666;
    private static final int COLOR_HOVERED  = 0xFFFFFF00;

    public PipeConfigPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
        System.out.println("[TinyPipes PiP] PipeConfigPipRenderer CONSTRUCTED");
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

        // ── Pipe geometry (identical to in-world BER) ──
        PipeBlockEntityRenderer.renderGeometry(pbe, poseStack, this.bufferSource, FULL_BRIGHT, 0);
        this.bufferSource.endBatch();

        // ── 3D direction labels on indicator faces ──
        renderIndicatorLabels(state, poseStack);
        this.bufferSource.endBatch();

        // ── Translucent layers: adjacent blocks + face indicators ──
        renderAdjacentBlocks(state, poseStack);
        renderFaceIndicators(state, poseStack);
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
                VertexConsumer builder = this.bufferSource.getBuffer(Sheets.cutoutBlockSheet());
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
                VertexConsumer builder = this.bufferSource.getBuffer(Sheets.cutoutBlockSheet());
                TextureAtlasSprite sprite = RenderHelper.getSprite(adjacent, dir.getOpposite());
                RenderHelper.drawCube(poseStack, builder, sprite,
                        0.02f, 0.98f, 0.02f, 0.98f, 0.02f, 0.98f,
                        FULL_BRIGHT, 0xFFFFFFFF, 0.2f);
            }

            poseStack.popPose();
        }
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
            addQuad(builder, matrix,
                    corners[0][0], corners[0][1], corners[0][2],
                    corners[1][0], corners[1][1], corners[1][2],
                    corners[2][0], corners[2][1], corners[2][2],
                    corners[3][0], corners[3][1], corners[3][2],
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

                poseStack.pushPose();

                switch (dir) {
                    case NORTH:
                        poseStack.translate(0.5, 0.5, NEG_INDICATOR_OFFSET - d);
                        break;
                    case SOUTH:
                        poseStack.translate(0.5, 0.5, INDICATOR_OFFSET + d);
                        poseStack.mulPose(Axis.YP.rotationDegrees(180));
                        break;
                    case EAST:
                        poseStack.translate(INDICATOR_OFFSET + d, 0.5, 0.5);
                        poseStack.mulPose(Axis.YP.rotationDegrees(90));
                        break;
                    case WEST:
                        poseStack.translate(NEG_INDICATOR_OFFSET - d, 0.5, 0.5);
                        poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                        break;
                    case UP:
                        poseStack.translate(0.5, INDICATOR_OFFSET + d, 0.5);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                        break;
                    case DOWN:
                        poseStack.translate(0.5, NEG_INDICATOR_OFFSET - d, 0.5);
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                        break;
                }

                boolean needsFlip = (!inward && (dir == Direction.NORTH || dir == Direction.SOUTH))
                        || (inward && (dir == Direction.EAST || dir == Direction.WEST));
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
        float min = 0.5f - size;
        float max = 0.5f + size;
        float pos = INDICATOR_OFFSET;
        float neg = NEG_INDICATOR_OFFSET;

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

        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            addQuad(builder, matrix,
                    outer[i][0], outer[i][1], outer[i][2],
                    outer[j][0], outer[j][1], outer[j][2],
                    inner[j][0], inner[j][1], inner[j][2],
                    inner[i][0], inner[i][1], inner[i][2],
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