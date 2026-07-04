package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import net.minecraft.core.Direction;

public class PipeSide<T extends AbstractFullPipe> {

    private PipeBlockEntity pipeBlockEntity;
    private T pipe;
    private Direction direction;

    public PipeSide(PipeBlockEntity pipeBlockEntity, T pipe, Direction direction){
        this.pipeBlockEntity = pipeBlockEntity;
        this.pipe = pipe;
        this.direction = direction;
    }

    public PipeConnectionState getSideStatus()
    {
        return pipe.getPipeSideStatus(direction);
    }

    public T getPipe()
    {
        return pipe;
    }

    public PipeBlockEntity getPipeBlockEntity()
    {
        return pipeBlockEntity;
    }

    public void toggleSideStatus(PipeBlockEntity pipeBlockEntity)
    {
        pipe.togglePipeSide(pipeBlockEntity, direction);
    }

    public boolean removePipe()
    {
        return pipeBlockEntity.removePipe(pipe);
    }

    public boolean applySpeedUpgrade() {
        if (pipe instanceof AbstractCapFullPipe abstractCapFullPipe
                && !abstractCapFullPipe.getNeighborHasSamePipeType(direction)
        ) {
            boolean applied = abstractCapFullPipe.applySpeedUpgrade();
            if (applied)
                pipeBlockEntity.sync();
            return applied;
        }
        return false;
    }

    public boolean removeSpeedUpgrade() {
        if (pipe instanceof AbstractCapFullPipe abstractCapFullPipe) {
            boolean removed = abstractCapFullPipe.removeSpeedUpgrade();
            if (removed)
                pipeBlockEntity.sync();
            return removed;
        }
        return false;
    }


    public int getSpeedUpgradeCount(){
        if (pipe instanceof AbstractCapFullPipe abstractCapFullPipe)
            return abstractCapFullPipe.getSpeedUpgradeCount();
        return 0;
    }

    public Direction getDirection() {
        return direction;
    }
}