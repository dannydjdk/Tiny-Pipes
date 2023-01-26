package com.dannyandson.tinypipes.blocks;

import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity> {

    public PipeBlockEntityRenderer(BlockEntityRendererProvider.Context context){
    }

    @Override
    public void render(PipeBlockEntity pipeBlockEntity, float p_112308_, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {

        VertexConsumer builder = buffer.getBuffer(RenderType.solid());
        TextureAtlasSprite sprite = pipeBlockEntity.getCenterSprite();

        poseStack.pushPose();

        //draw center cube
        RenderHelper.drawCube(poseStack, builder, sprite, 0.3125f, 0.6875f, 0.3125f, 0.6875f, 0.3125f, 0.6875f, combinedLight, 0xFFFFFFFF, 1.0f);

        AbstractFullPipe[] pipes = pipeBlockEntity.getPipes();
        boolean single = pipes.length == 1;

        for (AbstractFullPipe pipe : pipes) {

            for (Direction direction : new Direction[]{Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST}) {
                drawSide(pipe.getPipeSideStatus(direction), single, poseStack, builder, sprite, combinedLight);
                poseStack.translate(0, 0, 1);
                poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
            }

            poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
            poseStack.translate(0, 0, -1);
            drawSide(pipe.getPipeSideStatus(Direction.UP), single, poseStack, builder, sprite, combinedLight);

            poseStack.mulPose(Vector3f.XP.rotationDegrees(180));
            poseStack.translate(0, -1, -1);
            drawSide(pipe.getPipeSideStatus(Direction.DOWN), single, poseStack, builder, sprite, combinedLight);

        }

        poseStack.popPose();
    }

    private void drawSide(PipeSideStatus sideStatus, boolean single, PoseStack poseStack, VertexConsumer builder, TextureAtlasSprite sprite, int combinedLight){
        if (single) {
            if (sideStatus == PipeSideStatus.ENABLED) {
                RenderHelper.drawCube(poseStack, builder, sprite, 0.359375f, 0.640625f, 0, 0.3125f, 0.359375f, 0.640625f, combinedLight, 0xFFFFFFFF, 1.0f);
            }
            if (sideStatus == PipeSideStatus.PULLING) {
                RenderHelper.drawCube(poseStack, builder, sprite, 0.359375f, 0.640625f, 0.125f, 0.3125f, 0.359375f, 0.640625f, combinedLight, 0xFFFFFFFF, 1.0f);
                RenderHelper.drawCube(poseStack, builder, sprite, .25f, .75f, 0, 0.125f, .25f, .75f, combinedLight, 0xFFFFFFFF, 1.0f);
            }
        }

    }

}
