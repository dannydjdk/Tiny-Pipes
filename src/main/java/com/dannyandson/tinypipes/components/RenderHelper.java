package com.dannyandson.tinypipes.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.vector.Vector3f;

public class RenderHelper {

    public static void drawCube(MatrixStack poseStack, IVertexBuilder builder, TextureAtlasSprite sprite, float x1, float x2, float y1, float y2, float z1, float z2, int combinedLight, int color, float alpha){

        poseStack.pushPose();

        //top
        poseStack.translate(0,0,y2);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,1-x2,1-x1,z1,z2,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1.0-y2);

        //front
        poseStack.mulPose(Vector3f.XP.rotationDegrees(-90));
        poseStack.translate(0,0,z2);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,1-x2,1-x1,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1-z2);

        //right
        poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
        poseStack.translate(0,0,1-x1);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,1-z2,1-z1,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,x1);

        //back
        poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
        poseStack.translate(0,0,1-z1);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,x1,x2,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,z1);

        //left
        poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
        poseStack.translate(0,0,x2);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,z1,z2,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1-x2);

        //bottom
        poseStack.mulPose(Vector3f.XP.rotationDegrees(-90));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(-90));
        poseStack.translate(-1,0,1-y1);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,x1,x2,z1,z2,sprite,combinedLight,color,alpha);

        poseStack.popPose();
    }

}
