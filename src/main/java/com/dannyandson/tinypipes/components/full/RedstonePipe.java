package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinyredstone.blocks.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import static com.dannyandson.tinypipes.components.RenderHelper.REDSTONE_PIPE_TEXTURE;

public class RedstonePipe extends AbstractFullPipe{

    private static TextureAtlasSprite sprite = null;
    @Override
    public TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(REDSTONE_PIPE_TEXTURE);
        return sprite;
    }

}
