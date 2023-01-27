package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.components.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import static com.dannyandson.tinypipes.components.RenderHelper.ENERGY_PIPE_TEXTURE;

public class EnergyPipe extends AbstractFullPipe{

    private static TextureAtlasSprite sprite = null;

    @Override
    public TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(ENERGY_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public int slotPos() {
        return 2;
    }
}
