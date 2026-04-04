package com.dannyandson.tinypipes.blocks.rendering;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Render state for PipeBlockEntityRenderer.
 * Carries a snapshot reference to the block entity's cached renderer and
 * the block entity itself for cache rebuild access.
 *
 * The vertex cache (CachedPipeRenderer) handles all per-frame optimization.
 * This render state simply bridges the 26.1 extract/submit split.
 */
public class PipeBlockEntityRenderState extends BlockEntityRenderState {

    // Reference to the block entity for render geometry rebuild.
    // Safe to hold here because extractRenderState runs on the render thread
    // and submit runs immediately after in the same frame.
    public PipeBlockEntity pipeBlockEntity;

    // The block entity's cached renderer
    public CachedPipeRenderer cachedRenderer;
}
