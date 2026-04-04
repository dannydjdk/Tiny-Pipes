package com.dannyandson.tinypipes.blocks.rendering;

/**
 * Flat struct holding all per-vertex attributes for cached rendering.
 * Positions are stored pre-transformed relative to the block entity's local space.
 * Color is stored as floats (0-1) to avoid precision loss from repeated int/float conversions.
 * Light UV is stored as two separate ints matching the setUv2(int u, int v) contract.
 * Overlay UV stored for setUv1 (required in 26.1).
 */
public class CachedVertex {
    // Position (pre-transformed by the PoseStack matrix at capture time)
    public float x, y, z;

    // Color as floats (0.0 - 1.0)
    public float r, g, b, a;

    // Texture UV
    public float u, v;

    // Overlay UV (for setUv1 — required in 26.1)
    public int overlayU, overlayV;

    // Light UV (two separate ints for setUv2)
    public int lightU, lightV;

    // Normal
    public float normalX, normalY, normalZ;

    public CachedVertex() {
    }

    public CachedVertex(float x, float y, float z,
                        float r, float g, float b, float a,
                        float u, float v,
                        int overlayU, int overlayV,
                        int lightU, int lightV,
                        float normalX, float normalY, float normalZ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.u = u;
        this.v = v;
        this.overlayU = overlayU;
        this.overlayV = overlayV;
        this.lightU = lightU;
        this.lightV = lightV;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
    }
}
