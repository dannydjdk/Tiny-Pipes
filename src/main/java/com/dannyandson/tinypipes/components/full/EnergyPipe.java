package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeSideStatus;
import com.dannyandson.tinypipes.caphandlers.ModCapabilityManager;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

import static com.dannyandson.tinypipes.components.RenderHelper.ENERGY_PIPE_TEXTURE;

public class EnergyPipe extends AbstractCapFullPipe<IEnergyStorage>{

    private static TextureAtlasSprite sprite = null;

    @Override
    public TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(ENERGY_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public int slotPos() {
        return 2;
    }

    @Override
    public boolean tick(PipeBlockEntity pipeBlockEntity) {
        if (disabled) return false;
        super.tick(pipeBlockEntity);

        //clear query ids
        pushIds.clear();
        pushWrapper = null;

        for (Direction direction : Direction.values()) {
            if (getPipeSideStatus(direction) == PipeSideStatus.PULLING) {
                //if set to pull, check for connected neighbor with item capabilities
                BlockPos neighborBlockPos = pipeBlockEntity.getBlockPos().relative(direction);

                IEnergyStorage iEnergyStorage = ModCapabilityManager.getIEnergyStorage(pipeBlockEntity.getLevel(), neighborBlockPos, direction.getOpposite());
                if (iEnergyStorage != null) {
                    int toExtract = Config.ENERGY_THROUGHPUT.get();
                    int energy = iEnergyStorage.extractEnergy(toExtract, true);
                    if (energy > 0) {
                        int remainingEnergy = energy;
                        //we found energy that can be extracted
                        //see if there's a place to put it
                        PushWrapper<IEnergyStorage> pushWrapper = getPushWrapper(pipeBlockEntity);
                        for (PushWrapper.PushTarget<IEnergyStorage> pushTarget : pushWrapper.getSortedTargets()) {
                            //grab capabilities and push
                            IEnergyStorage iEnergyStorage2 = pushTarget.getTarget();
                            if (iEnergyStorage2 != null && !iEnergyStorage2.equals(iEnergyStorage) && iEnergyStorage2.canReceive()) {
                                int pushLimit = pushTarget.getPipe().canAccept(remainingEnergy);
                                if (pushLimit>0) {
                                    int energyReceived = iEnergyStorage2.receiveEnergy(pushLimit, false);
                                    pushTarget.getPipe().didPush(energyReceived);
                                    remainingEnergy -= energyReceived;
                                    if (remainingEnergy == 0) break;
                                }
                            }
                        }
                        if (remainingEnergy < energy)
                            iEnergyStorage.extractEnergy(energy - remainingEnergy, false);
                    }
                }
            }
        }
        return false;
    }

    private PushWrapper<IEnergyStorage> getPushWrapper(PipeBlockEntity pipeBlockEntity) {
        if (pushWrapper == null) {
            this.pushWrapper = new PushWrapper<>();
            populatePushWrapper(pipeBlockEntity, null, this.pushWrapper, 0);
        }
        return pushWrapper;
    }

    private void populatePushWrapper(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, PushWrapper<IEnergyStorage> pushWrapper, int distance) {
        //check if we've already played with this PushWrapper (to prevent infinite loops if there is a loop in the pipe network)
        if (disabled || pushIds.contains(pushWrapper.getId())) {
            //if so, return
            return;
        }
        //check if we're connected to the querying component
        if (side != null && getPipeSideStatus(side) == PipeSideStatus.DISABLED)
            return;

        //if checks pass, add id to list
        pushIds.add(pushWrapper.getId());

        //check if a destination exists on the side(s) set to push
        for (Direction direction : Direction.values()) {
            if (getPipeSideStatus(direction) == PipeSideStatus.ENABLED) {
                BlockPos pushToNeighbor = pipeBlockEntity.getBlockPos().relative(direction);
                if (pipeBlockEntity.getLevel().getBlockEntity(pushToNeighbor) instanceof PipeBlockEntity pipeBlockEntity2) {
                    if (pipeBlockEntity2.getPipe(this.slotPos()) instanceof EnergyPipe neighborPipe)
                    //check the next cell
                    neighborPipe.populatePushWrapper(pipeBlockEntity2, direction.getOpposite(), pushWrapper, distance + 1);
                } else  {
                    //edge of pipeline found, check for a neighboring tile entity
                    pushWrapper.addPushTarget(ModCapabilityManager.getIEnergyStorage(pipeBlockEntity.getLevel(), pushToNeighbor, direction.getOpposite()), this, distance);
                }
            }
        }
    }

    @Override
    public int canAccept(int amount) {
        return Math.min(amount,(Config.ENERGY_THROUGHPUT.get()-amountPushed));
    }

}
