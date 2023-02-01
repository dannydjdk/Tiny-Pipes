package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.ICapPipe;
import net.minecraft.nbt.CompoundTag;


public abstract class AbstractCapFullPipe<CapType> extends AbstractFullPipe implements ICapPipe<CapType> {
    protected PushWrapper<CapType> pushWrapper = null;
    protected int amountPushed = 0;
    private int speedUpgrades = 0;
    protected boolean disabled = false;

    public abstract int canAccept(int amount);

    public void didPush(int amount) {
        amountPushed+=amount;
    }

    public boolean applySpeedUpgrade()
    {
        if (speedUpgrades < Config.SPEED_UPGRADE_MAX.get()) {
            speedUpgrades++;
            return true;
        }
        return false;
    }

    public boolean removeSpeedUpgrade()
    {
        if (speedUpgrades > 0) {
            speedUpgrades--;
            return true;
        }
        return false;
    }

    public double getSpeedMultiplier(){
        return Math.pow(Config.SPEED_UPGRADE_MULTIPLIER.get(),speedUpgrades);
    }

    public int getSpeedUpgradeCount()
    {
        return speedUpgrades;
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
        if (compoundTag.contains("speedUpgrades"))
            speedUpgrades = compoundTag.getInt("speedUpgrades");
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag compoundTag = super.writeNBT();
        compoundTag.putBoolean("disabled",disabled);
        compoundTag.putInt("speedUpgrades",speedUpgrades);
        return compoundTag;
    }
}
