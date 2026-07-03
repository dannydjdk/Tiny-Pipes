package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.caphandlers.ModCapabilityManager;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

import static com.dannyandson.tinypipes.components.RenderHelper.ENERGY_PIPE_TEXTURE;

public class EnergyPipe extends AbstractCapFullPipe<EnergyHandler>{

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
    protected boolean canAutoConnectTo(net.minecraft.world.level.Level level, BlockPos neighborPos, Direction direction) {
        return ModCapabilityManager.getIEnergyStorage(level, neighborPos, direction.getOpposite()) != null;
    }

    @Override
    public Component getSpeedDescription() {
        int rate = (int) (Config.ENERGY_THROUGHPUT.get() * getSpeedMultiplier());
        return Component.translatable("tinypipes.gui.pipe_config.speed.energy", rate);
    }

    @Override
    public boolean tick(PipeBlockEntity pipeBlockEntity) {
        if (disabled) return false;
        super.tick(pipeBlockEntity);

        //clear query ids
        pushIds.clear();
        pushWrapper = null;

        for (Direction direction : Direction.values()) {
            if (getPipeSideStatus(direction) == PipeConnectionState.PULLING) {
                //if set to pull, check for connected neighbor with item capabilities
                BlockPos neighborBlockPos = pipeBlockEntity.getBlockPos().relative(direction);

                EnergyHandler iEnergyStorage = ModCapabilityManager.getIEnergyStorage(pipeBlockEntity.getLevel(), neighborBlockPos, direction.getOpposite());
                if (iEnergyStorage != null) {
                    int toExtract = (int) (Config.ENERGY_THROUGHPUT.get()*getSpeedMultiplier());
                    //simulate: how much energy can we actually pull?
                    int energy;
                    try (Transaction sim = Transaction.openRoot()) {
                        energy = iEnergyStorage.extract(toExtract, sim);
                    }
                    if (energy > 0) {
                        int remainingEnergy = energy;
                        //only deliver to output sides sharing this pull side's channel
                        int pullFrequency = getFrequency(direction);
                        //we found energy that can be extracted
                        //see if there's a place to put it
                        PushWrapper<EnergyHandler> pushWrapper = getPushWrapper(pipeBlockEntity);
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

    private PushWrapper<EnergyHandler> getPushWrapper(PipeBlockEntity pipeBlockEntity) {
        if (pushWrapper == null) {
            this.pushWrapper = new PushWrapper<>();
            populatePushWrapper(pipeBlockEntity, null, this.pushWrapper, 0);
        }
        return pushWrapper;
    }

    private void populatePushWrapper(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, PushWrapper<EnergyHandler> pushWrapper, int distance) {
        //check if we've already played with this PushWrapper (to prevent infinite loops if there is a loop in the pipe network)
        if (disabled || pushIds.contains(pushWrapper.getId())) {
            //if so, return
            return;
        }
        //check if we're connected to the querying component
        if (side != null && getPipeSideStatus(side) == PipeConnectionState.DISABLED)
            return;

        //if checks pass, add id to list
        pushIds.add(pushWrapper.getId());

        //check if a destination exists on the side(s) set to push
        for (Direction direction : Direction.values()) {
            if (getPipeSideStatus(direction) == PipeConnectionState.ENABLED) {
                BlockPos pushToNeighbor = pipeBlockEntity.getBlockPos().relative(direction);
                if (pipeBlockEntity.getLevel().getBlockEntity(pushToNeighbor) instanceof PipeBlockEntity pipeBlockEntity2) {
                    if (pipeBlockEntity2.getPipe(this.slotPos()) instanceof EnergyPipe neighborPipe)
                        //check the next cell
                        neighborPipe.populatePushWrapper(pipeBlockEntity2, direction.getOpposite(), pushWrapper, distance + 1);
                } else  {
                    //edge of pipeline found, check for a neighboring tile entity
                    pushWrapper.addPushTarget(ModCapabilityManager.getIEnergyStorage(pipeBlockEntity.getLevel(), pushToNeighbor, direction.getOpposite()), this, distance, 0, getFrequency(direction));
                }
            }
        }
    }

    @Override
    public int canAccept(int amount) {
        return (int) Math.min(amount,(Config.ENERGY_THROUGHPUT.get()*getSpeedMultiplier()-amountPushed));
    }

}