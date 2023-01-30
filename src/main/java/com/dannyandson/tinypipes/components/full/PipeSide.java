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

    public void toggleSideStatus()
    {
        pipe.togglePipeSide(direction);
    }

    public boolean removePipe()
    {
        return pipeBlockEntity.removePipe(pipe);
    }

    public Direction getDirection() {
        return direction;
    }
}
