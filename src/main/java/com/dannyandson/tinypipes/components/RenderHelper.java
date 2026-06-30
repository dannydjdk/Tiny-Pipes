package com.dannyandson.tinypipes.components;

import com.dannyandson.tinypipes.TinyPipes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;


public class RenderHelper {

    /**
     * When true, drawRectangle/drawRectangle2 skip applyShade and use the raw color.
     * Set by PiP renderers where the pose stack's normal matrix includes view-space
     * rotations that corrupt the model-space face shading.
     */
    public static boolean disableDirectionalShading = false;

    public static final Identifier REDSTONE_PIPE_TEXTURE = Identifier.fromNamespaceAndPath(TinyPipes.MODID, "block/redstone_pipe");
    public static final Identifier ENERGY_PIPE_TEXTURE = Identifier.fromNamespaceAndPath(TinyPipes.MODID, "block/energy_pipe");
    public static final Identifier FLUID_FILTER_PIPE_TEXTURE = Identifier.fromNamespaceAndPath(TinyPipes.MODID, "block/fluid_filter_pipe");
    public static final Identifier FLUID_PIPE_TEXTURE = Identifier.fromNamespaceAndPath(TinyPipes.MODID, "block/fluid_pipe");
    public static final Identifier ITEM_FILTER_PIPE_TEXTURE = Identifier.fromNamespaceAndPath(TinyPipes.MODID, "block/item_filter_pipe");
    public static final Identifier ITEM_PIPE_TEXTURE = Identifier.fromNamespaceAndPath(TinyPipes.MODID, "block/item_pipe");

    public static void drawCube(PoseStack poseStack, VertexConsumer builder, TextureAtlasSprite sprite, float x1, float x2, float y1, float y2, float z1, float z2, int combinedLight, int color, float alpha) {
        drawCube(poseStack, builder, sprite, x1, x2, y1, y2, z1, z2, combinedLight, color, alpha, true);
    }

    public static void drawCube(PoseStack poseStack, VertexConsumer builder, TextureAtlasSprite sprite, float x1, float x2, float y1, float y2, float z1, float z2, int combinedLight, int color, float alpha, Boolean caps){

        poseStack.pushPose();

        //top
        poseStack.translate(0,0,y2);
        if (caps)
            drawRectangle(builder,poseStack,1-x2,1-x1,z1,z2,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1.0-y2);

        //front
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.translate(0,0,z2);
        drawRectangle(builder,poseStack,1-x2,1-x1,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1-z2);

        //right
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(0,0,1-x1);
        drawRectangle(builder,poseStack,1-z2,1-z1,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,x1);

        //back
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(0,0,1-z1);
        drawRectangle(builder,poseStack,x1,x2,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,z1);

        //left
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(0,0,x2);
        drawRectangle(builder,poseStack,z1,z2,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1-x2);

        //bottom
        if (caps) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            poseStack.translate(-1, 0, 1 - y1);
            drawRectangle(builder, poseStack, x1, x2, z1, z2, sprite, combinedLight, color, alpha);
        }

