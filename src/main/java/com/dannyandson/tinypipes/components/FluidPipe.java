package com.dannyandson.tinypipes.components;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinyredstone.blocks.PanelCellNeighbor;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.Side;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public class FluidPipe  extends AbstractTinyPipe {

    private boolean disabled = false;
    private int priority = 0;//TODO

    public static final ResourceLocation FLUID_PIPE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/fluid_pipe");
    private static TextureAtlasSprite sprite = null;

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(FLUID_PIPE_TEXTURE);
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

        return false;
    }

    @Override
    public boolean isIndependentState() {
        return false;
    }

    @Override
    public boolean tick(PanelCellPos cellPos) {
        if (disabled) return false;

        if (ticks < 8) {
            ticks++;
            return false;
        }
        ticks = 0;

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

                IFluidHandler iFluidHandler = ModCapabilityManager.getIFluidHandler(cellPos.getPanelTile().getLevel(), neighborBlockPos, neighborSide);
                if (iFluidHandler != null) {
                    boolean fluidMoved = false;
                    for (int tank = 0; tank < iFluidHandler.getTanks() && !fluidMoved; tank++) {
                        //if an item stack exists that can be pulled, ask connected ItemPipe neighbors if a destination exists
                        FluidStack fluidStack = iFluidHandler.getFluidInTank(tank);
                        if (!fluidStack.isEmpty()) {
                            //we found a stack that can be extracted
                            //see if there's a place to put it
                            FluidStack fluidStack2 = fluidStack.copy();
                            fluidStack2.setAmount(Math.min(fluidStack2.getAmount(), 50));
                            PushWrapper pushWrapper = getPushWrapper(cellPos, fluidStack2);
                            for (PushWrapper.PushBlockEntity pushBlockEntity : pushWrapper.getSortedBlockEntities()) {
                                //grab capabilities and push
                                IFluidHandler iFluidHandler2 = pushBlockEntity.getIFluidHandler();
                                if (iFluidHandler2 != null) {
                                    int filled = iFluidHandler2.fill(fluidStack2, IFluidHandler.FluidAction.EXECUTE);
                                    if (filled > 0) {
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
        return false;
    }

    private PushWrapper getPushWrapper(PanelCellPos cellPos, FluidStack fluidStack) {
        this.pushWrapper = new PushWrapper();
        populatePushWrapper(cellPos, null, fluidStack, this.pushWrapper, 0);
        return pushWrapper;
    }

    protected void populatePushWrapper(PanelCellPos cellPos, @Nullable Side side, FluidStack fluidStack, PushWrapper pushWrapper, int distance) {
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

        //TODO check filter and return if it fails

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

                        pushWrapper.addBlockEntity(neighborBlockEntity, distance, priority, neighborSide);
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
