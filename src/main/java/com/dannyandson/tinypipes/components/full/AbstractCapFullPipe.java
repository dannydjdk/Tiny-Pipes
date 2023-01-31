package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.ICapPipe;
import net.minecraft.nbt.CompoundTag;


public abstract class AbstractCapFullPipe<CapType> extends AbstractFullPipe implements ICapPipe<CapType> {
    protected PushWrapper<CapType> pushWrapper = null;
    protected int amountPushed = 0;
    protected boolean disabled = false;

    public abstract int canAccept(int amount);

    public void didPush(int amount) {
        amountPushed+=amount;
    }

    @Override
    public int getColor() {
        if (disabled) return 0xFF888888;
        return super.getColor();
    }

    @Override
    public boolean neighborChanged(PipeBlockEntity pipeBlockEntity) {
        boolean change = disabled != pipeBlockEntity.getLevel().getDirectSignalTo(pipeBlockEntity.getBlockPos())>0;
        if (change){
            disabled=!disabled;
            pipeBlockEntity.sync();
        }
        return super.neighborChanged(pipeBlockEntity) || change;
    }

    @Override
    public boolean tick(PipeBlockEntity pipeBlockEntity) {
        amountPushed=0;
        return super.tick(pipeBlockEntity);
    }

    @Override
    public void readNBT(CompoundTag compoundTag) {
        super.readNBT(compoundTag);
        if (compoundTag.contains("disabled"))
            disabled = compoundTag.getBoolean("disabled");
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag compoundTag = super.writeNBT();
        compoundTag.putBoolean("disabled",disabled);
        return compoundTag;
    }
}
