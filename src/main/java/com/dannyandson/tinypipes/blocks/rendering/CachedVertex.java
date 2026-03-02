package com.dannyandson.tinypipes.blocks.rendering;

/**
 * Flat struct holding all per-vertex attributes for cached rendering.
 * Positions are stored pre-transformed relative to the block entity's local space.
 * Color is stored as floats (0-1) to avoid precision loss from repeated int/float conversions.
 * Light UV is stored as two separate ints matching the setUv2(int u, int v) contract in 1.21.1.
 */
public class CachedVertex {
    // Position (pre-transformed by the PoseStack matrix at capture time)
    public float x, y, z;

    // Color as floats (0.0 - 1.0)
    public float r, g, b, a;

    // Texture UV
    public float u, v;

    // Light UV (two separate ints for setUv2)
    public int lightU, lightV;

    // Normal
    public float normalX, normalY, normalZ;

    public CachedVertex() {
    }

    public CachedVertex(float x, float y, float z,
                        float r, float g, float b, float a,
                        float u, float v,
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
        this.lightU = lightU;
        this.lightV = lightV;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
    }
}