package com.dannyandson.tinypipes.blocks.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * A proxy MultiBufferSource that returns a CapturingVertexConsumer instead of a real buffer.
 * Since the pipe renderer only uses RenderType.solid(), a single consumer handles all calls.
 * If additional RenderTypes are needed in the future, this can be extended to use a Map.
 */
public class CaptureBufferSource implements MultiBufferSource {

    private final CapturingVertexConsumer consumer = new CapturingVertexConsumer();

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return consumer;
    }

    public CapturingVertexConsumer getConsumer() {
        return consumer;
    }
}