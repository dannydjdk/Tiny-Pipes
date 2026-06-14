package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.ICapPipe;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;


public abstract class AbstractCapFullPipe<CapType> extends AbstractFullPipe implements ICapPipe<CapType> {
    protected PushWrapper<CapType> pushWrapper = null;
    protected int amountPushed = 0;
    private int speedUpgrades = 0;
    protected boolean disabled = false;

    public abstract int canAccept(int amount);

    /**
     * A short, localized description of this pipe's current transfer rate,
     * (via {@link #getSpeedMultiplier()}).
     */
    public abstract Component getSpeedDescription();

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
    public boolean neighborChanged(PipeBlockEntity pipeBlockEntity, @Nullable Direction direction) {
        boolean change = disabled != pipeBlockEntity.getLevel().getDirectSignalTo(pipeBlockEntity.getBlockPos())>0;
        if (change){
            disabled=!disabled;
            pipeBlockEntity.sync();
        }
        return super.neighborChanged(pipeBlockEntity, null) || change;
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
            disabled = compoundTag.getBooleanOr("disabled", false);
        if (compoundTag.contains("speedUpgrades"))
            speedUpgrades = compoundTag.getIntOr("speedUpgrades", 0);
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag compoundTag = super.writeNBT();
        compoundTag.putBoolean("disabled",disabled);
        compoundTag.putInt("speedUpgrades",speedUpgrades);
        return compoundTag;
    }
}