package com.dannyandson.tinypipes.blocks.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Caches a PipeBlockEntity's full-pipe geometry as a flat vertex list.
 *
 * Geometry is captured once (or after a state change) into block-local space via a
 * CapturingVertexConsumer, then replayed under the real render matrix each frame,
 * skipping all drawCube/drawSide work. The 26.2 submit pipeline owns the replay:
 * the BER hands replay() the VertexConsumer supplied by
 * SubmitNodeCollector#submitCustomGeometry.
 *
 * The cache is invalidated on pipe add/remove, side toggle, neighbor/camouflage
 * changes, NBT load, or a light change.
 */
public class CachedPipeRenderer {

    private List<CachedVertex> cachedVertices = Collections.emptyList();
    private boolean dirty = true;
    private int lastCombinedLight = -1;

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean lightChanged(int combinedLight) {
        return combinedLight != lastCombinedLight;
    }

    /**
     * Rebuild the cache by capturing a full render pass into block-local space.
     * The capture uses an identity PoseStack, so the existing render transforms
     * produce positions relative to the block entity origin.
     */
    public void rebuild(RenderAction renderAction, int combinedLight) {
        CapturingVertexConsumer consumer = new CapturingVertexConsumer();
        PoseStack capturePoseStack = new PoseStack();

        renderAction.render(capturePoseStack, consumer);
        consumer.flush();

        this.cachedVertices = new ArrayList<>(consumer.getVertices());
        this.dirty = false;
        this.lastCombinedLight = combinedLight;
    }

    /**
     * Replay cached (block-local) vertices into the supplied consumer, applying the
     * current render matrix to place them in world space.
     */
    public void replay(VertexConsumer builder, Matrix4f matrix) {
        if (cachedVertices.isEmpty()) {
            return;
        }
        for (CachedVertex v : cachedVertices) {
            builder.addVertex(matrix, v.x, v.y, v.z)
                    .setColor(v.r, v.g, v.b, v.a)
                    .setUv(v.u, v.v)
                    .setUv1(v.overlayU, v.overlayV)
                    .setUv2(v.lightU, v.lightV)
                    .setNormal(v.normalX, v.normalY, v.normalZ);
        }
    }

    public boolean isEmpty() {
        return cachedVertices.isEmpty();
    }

    public void clear() {
        cachedVertices = Collections.emptyList();
        dirty = true;
        lastCombinedLight = -1;
    }

    /** Render callback used during a cache rebuild. */
    @FunctionalInterface
    public interface RenderAction {
        void render(PoseStack poseStack, VertexConsumer consumer);
    }
}