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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

import static com.dannyandson.tinypipes.components.RenderHelper.FLUID_PIPE_TEXTURE;

public class FluidPipe extends AbstractCapFullPipe<ResourceHandler<FluidResource>>{

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
    protected boolean canAutoConnectTo(net.minecraft.world.level.Level level, BlockPos neighborPos, Direction direction) {
        return ModCapabilityManager.getIFluidHandler(level, neighborPos, direction.getOpposite()) != null;
    }

    @Override
    public Component getSpeedDescription() {
        // tick() moves THROUGHPUT*mult/4 mB roughly 4x/sec, so the per-second rate is THROUGHPUT*mult
        int rate = (int) (Config.FLUID_THROUGHPUT.get() * getSpeedMultiplier());
        return Component.translatable("tinypipes.gui.pipe_config.speed.fluid", rate);
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

                ResourceHandler<FluidResource> iFluidHandler = ModCapabilityManager.getIFluidHandler(pipeBlockEntity.getLevel(), neighborBlockPos, direction.getOpposite());
                if (iFluidHandler != null) {
                    boolean fluidMoved = false;
                    for (int tank = 0; tank < iFluidHandler.size() && !fluidMoved; tank++) {
                        FluidResource resource = iFluidHandler.getResource(tank);
                        if (resource.isEmpty()) continue;
                        //cap this tick's pull at the per-tick throughput
                        int pullAmount = (int) Math.min(iFluidHandler.getAmountAsInt(tank), Config.FLUID_THROUGHPUT.get()*getSpeedMultiplier()/4);
                        if (pullAmount > 0) {
                            //a fluid that can be pulled exists; ask connected FluidPipe neighbors for a destination
                            //(the stack is only used for filter matching downstream)
                            FluidStack fluidStack = resource.toStack(pullAmount);
                            PushWrapper<ResourceHandler<FluidResource>> pushWrapper = getPushWrapper(pipeBlockEntity, fluidStack);
                            //track how much fluid this pulling pipe is still allowed to move this operation
                            //so leftover capacity spills into the next-closest target instead of stopping
                            //only deliver to output sides sharing this pull side's channel
                            int pullFrequency = getFrequency(direction);
                            int remaining = pullAmount;
                            for (PushWrapper.PushTarget<ResourceHandler<FluidResource>> pushTarget : pushWrapper.getSortedTargets()) {
                                if (remaining <= 0) break;
                                if (pushTarget.getFrequency() != pullFrequency) continue;
                                //grab capabilities and push
                                ResourceHandler<FluidResource> iFluidHandler2 = pushTarget.getTarget();
                                if (iFluidHandler2 != null && ! iFluidHandler2.equals(iFluidHandler)) {
                                    int pushLimit = pushTarget.getPipe().canAccept(remaining);
                                    if (pushLimit>0) {
                                        //fill the target and drain the same amount from the source atomically
                                        int filled;
                                        try (Transaction move = Transaction.openRoot()) {
                                            filled = iFluidHandler2.insert(resource, pushLimit, move);
                                            if (filled > 0) {
                                                iFluidHandler.extract(tank, resource, filled, move);
                                                move.commit();
                                            }
                                        }
                                        if (filled > 0) {
                                            pushTarget.getPipe().didPush(filled);
                                            remaining -= filled;
                                            fluidMoved = true;
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

    private PushWrapper<ResourceHandler<FluidResource>> getPushWrapper(PipeBlockEntity pipeBlockEntity, FluidStack fluidStack) {
        this.pushWrapper = new PushWrapper<>();
        populatePushWrapper(pipeBlockEntity, null, fluidStack, this.pushWrapper, 0);
        return pushWrapper;
    }

    protected void populatePushWrapper(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, FluidStack fluidStack, PushWrapper<ResourceHandler<FluidResource>> pushWrapper, int distance) {
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
                    pushWrapper.addPushTarget(ModCapabilityManager.getIFluidHandler(pipeBlockEntity.getLevel(),pushToNeighbor,direction.getOpposite()), this, distance, priority, getFrequency(direction));
                }
            }
        }
    }

    @Override
    public int canAccept(int amount) {
        return (int) Math.min(amount,(Config.FLUID_THROUGHPUT.get()*getSpeedMultiplier()/4-amountPushed));
    }

}