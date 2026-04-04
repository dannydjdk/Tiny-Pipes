package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.network.ModNetworkHandler;
import com.dannyandson.tinypipes.network.PushPipeConnection;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class PipeConfigGUI extends Screen {

    private static final float SCALE = 40f;
    private static final float DRAG_SENSITIVITY = 0.8f;
    private static final float CLICK_THRESHOLD_SQ = 9f;

    // Indicator sizing (needed for hit testing)
    private static final float INDICATOR_SIZE = 0.15f;
    private static final float INDICATOR_OFFSET = 1.005f;
    private static final float NEG_INDICATOR_OFFSET = -0.005f;

    // Colors for connection states (needed for legend)
    private static final int COLOR_ENABLED  = 0xFF00CC00;
    private static final int COLOR_PULLING  = 0xFF0066FF;
    private static final int COLOR_DISABLED = 0xFF666666;

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
    private static boolean pipDiagRun = false; // DIAGNOSTIC — remove later

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
        // DIAGNOSTIC — disabled to rule out layering over PiP
        int panelSize = (int)(SCALE * 3.5f);
        int panelX = width / 2 - panelSize / 2;
        int panelY = height / 2 - 8 - panelSize / 2;
        guiGraphics.fill(panelX - 1, panelY - 1, panelX + panelSize + 1, panelY + panelSize + 1, 0xFF000000);
        guiGraphics.fill(panelX, panelY, panelX + panelSize, panelY + panelSize, 0xE0222222);

        // ── Hit testing (same transform math as the PiP renderer, no draw calls) ──
        PoseStack hitPose = new PoseStack();
        float centerX = width / 2f;
        float centerY = height / 2f - 8;
        hitPose.translate(centerX, centerY, 150);
        hitPose.scale(SCALE, -SCALE, SCALE);
        hitPose.mulPose(Axis.XP.rotationDegrees(-rotationX));
        hitPose.mulPose(Axis.YP.rotationDegrees(rotationY));
        hitPose.translate(-0.5, -0.5, -0.5);
        Matrix4f mvMatrix = new Matrix4f(hitPose.last().pose());
        hoveredFace = hitTestIndicators(mvMatrix, mouseX, mouseY);

        // ── Submit the 3D scene via Picture-in-Picture ──
        // DIAGNOSTIC — reflective pool check, runs once
        if (!pipDiagRun) {
            pipDiagRun = true;
            try {
                var gr = Minecraft.getInstance().gameRenderer;
                // Find guiRenderer field on GameRenderer
                java.lang.reflect.Field guiRendererField = null;
                for (var f : gr.getClass().getDeclaredFields()) {
                    if (f.getType().getName().contains("GuiRenderer")) {
                        guiRendererField = f;
                        break;
                    }
                }
                if (guiRendererField != null) {
                    guiRendererField.setAccessible(true);
                    Object guiRenderer = guiRendererField.get(gr);
                    // Find pictureInPictureRendererPools field
                    java.lang.reflect.Field poolsField = null;
                    for (var f : guiRenderer.getClass().getDeclaredFields()) {
                        if (f.getName().contains("pictureInPicture") || f.getName().contains("pip")) {
                            poolsField = f;
                            System.out.println("[TinyPipes PiP DIAG] Found field: " + f.getName() + " type: " + f.getType().getName());
                        }
                    }
                    if (poolsField != null) {
                        poolsField.setAccessible(true);
                        java.util.Map<?, ?> pools = (java.util.Map<?, ?>) poolsField.get(guiRenderer);
                        System.out.println("[TinyPipes PiP DIAG] Pool map size: " + pools.size());
                        System.out.println("[TinyPipes PiP DIAG] Pool map keys: " + pools.keySet());
                        System.out.println("[TinyPipes PiP DIAG] Our state class: " + PipeConfigPipRenderState.class);
                        System.out.println("[TinyPipes PiP DIAG] Pool contains our class: " + pools.containsKey(PipeConfigPipRenderState.class));
                    } else {
                        System.out.println("[TinyPipes PiP DIAG] Could not find pools field. Fields:");
                        for (var f : guiRenderer.getClass().getDeclaredFields()) {
                            System.out.println("  " + f.getName() + " : " + f.getType().getSimpleName());
                        }
                    }
                } else {
                    System.out.println("[TinyPipes PiP DIAG] Could not find guiRenderer field. Fields:");
                    for (var f : gr.getClass().getDeclaredFields()) {
                        System.out.println("  " + f.getName() + " : " + f.getType().getSimpleName());
                    }
                }
            } catch (Exception e) {
                System.out.println("[TinyPipes PiP DIAG] Reflection failed: " + e);
            }
        }
        // END DIAGNOSTIC
        guiGraphics.submitPictureInPictureRenderState(
                new PipeConfigPipRenderState(
                        pipeBlockEntity, slotPos,
                        rotationX, rotationY, hoveredFace,
                        panelX, panelY, panelX + panelSize, panelY + panelSize,
                        1.0f, null));

        // ── 2D overlay (text on top of the 3D scene) ──
        guiGraphics.nextStratum();
        guiGraphics.centeredText(font,
                Component.translatable("tinypipes.gui.full_pipe_config", pipeName),
                width / 2, 10, 0xFFFFFF);
        guiGraphics.centeredText(font,
                Component.translatable("tinypipes.gui.pipe_config.hint"),
                width / 2, 22, 0x888888);
        renderLegend(guiGraphics);
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