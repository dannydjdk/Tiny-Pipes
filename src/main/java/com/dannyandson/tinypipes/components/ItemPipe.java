package com.dannyandson.tinypipes.components;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinyredstone.blocks.*;
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
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public class ItemPipe extends AbstractTinyPipe {

    private boolean disabled = false;
    private int priority = 0;//TODO

    public static final ResourceLocation ITEM_PIPE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/item_pipe");
    private static TextureAtlasSprite sprite = null;

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(ITEM_PIPE_TEXTURE);
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
            //if set to pull, check for connected neighbor with item capabilities
            PanelCellNeighbor extractNeighbor = cellPos.getNeighbor(side);
            BlockPos neighborBlockPos = (extractNeighbor==null)?null:extractNeighbor.getBlockPos();

            if (neighborBlockPos != null) {
                BlockEntity neighborBlockEntity = cellPos.getPanelTile().getLevel().getBlockEntity(neighborBlockPos);
                if (neighborBlockEntity != null) {
                    Capability<IItemHandler> iItemHandlerCapability = CapabilityManager.get(new CapabilityToken<>() {
                    });
                    BlockPos panelBlockPos = cellPos.getPanelTile().getBlockPos();
                    Direction neighborSide =
                            (neighborBlockPos.relative(Direction.NORTH).equals(panelBlockPos)) ? Direction.NORTH :
                                    (neighborBlockPos.relative(Direction.EAST).equals(panelBlockPos)) ? Direction.EAST :
                                            (neighborBlockPos.relative(Direction.SOUTH).equals(panelBlockPos)) ? Direction.SOUTH :
                                                    (neighborBlockPos.relative(Direction.WEST).equals(panelBlockPos)) ? Direction.WEST :
                                                            (neighborBlockPos.relative(Direction.UP).equals(panelBlockPos)) ? Direction.UP :
                                                                    Direction.DOWN;

                    IItemHandler iItemHandler = neighborBlockEntity.getCapability(iItemHandlerCapability, neighborSide).orElse(null);
                    if (iItemHandler != null) {
                        boolean itemMoved = false;
                        for (int slot = 0; slot < iItemHandler.getSlots() && !itemMoved; slot++) {
                            //if an item stack exists that can be pulled, ask connected ItemPipe neighbors if a destination exists
                            ItemStack itemStack = iItemHandler.extractItem(slot, 1, true);
                            if (itemStack != null && !itemStack.isEmpty()) {
                                //we found a stack that can be extracted
                                //see if there's a place to put it
                                ItemStack itemStack2 = itemStack.copy();
                                PushWrapper pushWrapper = getPushWrapper(cellPos, itemStack);
                                for (PushWrapper.PushBlockEntity pushBlockEntity : pushWrapper.getSortedBlockEntities()) {
                                    //grab capabilities and push
                                    IItemHandler iItemHandler2 = pushBlockEntity.getIItemHandler();
                                    if (iItemHandler2 != null) {
                                        for (int pSlot = 0; pSlot < iItemHandler2.getSlots() && !itemStack2.isEmpty() ; pSlot++) {
                                            itemStack2 = iItemHandler2.insertItem(pSlot, itemStack2, false);
                                        }
                                        if (itemStack2.getCount()< itemStack.getCount()) {
                                            iItemHandler.extractItem(slot, itemStack.getCount() - itemStack2.getCount(), false);
                                            itemMoved = true;
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

    private PushWrapper getPushWrapper(PanelCellPos cellPos, ItemStack itemStack) {
        if (pushWrapper == null) {
            this.pushWrapper = new PushWrapper();
            populatePushWrapper(cellPos, null, itemStack, this.pushWrapper, 0);
        }
        return pushWrapper;
    }

    protected void populatePushWrapper(PanelCellPos cellPos, @Nullable Side side, ItemStack itemStack, PushWrapper pushWrapper, int distance) {
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
                if (pushToNeighbor != null && pushToNeighbor.getNeighborIPanelCell() instanceof ItemPipe neighborPipe) {
                    //check the next cell
                    neighborPipe.populatePushWrapper(pushToNeighbor.getCellPos(), pushToNeighbor.getNeighborsSide(), itemStack, pushWrapper, distance + 1);
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
