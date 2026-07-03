package com.dannyandson.tinypipes.components.tiny;

import com.dannyandson.tinypipes.Config;
import com.dannyandson.tinypipes.caphandlers.ModCapabilityManager;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinyredstone.blocks.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

public class ItemPipe extends AbstractCapPipe<ResourceHandler<ItemResource>> {

    private boolean disabled = false;
    private int priority = 0;//TODO

    private static TextureAtlasSprite sprite = null;

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(com.dannyandson.tinypipes.components.RenderHelper.ITEM_PIPE_TEXTURE);
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

        if (ticks < ((Config.ITEM_THROUGHPUT.get()<4)?20/Config.ITEM_THROUGHPUT.get():5)) {
            ticks++;
            return false;
        }
        ticks = 0;

        super.tick(cellPos);

        //clear Push Wrappers
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

                ResourceHandler<ItemResource> iItemHandler = ModCapabilityManager.getItemHandler(cellPos.getPanelTile().getLevel(), neighborBlockPos, neighborSide);
                if (iItemHandler != null) {
                    boolean itemMoved = false;
                    int pullAmount = (Config.ITEM_THROUGHPUT.get() < 4) ? 1 : Config.ITEM_THROUGHPUT.get() / 4;
                    for (int slot = 0; slot < iItemHandler.size() && !itemMoved; slot++) {
                        ItemResource resource = iItemHandler.getResource(slot);
                        if (resource.isEmpty()) continue;
                        //simulate: how much of this slot can we actually pull?
                        int pullable;
                        try (Transaction sim = Transaction.openRoot()) {
                            pullable = iItemHandler.extract(slot, resource, pullAmount, sim);
                        }
                        if (pullable > 0) {
                            //an item that can be pulled exists; ask connected ItemPipe neighbors for a destination
                            //(the stack is only used for filter matching downstream)
                            ItemStack itemStack = resource.toStack(pullable);
                            PushWrapper<ResourceHandler<ItemResource>> pushWrapper = getPushWrapper(cellPos, itemStack);
                            //track how many items this pulling pipe is still allowed to move this operation
                            //so leftover capacity spills into the next-closest target instead of stopping
                            //only deliver to output sides sharing this pull side's channel
                            int pullFrequency = getFrequency(side);
                            int remaining = pullable;
                            for (PushWrapper.PushTarget<ResourceHandler<ItemResource>> pushTarget : pushWrapper.getSortedTargets()) {
                                if (remaining <= 0) break;
                                if (pushTarget.getFrequency() != pullFrequency) continue;
                                int pushLimit = pushTarget.getPipe().canAccept(remaining);
                                if (pushLimit > 0) {
                                    //grab capabilities and push
                                    ResourceHandler<ItemResource> iItemHandler2 = pushTarget.getTarget();
                                    if (iItemHandler2 != null && !iItemHandler2.equals(iItemHandler)) {
                                        //insert into the target and extract the same amount from the source atomically
                                        int pushed;
                                        try (Transaction move = Transaction.openRoot()) {
                                            pushed = iItemHandler2.insert(resource, pushLimit, move);
                                            if (pushed > 0) {
                                                iItemHandler.extract(slot, resource, pushed, move);
                                                move.commit();
                                            }
                                        }
                                        if (pushed>0) {
                                            pushTarget.getPipe().didPush(pushed);
                                            remaining -= pushed;
                                            itemMoved = true;
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

    private PushWrapper<ResourceHandler<ItemResource>> getPushWrapper(PanelCellPos cellPos, ItemStack itemStack) {
        this.pushWrapper = new PushWrapper<>();
        populatePushWrapper(cellPos, null, itemStack, this.pushWrapper, 0);
        return pushWrapper;
    }

    protected void populatePushWrapper(PanelCellPos cellPos, @Nullable Side side, ItemStack itemStack, PushWrapper<ResourceHandler<ItemResource>> pushWrapper, int distance) {
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
                    BlockPos panelBlockPos = cellPos.getPanelTile().getBlockPos();
                    Direction neighborSide =
                            (neighborBlockPos.relative(Direction.NORTH).equals(panelBlockPos)) ? Direction.NORTH :
                                    (neighborBlockPos.relative(Direction.EAST).equals(panelBlockPos)) ? Direction.EAST :
                                            (neighborBlockPos.relative(Direction.SOUTH).equals(panelBlockPos)) ? Direction.SOUTH :
                                                    (neighborBlockPos.relative(Direction.WEST).equals(panelBlockPos)) ? Direction.WEST :
                                                            (neighborBlockPos.relative(Direction.UP).equals(panelBlockPos)) ? Direction.UP :
                                                                    Direction.DOWN;

                    pushWrapper.addPushTarget(ModCapabilityManager.getItemHandler(cellPos.getPanelTile().getLevel(),neighborBlockPos,neighborSide), this, distance, priority, getFrequency(connectedSide));
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
        return Math.min(amount,(((Config.ITEM_THROUGHPUT.get()<4)?1:Config.ITEM_THROUGHPUT.get()/4)-amountPushed));
    }

}