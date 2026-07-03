package com.dannyandson.tinypipes.blocks.debug;

import com.dannyandson.tinypipes.setup.ModRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Debug-only finite energy buffer. Exposes an EnergyHandler capability so the energy pipes can
 * pull from / push to it, letting you verify that transfers conserve energy exactly (no loss/gain).
 * Registered only when the tinypipes.debugEnergy flag is set.
 */
public class DebugEnergyCellBlockEntity extends BlockEntity {

    public static final int CAPACITY = 10_000_000;

    // maxInsert/maxExtract == capacity so the pipe's own throughput is the only limit.
    // SimpleEnergyHandler is transaction-aware, so a pipe's simulate pass never actually drains it.
    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, CAPACITY, CAPACITY, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    public DebugEnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistration.DEBUG_ENERGY_CELL_BE.get(), pos, state);
    }

    public SimpleEnergyHandler getEnergy() {
        return energy;
    }

    public long getStored() {
        return energy.getAmountAsLong();
    }

    public long getCapacity() {
        return energy.getCapacityAsLong();
    }

    /** Directly overwrite stored energy (used by the block's debug interactions). */
    public void setStored(int amount) {
        energy.set(amount);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
    }
}