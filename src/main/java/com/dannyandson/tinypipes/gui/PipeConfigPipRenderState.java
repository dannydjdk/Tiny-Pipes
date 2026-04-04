package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * Carries all data needed by PipeConfigPipRenderer to draw the 3D pipe config scene.
 * Submitted once per frame from PipeConfigGUI.extractRenderState().
 */
public record PipeConfigPipRenderState(
        PipeBlockEntity pipeBlockEntity,
        int slotPos,
        float rotationX,
        float rotationY,
        @Nullable Direction hoveredFace,
        int x0, int y0, int x1, int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    /** Convenience constructor — computes bounds from the viewport coordinates. */
    public PipeConfigPipRenderState(PipeBlockEntity pbe, int slotPos,
                                    float rotX, float rotY,
                                    @Nullable Direction hoveredFace,
                                    int x0, int y0, int x1, int y1,
                                    float scale,
                                    @Nullable ScreenRectangle scissorArea) {
        this(pbe, slotPos, rotX, rotY, hoveredFace,
                x0, y0, x1, y1, scale, scissorArea,
                new ScreenRectangle(x0, y0, x1 - x0, y1 - y0));
    }
}