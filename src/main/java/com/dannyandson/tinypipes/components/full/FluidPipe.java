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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

import static com.dannyandson.tinypipes.components.RenderHelper.FLUID_PIPE_TEXTURE;

public class FluidPipe extends AbstractCapFullPipe<IFluidHandler>{

    private static TextureAtlasSprite sprite = null;
    private int priority = 0;//TODO

    @Override
    public TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(FLUID_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public int slotPos() {
        return 1;
    }

    @Override
    public boolean tick(PipeBlockEntity pipeBlockEntity) {
        if (disabled) return false;

        if (ticks < 5) {
            ticks++;
            return false;
        }
        ticks = 0;
        super.tick(pipeBlockEntity);

        //clear Push Wrappers
        pushIds.clear();
        pushWrapper = null;

        for (Direction direction : Direction.values()) {
            if (getPipeSideStatus(direction) == PipeConnectionState.PULLING) {
                //if set to pull, check for connected neighbor with item capabilities
                BlockPos neighborBlockPos = pipeBlockEntity.getBlockPos().relative(direction);

                IFluidHandler iFluidHandler = ModCapabilityManager.getIFluidHandler(pipeBlockEntity.getLevel(), neighborBlockPos, direction.getOpposite());
                if (iFluidHandler != null) {
                    boolean fluidMoved = false;
                    for (int tank = 0; tank < iFluidHandler.getTanks() && !fluidMoved; tank++) {
                        //if an item stack exists that can be pulled, ask connected ItemPipe neighbors if a destination exists
                        FluidStack fluidStack = iFluidHandler.getFluidInTank(tank);
                        if (!fluidStack.isEmpty()) {
                            //we found a stack that can be extracted
                            //see if there's a place to put it
                            FluidStack fluidStack2 = fluidStack.copy();
                            fluidStack2.setAmount(Math.min(fluidStack2.getAmount(), Config.FLUID_THROUGHPUT.get()/4));
                            PushWrapper<IFluidHandler> pushWrapper = getPushWrapper(pipeBlockEntity, fluidStack2);
                            for (PushWrapper.PushTarget<IFluidHandler> pushTarget : pushWrapper.getSortedTargets()) {
                                //grab capabilities and push
                                IFluidHandler iFluidHandler2 = pushTarget.getTarget();
                                if (iFluidHandler2 != null && ! iFluidHandler2.equals(iFluidHandler)) {
                                    int pushLimit = pushTarget.getPipe().canAccept(fluidStack.getAmount());
                                    if (pushLimit>0) {
                                        FluidStack fluidStack3 = fluidStack2.copy();
                                        fluidStack3.setAmount(pushLimit);
                                        int filled = iFluidHandler2.fill(fluidStack3, IFluidHandler.FluidAction.EXECUTE);
                                        if (filled > 0) {
                                            pushTarget.getPipe().didPush(filled);
                                            fluidStack2.setAmount(filled);
                                            iFluidHandler.drain(fluidStack2, IFluidHandler.FluidAction.EXECUTE);
                                            fluidMoved = true;
                                            break;
                                        }
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

    private PushWrapper<IFluidHandler> getPushWrapper(PipeBlockEntity pipeBlockEntity, FluidStack fluidStack) {
        this.pushWrapper = new PushWrapper<>();
        populatePushWrapper(pipeBlockEntity, null, fluidStack, this.pushWrapper, 0);
        return pushWrapper;
    }

    protected void populatePushWrapper(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, FluidStack fluidStack, PushWrapper<IFluidHandler> pushWrapper, int distance) {
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
                    if (pipeBlockEntity2.getPipe(this.slotPos()) instanceof FluidPipe neighborPipe)
                    //check the next cell
                    neighborPipe.populatePushWrapper(pipeBlockEntity2, direction.getOpposite(), fluidStack, pushWrapper, distance + 1);
                } else {
                    //edge of pipeline found, check for a neighboring tile entity
                    pushWrapper.addPushTarget(ModCapabilityManager.getIFluidHandler(pipeBlockEntity.getLevel(),pushToNeighbor,direction.getOpposite()), this, distance, priority);
                }
            }
        }
    }

    @Override
    public int canAccept(int amount) {
        return Math.min(amount,(Config.FLUID_THROUGHPUT.get()/4-amountPushed));
    }

}
