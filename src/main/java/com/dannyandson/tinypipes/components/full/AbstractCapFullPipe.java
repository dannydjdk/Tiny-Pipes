package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.ICapPipe;


public abstract class AbstractCapFullPipe<CapType> extends AbstractFullPipe implements ICapPipe<CapType> {
    protected PushWrapper<CapType> pushWrapper = null;
    protected int amountPushed = 0;
    protected boolean disabled = false;


    public abstract int canAccept(int amount);

    public void didPush(int amount) {
        amountPushed+=amount;
    }

    @Override
    public boolean tick(PipeBlockEntity pipeBlockEntity) {
        amountPushed=0;
        return super.tick(pipeBlockEntity);
    }
}
