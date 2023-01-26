package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeSideStatus;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinyredstone.blocks.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFullPipe {

    private final Map<Direction, PipeSideStatus> sideStatusMap = new HashMap<>();

    private static TextureAtlasSprite sprite = null;
    public TextureAtlasSprite getSprite(){
        if (sprite==null)
            sprite = RenderHelper.getSprite(ClientSetup.PIPE_TEXTURE);
        return sprite;
    }

    /**
     * get the status of a side of a pipe (DISABLED, ENABLED, PULLING)
     * @param direction side of pipe being queried
     * @return PipeSideStatus enum
     */
    public PipeSideStatus getPipeSideStatus(Direction direction){
        PipeSideStatus status = sideStatusMap.get(direction);
        return (status==null)?PipeSideStatus.DISABLED:status;
    }

    public void togglePipeSide(Direction direction){
        if (sideStatusMap.get(direction)==PipeSideStatus.ENABLED)
            sideStatusMap.put(direction,PipeSideStatus.PULLING);
        else if (sideStatusMap.get(direction)==PipeSideStatus.PULLING)
            sideStatusMap.put(direction,PipeSideStatus.DISABLED);
        else
            sideStatusMap.put(direction,PipeSideStatus.ENABLED);
    }

    public CompoundTag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        for (Direction direction: sideStatusMap.keySet()){
            nbt.putString(direction.name(), sideStatusMap.get(direction).name());
        }
        return nbt;
    }

    public void readNBT(CompoundTag compoundTag) {
        for (String key : compoundTag.getAllKeys()){
            try {
                Direction direction = Direction.valueOf(key);
                PipeSideStatus status = PipeSideStatus.valueOf(compoundTag.getString(key));
                sideStatusMap.put(direction, status);
            }catch (IllegalArgumentException exception){
                TinyPipes.LOGGER.error("Exception attempting to read pipe direction from NBT " + key, exception);
            }
        }
    }
}