        poseStack.popPose();
    }

    public static void drawRectangle(VertexConsumer builder, PoseStack matrixStack, float x1, float x2, float y1, float y2, TextureAtlasSprite sprite, int combinedLight , int color, float alpha) {
        drawRectangle(builder, matrixStack, x1, x2, y1, y2, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), combinedLight, color, alpha);
    }

    public static void drawRectangle(VertexConsumer builder, PoseStack matrixStack, float x1, float x2, float y1, float y2, float u0, float u1, float v0, float v1, int combinedLight , int color, float alpha){
        int shadedColor;
        if (disableDirectionalShading) {
            shadedColor = color;
        } else {
            // Compute the local face normal from the quad's winding order.
            float localNz = Math.signum((x2 - x1) * (y2 - y1));
            Vector3f normal = matrixStack.last().normal().transform(new Vector3f(0, 0, localNz));
            normal.normalize();
            shadedColor = applyShade(color, getShadeFromNormal(normal.x, normal.y, normal.z));
        }
        Matrix4f matrix4f = matrixStack.last().pose();
        // Pass UP normal (0,1,0) so the shader shade factor is 1.0 —
        // vertex colors already have directional shading baked in via applyShade().
        add(builder, matrix4f, x1, y1, 0, u0, v0, combinedLight, shadedColor, alpha, 0f, 1f, 0f);
        add(builder, matrix4f, x2, y1, 0, u1, v0, combinedLight, shadedColor, alpha, 0f, 1f, 0f);
        add(builder, matrix4f, x2, y2, 0, u1, v1, combinedLight, shadedColor, alpha, 0f, 1f, 0f);
        add(builder, matrix4f, x1, y2, 0, u0, v1, combinedLight, shadedColor, alpha, 0f, 1f, 0f);
    }

    public static void drawRectangle2(VertexConsumer builder, PoseStack matrixStack, float x1, float x2, float y1, float y2, TextureAtlasSprite sprite, int combinedLight , int color, float alpha){
        int shadedColor;
        if (disableDirectionalShading) {
            shadedColor = color;
        } else {
            float localNz = Math.signum((x2 - x1) * (y2 - y1));
            Vector3f normal = matrixStack.last().normal().transform(new Vector3f(0, 0, localNz));
            normal.normalize();
            shadedColor = applyShade(color, getShadeFromNormal(normal.x, normal.y, normal.z));
        }
        Matrix4f matrix4f = matrixStack.last().pose();
        // Pass UP normal (0,1,0) — same double-shading fix as drawRectangle().
        add(builder, matrix4f, x1, y1, 0, sprite.getU0(), sprite.getV1(), combinedLight, shadedColor, alpha, 0f, 1f, 0f);
        add(builder, matrix4f, x2, y1, 0, sprite.getU1(), sprite.getV1(), combinedLight, shadedColor, alpha, 0f, 1f, 0f);
        add(builder, matrix4f, x2, y2, 0, sprite.getU1(), sprite.getV0(), combinedLight, shadedColor, alpha, 0f, 1f, 0f);
        add(builder, matrix4f, x1, y2, 0, sprite.getU0(), sprite.getV0(), combinedLight, shadedColor, alpha, 0f, 1f, 0f);
    }


    public static void add(VertexConsumer renderer, Matrix4f matrix4f, float x, float y, float z, float u, float v, int combinedLightIn, int color, float alpha, float nx, float ny, float nz) {
        renderer.addVertex(matrix4f, x, y, z)
                .setColor(color >> 16 & 255,color >> 8 & 255, color & 255, (int)(alpha*255f))
                .setUv(u, v)
                .setUv1(0, 10)
                .setUv2(combinedLightIn & 0xFFFF, (combinedLightIn >> 16) & 0xFFFF)
                .setNormal(nx, ny, nz);
    }

    /**
     * Returns the diffuse shade multiplier based on which axis a normal is aligned to,
     * matching Minecraft's standard block face shading.
     */
    public static float getShadeFromNormal(float nx, float ny, float nz) {
        float ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        if (ay >= ax && ay >= az) {
            return ny > 0 ? 1.0f : 0.5f;  // UP or DOWN
        } else if (az >= ax) {
            return 0.8f;  // NORTH or SOUTH
        } else {
            return 0.6f;  // EAST or WEST
        }
    }

    /**
     * Multiplies the RGB channels of a color by a shade factor.
     */
    public static int applyShade(int color, float shade) {
        int r = (int)((color >> 16 & 255) * shade);
        int g = (int)((color >> 8 & 255) * shade);
        int b = (int)((color & 255) * shade);
        int a = color >> 24 & 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static TextureAtlasSprite getSprite(Identifier resourceLocation)
    {
        return Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, resourceLocation));
    }

}