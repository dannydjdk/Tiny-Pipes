package com.dannyandson.tinypipes.blocks.rendering;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.components.full.AbstractCapFullPipe;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.components.full.RedstonePipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity> {

    public PipeBlockEntityRenderer(BlockEntityRendererProvider.Context context){
    }

    @Override
    public void render(PipeBlockEntity pipeBlockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        CachedPipeRenderer cache = pipeBlockEntity.getCachedRenderer();

        // Check if we need to rebuild the cache
        if (cache.isDirty() || cache.lightChanged(combinedLight)) {
            cache.rebuild(
                    (capturePoseStack, captureBuffer) ->
                            renderGeometry(pipeBlockEntity, capturePoseStack, captureBuffer, combinedLight, combinedOverlay),
                    combinedLight
            );
        }

        // Replay cached vertices with the real PoseStack (applies block-to-world transform)
        cache.replay(poseStack, buffer, combinedLight);
    }

    /**
     * The actual geometry generation logic.
     * Called during cache rebuild with a capture PoseStack (identity) and capture buffer.
     * Positions are baked into block-local space during capture; on replay only the
     * block-to-world transform from the real PoseStack is applied.
     */
    public static void renderGeometry(PipeBlockEntity pipeBlockEntity, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        renderGeometry(pipeBlockEntity, poseStack, buffer, combinedLight, combinedOverlay, 1.0f);
    }

    public static void renderGeometry(PipeBlockEntity pipeBlockEntity, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, float alpha) {
        RenderType renderType = alpha < 1.0f ? RenderType.translucent() : RenderType.solid();
        VertexConsumer builder = buffer.getBuffer(renderType);

        if(pipeBlockEntity.getCamouflageBlockState()!=null) {
            BlockState camouflageState = pipeBlockEntity.getCamouflageBlockState();
            var blockRenderer = Minecraft.getInstance().getBlockRenderer();
            BakedModel model = blockRenderer.getBlockModel(camouflageState);

            poseStack.pushPose();
            blockRenderer.getModelRenderer().tesselateBlock(
                    pipeBlockEntity.getLevel(),
                    model,
                    camouflageState,
                    pipeBlockEntity.getBlockPos(),
                    poseStack,
                    builder,
                    false,
                    RandomSource.create(),
                    camouflageState.getSeed(pipeBlockEntity.getBlockPos()),
                    combinedOverlay
            );
            poseStack.popPose();

            return;
        }

        TextureAtlasSprite sprite = pipeBlockEntity.getCenterSprite();
        AbstractFullPipe[] pipes = pipeBlockEntity.getPipes();
        boolean single = pipes.length == 1;

        poseStack.pushPose();

        //draw center cube
        if (single)
            RenderHelper.drawCube(poseStack, builder, sprite, 0.4f, 0.6f, 0.4f, 0.6f, 0.4f, 0.6f, combinedLight, 0xFFFFFFFF, alpha);
        else
            RenderHelper.drawCube(poseStack, builder, sprite, 0.3125f, 0.6875f, 0.3125f, 0.6875f, 0.3125f, 0.6875f, combinedLight, 0xFFFFFFFF, alpha);

        for (AbstractFullPipe pipe : pipes) {
            RedstonePipe rsPipe = (pipe instanceof RedstonePipe)?(RedstonePipe) pipe:null;
            int color = pipe.getColor();
            int upgrades = (pipe instanceof AbstractCapFullPipe)?((AbstractCapFullPipe<?>) pipe).getSpeedUpgradeCount():0;
            Integer upgradeColor = (upgrades>0)?0xFF226600+0xFF/Config.SPEED_UPGRADE_MAX.get()*upgrades :0xFF222222;
            poseStack.pushPose();


            int slot = (single)?-1: pipe.slotPos();
            sprite = pipe.getSprite();
            for (Direction direction : new Direction[]{Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST}) {
                Integer connectionColor = (rsPipe!=null)?rsPipe.getColor(direction):(!pipe.getNeighborHasSamePipeType(direction))?upgradeColor:null;
                drawSide(pipe.getPipeSideStatus(direction), slot, poseStack, builder, sprite, combinedLight,direction.getAxisDirection(), color, connectionColor, pipe.getNeighborIsPipeCluster(direction), alpha);
                poseStack.translate(0, 0, 1);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }

            Integer connectionColor = (rsPipe!=null)?rsPipe.getColor(Direction.UP):(!pipe.getNeighborHasSamePipeType(Direction.UP))?upgradeColor:null;
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.translate(0, 0, -1);
            drawSide(pipe.getPipeSideStatus(Direction.UP), slot, poseStack, builder, sprite, combinedLight, Direction.AxisDirection.POSITIVE,color, connectionColor, pipe.getNeighborIsPipeCluster(Direction.UP), alpha);

            connectionColor = (rsPipe!=null)?rsPipe.getColor(Direction.DOWN):(!pipe.getNeighborHasSamePipeType(Direction.DOWN))?upgradeColor:null;
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-1, 0, -1);
            drawSide(pipe.getPipeSideStatus(Direction.DOWN), slot , poseStack, builder, sprite, combinedLight, Direction.AxisDirection.NEGATIVE,color, connectionColor, pipe.getNeighborIsPipeCluster(Direction.DOWN), alpha);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void drawSide(PipeConnectionState sideStatus, int slot, PoseStack poseStack, VertexConsumer builder, TextureAtlasSprite sprite, int combinedLight, Direction.AxisDirection dir, int pipeColor, Integer connectionColor, Boolean clusterNeighbor, float alpha) {
        // 0.359375f 0.5f 0.640625f
        boolean alt = dir == Direction.AxisDirection.NEGATIVE;
        boolean xRight = (slot == 0 && alt) || (slot == 1 && !alt) || (slot == 2 && alt) || (slot == 3 && !alt);
        boolean yUpper = slot == 0 || slot == 1;

        float xmin = slot == -1 ? 0.4296875f : xRight ? 0.5f : 0.359375f;
        float xmax = slot == -1 ? 0.5703125f : xRight ? 0.640625f : 0.5f;
        float zmin = slot == -1 ? 0.4296875f : yUpper ? 0.5f : 0.359375f;
        float zmax = slot == -1 ? 0.5703125f : yUpper ? 0.640625f : 0.5f;
        float ymax = slot == -1 ? 0.4296875f : 0.3125f;

        if (sideStatus == PipeConnectionState.ENABLED) {
            if (slot==-1 && clusterNeighbor!=null && clusterNeighbor){
                RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0.0625f, ymax, zmin, zmax, combinedLight, pipeColor, alpha);
                RenderHelper.drawCube(poseStack, builder, sprite, 0.359375f, 0.640625f, 0, 0.0625f, 0.359375f, 0.640625f, combinedLight, pipeColor, alpha);
            }else if (connectionColor==null) {
                RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0, ymax, zmin, zmax, combinedLight, pipeColor, alpha);
            }else{
                RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0, ymax, zmin, zmax, combinedLight, pipeColor, alpha);
                RenderHelper.drawCube(poseStack, builder, PipeBlockEntity.getWhitePipeSprite(), xmin-.005f, xmax+.005f, 0.0625f, 0.125f, zmin-.005f, zmax+.005f, combinedLight, connectionColor, alpha);
            }
        } else if (sideStatus == PipeConnectionState.PULLING) {
            RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0.125f, ymax, zmin, zmax, combinedLight, pipeColor, alpha);
            RenderHelper.drawCube(poseStack, builder, PipeBlockEntity.getPullSprite(), xmin, xmax, 0, 0.125f, zmin, zmax, combinedLight, (connectionColor==null)?0xFFFFFFFF:connectionColor, alpha);
        } else if (slot != -1) {
            RenderHelper.drawCube(poseStack, builder, sprite, xmin, xmax, 0.28125f, 0.3125f, zmin, zmax, combinedLight, pipeColor, alpha);
        }

    }

}