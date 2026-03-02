package com.dannyandson.tinypipes.blocks.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores cached vertex data for a PipeBlockEntity's full-pipe rendering.
 *
 * On the first frame (or after a state change), the renderer captures all vertex data
 * by running the normal render logic against a proxy MultiBufferSource. On subsequent
 * frames, the cached vertices are replayed directly into the real buffer, skipping all
 * geometry generation, PoseStack manipulation, and drawCube/drawRectangle calculations.
 *
 * The cache is invalidated (marked dirty) when:
 * - A pipe is added or removed
 * - A pipe side is toggled (connection state change)
 * - Neighbor changes affect visual state
 * - NBT is loaded (world load or server->client sync)
 * - Camouflage is set or removed
 * - Combined light changes
 *
 * Architecture:
 *   render() call
 *     ├─ if dirty or light changed:
 *     │    rebuild() → CaptureBufferSource → CapturingVertexConsumer
 *     │    stores List<CachedVertex> (positions pre-transformed to block-local space)
 *     │
 *     └─ replay() → iterate cached vertices → addVertex() into real VertexConsumer
 */
public class CachedPipeRenderer {

    private List<CachedVertex> cachedVertices = Collections.emptyList();
    private boolean dirty = true;
    private int lastCombinedLight = -1;

    /**
     * Mark the cache as needing a rebuild on the next frame.
     */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * @return true if the cache needs to be rebuilt
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Check if the light level has changed since last render.
     */
    public boolean lightChanged(int combinedLight) {
        return combinedLight != lastCombinedLight;
    }

    /**
     * Rebuild the cache by capturing all vertex data from a full render pass.
     *
     * @param renderAction a callback that performs the actual rendering logic
     *                     (the same code that would normally run in the BER's render method)
     * @param combinedLight the current combined light value
     */
    public void rebuild(RenderAction renderAction, int combinedLight) {
        CaptureBufferSource captureSource = new CaptureBufferSource();

        // Create an identity PoseStack for capture.
        // The existing render code applies all transforms (translate, rotate) relative to
        // the block entity origin, so using identity captures positions in block-local space.
        PoseStack capturePoseStack = new PoseStack();

        // Run the full render logic against our capturing buffer
        renderAction.render(capturePoseStack, captureSource);

        // Flush the last vertex (1.21.1 contract: no endVertex, last vertex needs explicit flush)
        captureSource.getConsumer().flush();

        // Store the captured vertices
        this.cachedVertices = new ArrayList<>(captureSource.getConsumer().getVertices());
        this.dirty = false;
        this.lastCombinedLight = combinedLight;
    }

    /**
     * Replay cached vertices into the real buffer.
     * Since positions were captured in block-local space (with identity PoseStack),
     * we apply the real PoseStack's transform matrix during replay to place them in world space.
     *
     * @param poseStack the current PoseStack from the render call (contains block-to-world transform)
     * @param buffer the real MultiBufferSource to render into
     * @param combinedLight current light value (used for light UV override if light changed,
     *                      but we rebuild on light change so this is just for consistency)
     */
    public void replay(PoseStack poseStack, MultiBufferSource buffer, int combinedLight) {
        if (cachedVertices.isEmpty()) {
            return;
        }

        VertexConsumer builder = buffer.getBuffer(RenderType.solid());
        org.joml.Matrix4f matrix = poseStack.last().pose();

        for (CachedVertex v : cachedVertices) {
            builder.addVertex(matrix, v.x, v.y, v.z)
                    .setColor(v.r, v.g, v.b, v.a)
                    .setUv(v.u, v.v)
                    .setUv2(v.lightU, v.lightV)
                    .setNormal(v.normalX, v.normalY, v.normalZ);
        }
    }

    /**
     * Clear cached data. Call when the block entity is removed.
     */
    public void clear() {
        cachedVertices = Collections.emptyList();
        dirty = true;
        lastCombinedLight = -1;
    }

    /**
     * Functional interface for the render action callback.
     * This allows the BER to pass its rendering logic without the cache needing
     * to know about PipeBlockEntity internals.
     */
    @FunctionalInterface
    public interface RenderAction {
        void render(PoseStack poseStack, MultiBufferSource buffer);
    }
}