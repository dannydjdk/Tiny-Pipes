package com.dannyandson.tinypipes.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity> {

    public PipeBlockEntityRenderer(BlockEntityRendererProvider.Context context){
    }

    @Override
    public void render(PipeBlockEntity pipeBlockEntity, float p_112308_, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {

    }
}
