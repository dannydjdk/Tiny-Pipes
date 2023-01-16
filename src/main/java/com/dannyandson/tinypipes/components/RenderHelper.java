package com.dannyandson.tinypipes.components;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class RenderHelper {

    public static void drawCube(PoseStack poseStack, VertexConsumer builder, TextureAtlasSprite sprite, float x1, float x2, float y1, float y2, float z1, float z2, int combinedLight, int color, float alpha){

        poseStack.pushPose();

        //top
        poseStack.translate(0,0,y2);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,1-x2,1-x1,z1,z2,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1.0-y2);

        //front
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.translate(0,0,z2);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,1-x2,1-x1,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1-z2);

        //right
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(0,0,1-x1);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,1-z2,1-z1,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,x1);

        //back
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(0,0,1-z1);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,x1,x2,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,z1);

        //left
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(0,0,x2);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,z1,z2,1-y2,1-y1,sprite,combinedLight,color,alpha);
        poseStack.translate(0,0,1-x2);

        //bottom
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
        poseStack.translate(-1,0,1-y1);
        com.dannyandson.tinyredstone.blocks.RenderHelper.drawRectangle(builder,poseStack,x1,x2,z1,z2,sprite,combinedLight,color,alpha);

        poseStack.popPose();
    }

}
