package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.components.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import static com.dannyandson.tinypipes.components.RenderHelper.FLUID_PIPE_TEXTURE;

public class FluidPipe extends AbstractFullPipe{

    private static TextureAtlasSprite sprite = null;

    @Override
    public TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(FLUID_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public int slotPos() {
        return 1;
    }
}
