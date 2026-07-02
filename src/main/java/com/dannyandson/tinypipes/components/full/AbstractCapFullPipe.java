package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.ICapPipe;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;


public abstract class AbstractCapFullPipe<CapType> extends AbstractFullPipe implements ICapPipe<CapType> {
    protected PushWrapper<CapType> pushWrapper = null;
    protected int amountPushed = 0;
    private int speedUpgrades = 0;
    protected boolean disabled = false;

    // Gray dye reverts a side to the default channel (no map entry == default).
    public static final int DEFAULT_FREQUENCY = DyeColor.GRAY.getId();

    // Per-side channel ("frequency"). Only non-default channels are stored; an absent
    // side uses DEFAULT_FREQUENCY. A pull side only delivers to output sides on the same channel.
    private final Map<Direction, Integer> frequencies = new HashMap<>();

    public abstract int canAccept(int amount);

    /**
     * A short, localized description of this pipe's current transfer rate,
     * accounting for installed speed upgrades (via {@link #getSpeedMultiplier()}).
     * Shown in the pipe config GUI.
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

    /**
     * The channel ("frequency") assigned to a side; DEFAULT_FREQUENCY when undyed.
     * Items/fluid/energy pulled in on one side only travel to output sides on the same channel.
     */
    public int getFrequency(Direction direction) {
        return frequencies.getOrDefault(direction, DEFAULT_FREQUENCY);
    }

    /**
     * Render color for a side's channel band, or null on pipe-to-pipe sides (no band drawn there).
     * The default channel renders as gray; dyed channels render in the dye's map color.
     */
    @CheckForNull
    public Integer getColor(Direction direction) {
        if (getNeighborHasSamePipeType(direction)) return null;
        return DyeColor.byId(getFrequency(direction)).getMapColor().col;
    }

    /**
     * Assign a channel to a side from a dye. Gray reverts the side to the default channel.
     * Only changes which output sides a given input feeds; routing recomputes each tick,
     * so this just updates state and re-syncs the band color to clients.
     */
    public void setColor(PipeBlockEntity pipeBlockEntity, Direction direction, int dyeId) {
        if (dyeId == DEFAULT_FREQUENCY)
            frequencies.remove(direction);
        else
            frequencies.put(direction, dyeId);
        pipeBlockEntity.markRenderDirty();
        pipeBlockEntity.sync();
    }

    @Override
    public void rotate(Rotation rotation) {
        super.rotate(rotation);
        if (rotation == Rotation.NONE) return;
        rotateMap(frequencies, rotation);
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
            disabled = compoundTag.getBoolean("disabled");
        if (compoundTag.contains("speedUpgrades"))
            speedUpgrades = compoundTag.getInt("speedUpgrades");
        if (compoundTag.contains("frequencies")) {
            for (String side : compoundTag.getCompound("frequencies").getAllKeys()) {
                frequencies.put(Direction.valueOf(side), compoundTag.getCompound("frequencies").getInt(side));
            }
        }
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag compoundTag = super.writeNBT();
        compoundTag.putBoolean("disabled",disabled);
        compoundTag.putInt("speedUpgrades",speedUpgrades);
        if (!frequencies.isEmpty()) {
            CompoundTag frequenciesNBT = new CompoundTag();
            for (Map.Entry<Direction, Integer> set : frequencies.entrySet())
                if (set.getKey() != null)
                    frequenciesNBT.putInt(set.getKey().name(), set.getValue());
            compoundTag.put("frequencies", frequenciesNBT);
        }
        return compoundTag;
    }
}