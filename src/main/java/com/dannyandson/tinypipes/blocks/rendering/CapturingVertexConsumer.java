package com.dannyandson.tinypipes.blocks.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A VertexConsumer implementation that captures vertex data instead of submitting it to the GPU.
 *
 * Matches the 1.21.1 VertexConsumer contract:
 * - addVertex() starts a new vertex and implicitly commits the previous one
 * - Attribute setters (setColor, setUv, setUv2, setNormal) accumulate on the current in-progress vertex
 * - There is no endVertex() call; the last vertex must be flushed explicitly after rendering completes
 *
 * Positions are pre-transformed by the matrix at capture time so they are stored in block-local space.
 */
public class CapturingVertexConsumer implements VertexConsumer {

    private final List<CachedVertex> vertices = new ArrayList<>();

    // Current in-progress vertex
    private boolean hasPosition = false;
    private float cx, cy, cz;
    private float cr = 1f, cg = 1f, cb = 1f, ca = 1f;
    private float cu, cv;
    private int cLightU, cLightV;
    private float cnx, cny, cnz;

    /**
     * Flush the current in-progress vertex to the list (if one exists).
     * Must be called after all rendering is complete to capture the final vertex.
     */
    public void flush() {
        if (hasPosition) {
            vertices.add(new CachedVertex(
                    cx, cy, cz,
                    cr, cg, cb, ca,
                    cu, cv,
                    cLightU, cLightV,
                    cnx, cny, cnz
            ));
            hasPosition = false;
        }
    }

    public List<CachedVertex> getVertices() {
        return vertices;
    }

    public void clear() {
        vertices.clear();
        hasPosition = false;
    }

    // --- VertexConsumer implementation ---

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        // Flush previous vertex
        flush();

        // Start new vertex with raw position (no matrix - will be set via the Matrix4f overload)
        hasPosition = true;
        cx = x;
        cy = y;
        cz = z;

        // Reset attributes to defaults
        cr = 1f;
        cg = 1f;
        cb = 1f;
        ca = 1f;
        cu = 0f;
        cv = 0f;
        cLightU = 0;
        cLightV = 0;
        cnx = 0f;
        cny = 0f;
        cnz = 0f;

        return this;
    }

    /**
     * Override that applies the matrix transform at capture time.
     * This pre-transforms positions into block-local space.
     */
    @Override
    public VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        // Flush previous vertex
        flush();

        // Transform position by the current matrix and store result
        Vector3f pos = new Vector3f(x, y, z);
        pos.mulPosition(matrix);

        hasPosition = true;
        cx = pos.x();
        cy = pos.y();
        cz = pos.z();

        // Reset attributes to defaults
        cr = 1f;
        cg = 1f;
        cb = 1f;
        ca = 1f;
        cu = 0f;
        cv = 0f;
        cLightU = 0;
        cLightV = 0;
        cnx = 0f;
        cny = 0f;
        cnz = 0f;

        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        // Normalize int (0-255) to float (0-1) for storage
        cr = red / 255f;
        cg = green / 255f;
        cb = blue / 255f;
        ca = alpha / 255f;
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        cu = u;
        cv = v;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        // Overlay UV - not used by the pipe renderer, but implement for completeness
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        cLightU = u;
        cLightV = v;
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        cnx = x;
        cny = y;
        cnz = z;
        return this;
    }
}