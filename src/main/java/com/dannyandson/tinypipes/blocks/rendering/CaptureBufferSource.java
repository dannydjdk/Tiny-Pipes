package com.dannyandson.tinypipes.blocks.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * A proxy MultiBufferSource that returns a CapturingVertexConsumer instead of a real buffer.
 * Since the pipe renderer uses a single buffer during capture, a single consumer handles all calls.
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
