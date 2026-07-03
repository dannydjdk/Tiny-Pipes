package com.dannyandson.tinypipes.components.tiny;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.caphandlers.ModCapabilityManager;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinyredstone.blocks.PanelCellNeighbor;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.Side;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

public class FluidPipe  extends AbstractCapPipe<ResourceHandler<FluidResource>> {

    private boolean disabled = false;
    private int priority = 0;//TODO

    private static TextureAtlasSprite sprite = null;

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(RenderHelper.FLUID_PIPE_TEXTURE);
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
    public boolean isIndependentState() {
        return false;
    }

    @Override
    public boolean tick(PanelCellPos cellPos) {
        if (disabled) return false;

        if (ticks < 5) {
            ticks++;
            return false;
        }
        ticks = 0;
        super.tick(cellPos);

        //clear Push Wrappers
        pushIds.clear();
        pushWrapper = null;

        for (Side side : pullSides) {
            //if set to pull, check for connected neighbor with fluid capabilities
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

                ResourceHandler<FluidResource> iFluidHandler = ModCapabilityManager.getIFluidHandler(cellPos.getPanelTile().getLevel(), neighborBlockPos, neighborSide);
                if (iFluidHandler != null) {
                    boolean fluidMoved = false;
                    for (int tank = 0; tank < iFluidHandler.size() && !fluidMoved; tank++) {
                        FluidResource resource = iFluidHandler.getResource(tank);
                        if (resource.isEmpty()) continue;
                        //cap this tick's pull at the per-tick throughput
                        int pullAmount = Math.min(iFluidHandler.getAmountAsInt(tank), Config.FLUID_THROUGHPUT.get()/4);
                        if (pullAmount > 0) {
                            //a fluid that can be pulled exists; ask connected FluidPipe neighbors for a destination
                            //(the stack is only used for filter matching downstream)
                            FluidStack fluidStack = resource.toStack(pullAmount);
                            PushWrapper<ResourceHandler<FluidResource>> pushWrapper = getPushWrapper(cellPos, fluidStack);
                            //track how much fluid this pulling pipe is still allowed to move this operation
                            //so leftover capacity spills into the next-closest target instead of stopping
                            //only deliver to output sides sharing this pull side's channel
                            int pullFrequency = getFrequency(side);
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

    private PushWrapper<ResourceHandler<FluidResource>> getPushWrapper(PanelCellPos cellPos, FluidStack fluidStack) {
        this.pushWrapper = new PushWrapper<>();
        populatePushWrapper(cellPos, null, fluidStack, this.pushWrapper, 0);
        return pushWrapper;
    }

    protected void populatePushWrapper(PanelCellPos cellPos, @Nullable Side side, FluidStack fluidStack, PushWrapper<ResourceHandler<FluidResource>> pushWrapper, int distance) {
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
                if (pushToNeighbor != null && pushToNeighbor.getNeighborIPanelCell() instanceof FluidPipe neighborPipe) {
                    //check the next cell
                    neighborPipe.populatePushWrapper(pushToNeighbor.getCellPos(), pushToNeighbor.getNeighborsSide(), fluidStack, pushWrapper, distance + 1);
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

                    pushWrapper.addPushTarget(ModCapabilityManager.getIFluidHandler(cellPos.getPanelTile().getLevel(),neighborBlockPos,neighborSide), this, distance, priority, getFrequency(connectedSide));
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
        return Math.min(amount,(Config.FLUID_THROUGHPUT.get()/4-amountPushed));
    }

}