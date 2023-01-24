package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeSideStatus;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFullPipe {

    private final Map<Direction, PipeSideStatus> sideStatusMap = new HashMap<>();

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
