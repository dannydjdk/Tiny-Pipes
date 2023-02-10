package com.dannyandson.tinypipes.components.tiny;

import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.ICapPipe;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;


public abstract class AbstractCapPipe<CapType> extends AbstractTinyPipe implements ICapPipe<CapType> {
    protected PushWrapper<CapType> pushWrapper = null;
    protected int amountPushed = 0;


    public abstract int canAccept(int amount);

    public void didPush(int amount) {
        amountPushed+=amount;
    }

    @Override
    public boolean tick(PanelCellPos cellPos) {
        amountPushed=0;
        return false;
    }
}
