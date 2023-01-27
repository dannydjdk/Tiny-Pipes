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
        if(false)return;
        VertexConsumer builder = buffer.getBuffer(RenderType.solid());
        TextureAtlasSprite sprite = pipeBlockEntity.getCenterSprite();
        AbstractFullPipe[] pipes = pipeBlockEntity.getPipes();
        boolean single = pipes.length == 1;

        poseStack.pushPose();

        //draw center cube
        if (single)
            RenderHelper.drawCube(poseStack, builder, sprite, 0.4f, 0.6f, 0.4f, 0.6f, 0.4f, 0.6f, combinedLight, 0xFFFFFFFF, 1.0f);
        else
            RenderHelper.drawCube(poseStack, builder, sprite, 0.3125f, 0.6875f, 0.3125f, 0.6875f, 0.3125f, 0.6875f, combinedLight, 0xFFFFFFFF, 1.0f);

        for (AbstractFullPipe pipe : pipes) {
            poseStack.pushPose();


            int slot = (single)?-1: pipe.slotPos();
            sprite = pipe.getSprite();
            for (Direction direction : new Direction[]{Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST}) {
                drawSide(pipe.getPipeSideStatus(direction), slot, poseStack, builder, sprite, combinedLight,direction.getAxisDirection());
                poseStack.translate(0, 0, 1);
                poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
            }

            poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
            poseStack.translate(0, 0, -1);
            drawSide(pipe.getPipeSideStatus(Direction.UP), slot, poseStack, builder, sprite, combinedLight, Direction.AxisDirection.POSITIVE);

            poseStack.mulPose(Vector3f.YP.rotationDegrees(180));
            poseStack.translate(-1, 0, -1);
            drawSide(pipe.getPipeSideStatus(Direction.DOWN), slot , poseStack, builder, sprite, combinedLight, Direction.AxisDirection.NEGATIVE);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void drawSide(PipeSideStatus sideStatus, int slot, PoseStack poseStack, VertexConsumer builder, TextureAtlasSprite sprite, int combinedLight, Direction.AxisDirection dir) {
        // 0.359375f 0.5f 0.640625f
        boolean alt = dir == Direction.AxisDirection.NEGATIVE;
        boolean xRight = (slot == 0 && alt) || (slot == 1 && !alt) || (slot == 2 && alt) || (slot == 3 && !alt);
        boolean yUpper = slot == 0 || slot == 1;

        float xmin = slot==-1 ? 0.4296875f : xRight ? 0.5f: 0.359375f;
        float xmax = slot==-1 ? 0.5703125f : xRight ? 0.640625f : 0.5f;
        float zmin = slot==-1 ? 0.4296875f : yUpper ? 0.5f: 0.359375f;
        float zmax = slot==-1 ? 0.5703125f : yUpper ? 0.640625f : 0.5f;
        float ymax = slot==-1 ? 0.4296875f : 0.3125f;

        if (sideStatus == PipeSideStatus.ENABLED) {
            RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0, ymax, zmin, zmax, combinedLight, 0xFFFFFFFF, 1.0f);
        }
        else if (sideStatus == PipeSideStatus.PULLING) {
            RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0.125f, ymax, zmin, zmax, combinedLight, 0xFFFFFFFF, 1.0f);
            RenderHelper.drawCube(poseStack, builder, PipeBlockEntity.getPullSprite(), xmin, xmax, 0, 0.125f, zmin, zmax, combinedLight, 0xFFFFFFFF, 1.0f);
        }
        else if (slot != -1){
            RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0.28125f, 0.3125f, zmin, zmax, combinedLight, 0xFFFFFFFF, 1.0f);
        }

    }

}
