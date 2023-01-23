package com.dannyandson.tinypipes.components;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class RenderHelper {

    public static void drawCube(PoseStack poseStack, VertexConsumer builder, TextureAtlasSprite sprite, float x1, float x2, float y1, float y2, float z1, float z2, int combinedLight, int color, float alpha){

        poseStack.pushPose();

        //top
        poseStack.translate(0,0,y2);
        drawRectangle(builder,poseStack,1-x2,1-x1,z1,z2,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1.0-y2);

        //front
        poseStack.mulPose(Vector3f.XP.rotationDegrees(-90));
        poseStack.translate(0,0,z2);
        drawRectangle(builder,poseStack,1-x2,1-x1,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1-z2);

        //right
        poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
        poseStack.translate(0,0,1-x1);
        drawRectangle(builder,poseStack,1-z2,1-z1,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,x1);

        //back
        poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
        poseStack.translate(0,0,1-z1);
        drawRectangle(builder,poseStack,x1,x2,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,z1);

        //left
        poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
        poseStack.translate(0,0,x2);
        drawRectangle(builder,poseStack,z1,z2,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1-x2);

        //bottom
        poseStack.mulPose(Vector3f.XP.rotationDegrees(-90));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(-90));
        poseStack.translate(-1,0,1-y1);
        drawRectangle(builder,poseStack,x1,x2,z1,z2,sprite,combinedLight,color,alpha);

        poseStack.popPose();
    }

    public static void drawRectangle(VertexConsumer builder, PoseStack matrixStack, float x1, float x2, float y1, float y2, TextureAtlasSprite sprite, int combinedLight , int color, float alpha) {
        drawRectangle(builder, matrixStack, x1, x2, y1, y2, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), combinedLight, color, alpha);
    }

    public static void drawRectangle(VertexConsumer builder, PoseStack matrixStack, float x1, float x2, float y1, float y2, float u0, float u1, float v0, float v1, int combinedLight , int color, float alpha){
        Matrix4f matrix4f = matrixStack.last().pose();
        add(builder, matrix4f, x1, y1, 0, u0, v0, combinedLight, color, alpha);
        add(builder, matrix4f, x2, y1, 0, u1, v0, combinedLight, color, alpha);
        add(builder, matrix4f, x2, y2, 0, u1, v1, combinedLight, color, alpha);
        add(builder, matrix4f, x1, y2, 0, u0, v1, combinedLight, color, alpha);
    }

    public static void add(VertexConsumer renderer, Matrix4f matrix4f, float x, float y, float z, float u, float v, int combinedLightIn, int color, float alpha) {
        renderer.vertex(matrix4f, x, y, z)
                .color(color >> 16 & 255,color >> 8 & 255, color & 255, (int)(alpha*255f))
                .uv(u, v)
                .uv2(combinedLightIn)
                .normal(1, 0, 0)
                .endVertex();
    }

}
