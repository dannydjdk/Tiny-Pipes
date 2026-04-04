package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.blocks.PipeBlock;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.blocks.rendering.PipeBlockEntityRenderer;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.network.ModNetworkHandler;
import com.dannyandson.tinypipes.network.PushPipeConnection;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

public class PipeConfigGUI extends Screen {

    private static final float SCALE = 40f;
    private static final float DRAG_SENSITIVITY = 0.8f;
    private static final float CLICK_THRESHOLD_SQ = 9f;
    private static final int FULL_BRIGHT = 15728880;

    // Indicator sizing
    private static final float INDICATOR_SIZE = 0.15f;
    private static final float INDICATOR_OFFSET = 1.005f;
    private static final float NEG_INDICATOR_OFFSET = -0.005f;

    // Colors for connection states
    private static final int COLOR_ENABLED  = 0xFF00CC00;
    private static final int COLOR_PULLING  = 0xFF0066FF;
    private static final int COLOR_DISABLED = 0xFF666666;
    private static final int COLOR_HOVERED  = 0xFFFFFF00;

    private final PipeBlockEntity pipeBlockEntity;
    private final int slotPos;
    private final Component pipeName;

    // View rotation (degrees)
    private float rotationX = 25f;
    private float rotationY = -45f;

    // Mouse interaction state
    private boolean isDragging = false;
    private int activeButton = -1;
    private double clickStartX, clickStartY;
    private boolean clickCandidate = false;

    // Currently hovered face (null if none)
    private Direction hoveredFace = null;
    private Button closeButton;

    protected PipeConfigGUI(PipeBlockEntity pipeBlockEntity, AbstractFullPipe pipe) {
        super(Component.translatable("tinypipes:pipeconfiggui"));
        this.pipeBlockEntity = pipeBlockEntity;
        this.slotPos = pipe.slotPos();

        // Get the pipe's display name from its registered item
        Item pipeItem = Registry.getFullPipeItemFromClass(pipe.getClass());
        this.pipeName = (pipeItem != null)
                ? pipeItem.getDefaultInstance().getHoverName()
                : Component.literal("Pipe");

        // Set initial rotation to match the player's current view
        if (Minecraft.getInstance().player != null) {
            float yaw = Minecraft.getInstance().player.getYRot();
            float pitch = Minecraft.getInstance().player.getXRot();
            rotationY = 180f + yaw;
            rotationX = -pitch;
        }
    }

    /**
     * Always fetch the live pipe from the block entity.
     */
    private AbstractFullPipe getPipe() {
        return pipeBlockEntity.getPipe(slotPos);
    }

    @Override
    protected void init() {
        closeButton = ModWidget.buildButton(
                width / 2 - 40, height - 28, 80, 20,
                Component.translatable("tinypipes.close"),
                button -> minecraft.setScreen(null)
        );
        addRenderableWidget(closeButton);
    }

    // ── Rendering ───────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, 0xC0101010);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        extractBackground(guiGraphics, mouseX, mouseY, partialTicks);

        // Dark panel behind the 3D scene for contrast
        int panelSize = (int)(SCALE * 3.5f);
        int panelX = width / 2 - panelSize / 2;
        int panelY = height / 2 - 8 - panelSize / 2;
        guiGraphics.fill(panelX - 1, panelY - 1, panelX + panelSize + 1, panelY + panelSize + 1, 0xFF000000);
        guiGraphics.fill(panelX, panelY, panelX + panelSize, panelY + panelSize, 0xE0222222);

        PoseStack poseStack = new PoseStack();

        // ── 3D scene ──

        MultiBufferSource.BufferSource bufferSource =
                Minecraft.getInstance().renderBuffers().bufferSource();

        poseStack.pushPose();

