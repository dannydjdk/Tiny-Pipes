package com.dannyandson.tinypipes.components;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinyredstone.blocks.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class EnergyPipe extends AbstractTinyPipe {

    public static final ResourceLocation ENERGY_PIPE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/energy_pipe");
    private static TextureAtlasSprite sprite = null;

    private boolean disabled = false;

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(ENERGY_PIPE_TEXTURE);
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

        if (
                (rightNeighbor != null && rightNeighbor.getStrongRsOutput() > 0) ||
                        (leftNeighbor != null && leftNeighbor.getStrongRsOutput() > 0) ||
                        (backNeighbor != null && backNeighbor.getStrongRsOutput() > 0) ||
                        (frontNeighbor != null && frontNeighbor.getStrongRsOutput() > 0) ||
                        (topNeighbor != null && topNeighbor.getStrongRsOutput() > 0) ||
                        (bottomNeighbor != null && bottomNeighbor.getStrongRsOutput() > 0)
        )
            disabled = true;
        else
            disabled = false;

        return false;
    }

    @Override
    public boolean tick(PanelCellPos cellPos) {
        if (disabled) return false;

        //clear query ids
        pushIds.clear();
        pushWrapper = null;

        for (Side side : pullSides) {
            //if set to pull, check for connected neighbor with item capabilities
            PanelCellNeighbor extractNeighbor = cellPos.getNeighbor(side);
            BlockPos neighborBlockPos = (extractNeighbor==null)?null:extractNeighbor.getBlockPos();

            if (neighborBlockPos != null) {
                BlockEntity neighborBlockEntity = cellPos.getPanelTile().getLevel().getBlockEntity(neighborBlockPos);
                if (neighborBlockEntity != null) {
                    Capability<IEnergyStorage> iEnergyStorageCapability = CapabilityManager.get(new CapabilityToken<>() {
                    });
                    BlockPos panelBlockPos = cellPos.getPanelTile().getBlockPos();
                    Direction neighborSide =
                            (neighborBlockPos.relative(Direction.NORTH).equals(panelBlockPos)) ? Direction.NORTH :
                                    (neighborBlockPos.relative(Direction.EAST).equals(panelBlockPos)) ? Direction.EAST :
                                            (neighborBlockPos.relative(Direction.SOUTH).equals(panelBlockPos)) ? Direction.SOUTH :
                                                    (neighborBlockPos.relative(Direction.WEST).equals(panelBlockPos)) ? Direction.WEST :
                                                            (neighborBlockPos.relative(Direction.UP).equals(panelBlockPos)) ? Direction.UP :
                                                                    Direction.DOWN;

                    IEnergyStorage iEnergyStorage = neighborBlockEntity.getCapability(iEnergyStorageCapability, neighborSide).orElse(null);
                    if (iEnergyStorage != null) {
                        int toExtract = 20;
                        int energy = iEnergyStorage.extractEnergy(toExtract, true);
                        if (energy > 0) {
                            int remainingEnergy = energy;
                            //we found energy that can be extracted
                            //see if there's a place to put it
                            PushWrapper pushWrapper = getPushWrapper(cellPos, energy);
                            for (PushWrapper.PushBlockEntity pushBlockEntity : pushWrapper.getSortedBlockEntities()) {
                                //grab capabilities and push
                                IEnergyStorage iEnergyStorage2 = pushBlockEntity.getIEnergyStorage();
                                if (iEnergyStorage2 != null && iEnergyStorage2.canReceive()) {
                                    int energyReceived = iEnergyStorage2.receiveEnergy(remainingEnergy, false);
                                    remainingEnergy -= energyReceived;
                                    if (energy == 0) break;
                                }
                            }
                            if (remainingEnergy < energy)
                                iEnergyStorage.extractEnergy(energy - remainingEnergy, false);
                        }
                    }
                }
            }
        }
        return false;
    }

    private PushWrapper getPushWrapper(PanelCellPos cellPos, int power) {
        if (pushWrapper == null) {
            this.pushWrapper = new PushWrapper();
            populatePushWrapper(cellPos, null, power, this.pushWrapper, 0);
        }
        return pushWrapper;
    }

    private void populatePushWrapper(PanelCellPos cellPos, @Nullable Side side, int power, PushWrapper pushWrapper, int distance) {
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
                    neighborPipe.populatePushWrapper(pushToNeighbor.getCellPos(), pushToNeighbor.getNeighborsSide(), power, pushWrapper, distance + 1);
                } else if (pushToNeighbor != null && pushToNeighbor.getBlockPos() != null) {
                    //edge of tile found, check for a neighboring tile entity
                    BlockPos neighborBlockPos = pushToNeighbor.getBlockPos();
                    BlockEntity neighborBlockEntity = cellPos.getPanelTile().getLevel().getBlockEntity(neighborBlockPos);
                    if (neighborBlockEntity != null) {
                        BlockPos panelBlockPos = cellPos.getPanelTile().getBlockPos();
                        Direction neighborSide =
                                (neighborBlockPos.relative(Direction.NORTH).equals(panelBlockPos)) ? Direction.NORTH :
                                        (neighborBlockPos.relative(Direction.EAST).equals(panelBlockPos)) ? Direction.EAST :
                                                (neighborBlockPos.relative(Direction.SOUTH).equals(panelBlockPos)) ? Direction.SOUTH :
                                                        (neighborBlockPos.relative(Direction.WEST).equals(panelBlockPos)) ? Direction.WEST :
                                                                (neighborBlockPos.relative(Direction.UP).equals(panelBlockPos)) ? Direction.UP :
                                                                        Direction.DOWN;

                        pushWrapper.addBlockEntity(neighborBlockEntity, distance, neighborSide);
                    }
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
        disabled = compoundTag.getBoolean("disabled");
    }

}
