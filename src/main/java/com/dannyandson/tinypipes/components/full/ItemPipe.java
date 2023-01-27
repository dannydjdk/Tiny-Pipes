package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.components.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import static com.dannyandson.tinypipes.components.RenderHelper.ITEM_PIPE_TEXTURE;

public class ItemPipe extends AbstractFullPipe{

    private static TextureAtlasSprite sprite = null;

    @Override
    public TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(ITEM_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public int slotPos() {
        return 0;
    }
}