        float centerX = width / 2f;
        float centerY = height / 2f - 8;
        poseStack.translate(centerX, centerY, 150);
        poseStack.scale(SCALE, -SCALE, SCALE);
        poseStack.mulPose(Axis.XP.rotationDegrees(-rotationX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        poseStack.translate(-0.5, -0.5, -0.5);

        // Hit test: project indicator quads to screen and check mouse
        Matrix4f mvMatrix = new Matrix4f(poseStack.last().pose());
        hoveredFace = hitTestIndicators(mvMatrix, mouseX, mouseY);

        // Render the pipe (identical to in-world)
        PipeBlockEntityRenderer.renderGeometry(pipeBlockEntity, poseStack, bufferSource, FULL_BRIGHT, 0);
        bufferSource.endBatch();

        // 3D text labels on indicator faces
        renderIndicatorLabels(poseStack, bufferSource);
        bufferSource.endBatch();

        // Translucent layer
        renderAdjacentBlocks(poseStack, bufferSource);
        renderFaceIndicators(poseStack, bufferSource);
        bufferSource.endBatch();

        poseStack.popPose();

        guiGraphics.nextStratum();
        // 2D overlay
        guiGraphics.centeredText(font,
                Component.translatable("tinypipes.gui.full_pipe_config", pipeName),
                width / 2, 10, 0xFFFFFF);
        guiGraphics.centeredText(font,
                Component.translatable("tinypipes.gui.pipe_config.hint"),
                width / 2, 22, 0x888888);
        renderLegend(guiGraphics);
    }

    // ── Adjacent blocks ─────────────────────────────────────────────────────────

    private void renderAdjacentBlocks(PoseStack poseStack, MultiBufferSource bufferSource) {
        Level level = pipeBlockEntity.getLevel();
        if (level == null) return;

        BlockPos pos = pipeBlockEntity.getBlockPos();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState adjacent = level.getBlockState(neighborPos);
            if (adjacent.isAir()) continue;

            poseStack.pushPose();
            poseStack.translate(dir.getStepX(), dir.getStepY(), dir.getStepZ());

            if (adjacent.getBlock() instanceof PipeBlock
                    && level.getBlockEntity(neighborPos) instanceof PipeBlockEntity adjacentPBE) {
                PipeBlockEntityRenderer.renderGeometry(adjacentPBE, poseStack, bufferSource, FULL_BRIGHT, 0, 0.3f);
            } else if (adjacent.getBlock() instanceof ChestBlock) {
                VertexConsumer builder = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
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
                VertexConsumer builder = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
                TextureAtlasSprite sprite = getBlockSprite(adjacent, dir.getOpposite());
                RenderHelper.drawCube(poseStack, builder, sprite,
                        0.02f, 0.98f, 0.02f, 0.98f, 0.02f, 0.98f,
                        FULL_BRIGHT, 0xFFFFFFFF, 0.2f);
            }

            poseStack.popPose();
        }
    }

    private static TextureAtlasSprite getBlockSprite(BlockState state, Direction face) {
        // TODO: Restore per-face sprite lookup once BlockStateModel particle API is identified
        return RenderHelper.getSprite(state, face);
    }

    // ── Face indicators ─────────────────────────────────────────────────────────

