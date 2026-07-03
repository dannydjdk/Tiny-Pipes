package com.dannyandson.tinypipes.components.tiny;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.caphandlers.ModCapabilityManager;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinyredstone.blocks.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

public class EnergyPipe extends AbstractCapPipe<EnergyHandler> {

    private static TextureAtlasSprite sprite = null;

    private boolean disabled = false;

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(com.dannyandson.tinypipes.components.RenderHelper.ENERGY_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    protected int getColor() {
        if (disabled) return 0xFF888888;
        return super.getColor();
    }

    @Override
    public boolean neighborChanged(PanelCellPos cellPos) {
        PanelCellNeighbor rightNeighbor = cellPos.getNeighbor(Side.RIGHT),
                leftNeighbor = cellPos.getNeighbor(Side.LEFT),
                backNeighbor = cellPos.getNeighbor(Side.BACK),
                frontNeighbor = cellPos.getNeighbor(Side.FRONT),
                topNeighbor = cellPos.getNeighbor(Side.TOP),
                bottomNeighbor = cellPos.getNeighbor(Side.BOTTOM);

        disabled = (rightNeighbor != null && rightNeighbor.getStrongRsOutput() > 0) ||
                (leftNeighbor != null && leftNeighbor.getStrongRsOutput() > 0) ||
                (backNeighbor != null && backNeighbor.getStrongRsOutput() > 0) ||
                (frontNeighbor != null && frontNeighbor.getStrongRsOutput() > 0) ||
                (topNeighbor != null && topNeighbor.getStrongRsOutput() > 0) ||
                (bottomNeighbor != null && bottomNeighbor.getStrongRsOutput() > 0);

        updateEdgeSides(cellPos);
        return false;
    }

    @Override
    protected boolean isDisabled() {
        return disabled;
    }

    @Override
    public boolean tick(PanelCellPos cellPos) {
        if (disabled) return false;
        super.tick(cellPos);

        //clear query ids
        pushIds.clear();
        pushWrapper = null;

        for (Side side : pullSides) {
            //if set to pull, check for connected neighbor with item capabilities
            PanelCellNeighbor extractNeighbor = cellPos.getNeighbor(side);
            BlockPos neighborBlockPos = (extractNeighbor == null) ? null : extractNeighbor.getBlockPos();

            if (neighborBlockPos != null) {
                BlockPos panelBlockPos = cellPos.getPanelTile().getBlockPos();
                Direction neighborSide =
                        (neighborBlockPos.relative(Direction.NORTH).equals(panelBlockPos)) ? Direction.NORTH :
                                (neighborBlockPos.relative(Direction.EAST).equals(panelBlockPos)) ? Direction.EAST :
                                        (neighborBlockPos.relative(Direction.SOUTH).equals(panelBlockPos)) ? Direction.SOUTH :
                                                (neighborBlockPos.relative(Direction.WEST).equals(panelBlockPos)) ? Direction.WEST :
                                                        (neighborBlockPos.relative(Direction.UP).equals(panelBlockPos)) ? Direction.UP :
                                                                Direction.DOWN;

                EnergyHandler iEnergyStorage = ModCapabilityManager.getIEnergyStorage(cellPos.getPanelTile().getLevel(), neighborBlockPos, neighborSide);
                if (iEnergyStorage != null) {
                    int toExtract = Config.ENERGY_THROUGHPUT.get();
                    //simulate: how much energy can we actually pull?
                    int energy;
                    try (Transaction sim = Transaction.openRoot()) {
                        energy = iEnergyStorage.extract(toExtract, sim);
                    }
                    if (energy > 0) {
                        int remainingEnergy = energy;
                        //only deliver to output sides sharing this pull side's channel
                        int pullFrequency = getFrequency(side);
                        //we found energy that can be extracted
                        //see if there's a place to put it
                        PushWrapper<EnergyHandler> pushWrapper = getPushWrapper(cellPos);
                        for (PushWrapper.PushTarget<EnergyHandler> pushTarget : pushWrapper.getSortedTargets()) {
                            if (pushTarget.getFrequency() != pullFrequency) continue;
                            //grab capabilities and push
                            EnergyHandler iEnergyStorage2 = pushTarget.getTarget();
                            if (iEnergyStorage2 != null && !iEnergyStorage2.equals(iEnergyStorage)) {
                                int pushLimit = pushTarget.getPipe().canAccept(remainingEnergy);
                                if (pushLimit>0) {
                                    //insert into the target and extract the same amount from the source atomically
                                    int energyReceived;
                                    try (Transaction move = Transaction.openRoot()) {
                                        energyReceived = iEnergyStorage2.insert(pushLimit, move);
                                        if (energyReceived > 0) {
                                            iEnergyStorage.extract(energyReceived, move);
                                            move.commit();
                                        }
                                    }
                                    if (energyReceived > 0) {
                                        pushTarget.getPipe().didPush(energyReceived);
                                        remainingEnergy -= energyReceived;
                                        if (remainingEnergy == 0) break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private PushWrapper<EnergyHandler> getPushWrapper(PanelCellPos cellPos) {
        if (pushWrapper == null) {
            this.pushWrapper = new PushWrapper<>();
            populatePushWrapper(cellPos, null, this.pushWrapper, 0);
        }
        return pushWrapper;
    }

    private void populatePushWrapper(PanelCellPos cellPos, @Nullable Side side, PushWrapper<EnergyHandler> pushWrapper, int distance) {
        //check if we've already played with this PushWrapper (to prevent infinite loops if there is a loop in the pipe network)
        if (disabled || pushIds.contains(pushWrapper.getId())) {
            //if so, return
            return;
        }
        //check if we're connected to the querying component
        if (side != null && !connectedSides.contains(side))
            return;

        //if checks pass, add id to list
        pushIds.add(pushWrapper.getId());

        //check if a destination exists on the side(s) set to push
        for (Side connectedSide : connectedSides) {
            if (!pullSides.contains(connectedSide)) {
                PanelCellNeighbor pushToNeighbor = cellPos.getNeighbor(connectedSide);
                if (pushToNeighbor != null && pushToNeighbor.getNeighborIPanelCell() instanceof EnergyPipe neighborPipe) {
                    //check the next cell
                    neighborPipe.populatePushWrapper(pushToNeighbor.getCellPos(), pushToNeighbor.getNeighborsSide(), pushWrapper, distance + 1);
                } else if (pushToNeighbor != null && pushToNeighbor.getBlockPos() != null) {
                    //edge of tile found, check for a neighboring tile entity
                    BlockPos neighborBlockPos = pushToNeighbor.getBlockPos();
                    BlockPos panelBlockPos = cellPos.getPanelTile().getBlockPos();
                    Direction neighborSide =
                            (neighborBlockPos.relative(Direction.NORTH).equals(panelBlockPos)) ? Direction.NORTH :
                                    (neighborBlockPos.relative(Direction.EAST).equals(panelBlockPos)) ? Direction.EAST :
                                            (neighborBlockPos.relative(Direction.SOUTH).equals(panelBlockPos)) ? Direction.SOUTH :
                                                    (neighborBlockPos.relative(Direction.WEST).equals(panelBlockPos)) ? Direction.WEST :
                                                            (neighborBlockPos.relative(Direction.UP).equals(panelBlockPos)) ? Direction.UP :
                                                                    Direction.DOWN;
                    pushWrapper.addPushTarget(ModCapabilityManager.getIEnergyStorage(cellPos.getPanelTile().getLevel(), neighborBlockPos, neighborSide), this, distance, 0, getFrequency(connectedSide));
                }
            }
        }
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();
        nbt.putBoolean("disabled", disabled);
        return nbt;
    }

    @Override
    public void readNBT(CompoundTag compoundTag) {
        super.readNBT(compoundTag);
        disabled = compoundTag.getBooleanOr("disabled", false);
    }

    @Override
    public int canAccept(int amount) {
        return Math.min(amount,(Config.ENERGY_THROUGHPUT.get()-amountPushed));
    }

}