    private void renderFaceIndicators(PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer builder = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        Matrix4f matrix = poseStack.last().pose();
        TextureAtlasSprite sprite = PipeBlockEntity.getWhitePipeSprite();
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        for (Direction dir : Direction.values()) {
            PipeConnectionState state = getPipe().getPipeSideStatus(dir);
            boolean hovered = (dir == hoveredFace);

            if (hovered) {
                float inner = INDICATOR_SIZE;
                float outer = INDICATOR_SIZE + 0.05f;
                drawOutlineFrame(builder, matrix, dir, inner, outer, u0, u1, v0, v1, COLOR_HOVERED, 0.9f);
            }

            int color = getStateColor(state);
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

    private static float[][] getIndicatorCorners(Direction dir, float size) {
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

    // ── Hit testing ─────────────────────────────────────────────────────────────

    private Direction hitTestIndicators(Matrix4f mvMatrix, double mouseX, double mouseY) {
        float mx = (float) mouseX;
        float my = (float) mouseY;
        float hitSize = INDICATOR_SIZE + 0.05f;

        Direction closest = null;
        float closestZ = Float.NEGATIVE_INFINITY;

        for (Direction dir : Direction.values()) {
            float[][] corners = getIndicatorCorners(dir, hitSize);

            float[] sx = new float[4];
            float[] sy = new float[4];
            float avgZ = 0;
            for (int i = 0; i < 4; i++) {
                Vector4f v = new Vector4f(corners[i][0], corners[i][1], corners[i][2], 1.0f);
                mvMatrix.transform(v);
                sx[i] = v.x / v.w;
                sy[i] = v.y / v.w;
                avgZ += v.z / v.w;
            }
            avgZ /= 4f;

            if (pointInQuad(mx, my, sx, sy) && avgZ > closestZ) {
                closestZ = avgZ;
                closest = dir;
            }
        }
        return closest;
    }

    private static boolean pointInQuad(float px, float py, float[] qx, float[] qy) {
        boolean allPositive = true;
        boolean allNegative = true;

        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            float cross = (qx[j] - qx[i]) * (py - qy[i]) - (qy[j] - qy[i]) * (px - qx[i]);
            if (cross > 0) allNegative = false;
            if (cross < 0) allPositive = false;
        }

        return allPositive || allNegative;
    }

    // ── Legend ───────────────────────────────────────────────────────────────────

    private void renderLegend(GuiGraphicsExtractor guiGraphics) {
        int legendY = height - 68;
        int legendX = width / 2 - 105;
        int boxSize = 8;
        int textGap = boxSize + 4;

        guiGraphics.fill(legendX, legendY, legendX + boxSize, legendY + boxSize, COLOR_ENABLED);
        guiGraphics.text(font, Component.translatable("tinypipes.gui.pipe_config.msg.enabled"), legendX + textGap, legendY, 0xFFFFFF);

        legendY += 12;
        guiGraphics.fill(legendX, legendY, legendX + boxSize, legendY + boxSize, COLOR_DISABLED);
        guiGraphics.text(font, Component.translatable("tinypipes.gui.pipe_config.msg.disabled"), legendX + textGap, legendY, 0xFFFFFF);

        legendY += 12;
        guiGraphics.fill(legendX, legendY, legendX + boxSize, legendY + boxSize, COLOR_PULLING);
        guiGraphics.text(font, Component.translatable("tinypipes.gui.pipe_config.msg.pulling"), legendX + textGap, legendY, 0xFFFFFF);
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

    private void renderIndicatorLabels(PoseStack poseStack, MultiBufferSource bufferSource) {
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
                        poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL,
                        0, FULL_BRIGHT);

                poseStack.popPose();
            }
        }
    }

    // ── Mouse interaction ───────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int button = event.button();
        if (button == 0 || button == 1) {
            isDragging = true;
            activeButton = button;
            clickCandidate = true;
            clickStartX = event.x();
            clickStartY = event.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        int button = event.button();
        double mouseX = event.x();
        double mouseY = event.y();
        if (button == activeButton && (button == 0 || button == 1)) {
            if (clickCandidate && hoveredFace != null) {
                double dx = mouseX - clickStartX;
                double dy = mouseY - clickStartY;
                if (dx * dx + dy * dy < CLICK_THRESHOLD_SQ) {
                    toggleConnection(hoveredFace, button == 1);
                }
            }
            isDragging = false;
            activeButton = -1;
            clickCandidate = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        int button = event.button();
        double mouseX = event.x();
        double mouseY = event.y();
        if (isDragging && button == activeButton) {
            double dx = mouseX - clickStartX;
            double dy = mouseY - clickStartY;
            if (dx * dx + dy * dy >= CLICK_THRESHOLD_SQ) {
                clickCandidate = false;
            }
            if (button == 0) {
                rotationY += (float) dragX * DRAG_SENSITIVITY;
                rotationX -= (float) dragY * DRAG_SENSITIVITY;
                rotationX = Math.max(-90f, Math.min(90f, rotationX));
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    // ── State toggle ────────────────────────────────────────────────────────────

    private void toggleConnection(Direction side, boolean reverse) {
        AbstractFullPipe currentPipe = getPipe();
        if (currentPipe == null) return;
        PipeConnectionState state = reverse
                ? currentPipe.getPrevToggleState(side)
                : currentPipe.togglePipeSide(pipeBlockEntity, side);
        if (reverse) {
            currentPipe.setConnectionState(pipeBlockEntity, side, state);
        }
        ModNetworkHandler.sendToServer(new PushPipeConnection(
                pipeBlockEntity.getBlockPos(), slotPos, side, state));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void open(PipeBlockEntity pipeBlockEntity, AbstractFullPipe tinyPipe) {
        if (pipeBlockEntity != null && tinyPipe != null)
            Minecraft.getInstance().setScreen(new PipeConfigGUI(pipeBlockEntity, tinyPipe));
    }
}
