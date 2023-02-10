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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

import static com.dannyandson.tinypipes.components.RenderHelper.ITEM_PIPE_TEXTURE;

public class ItemPipe extends AbstractCapFullPipe<IItemHandler>{

    private static TextureAtlasSprite sprite = null;
    private int priority = 0;//TODO

    @Override
    public TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(ITEM_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public int slotPos() {
        return 0;
    }

    @Override
    public boolean tick(PipeBlockEntity pipeBlockEntity) {
        if (disabled) return false;

        int itemThroughput = (int) (Config.ITEM_THROUGHPUT.get()*getSpeedMultiplier());

        if (ticks < ((itemThroughput < 4) ? 20 / itemThroughput : 5)) {
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

                IItemHandler iItemHandler = ModCapabilityManager.getItemHandler(pipeBlockEntity.getLevel(), neighborBlockPos, direction.getOpposite());
                if (iItemHandler != null) {
                    boolean itemMoved = false;
                    for (int slot = 0; slot < iItemHandler.getSlots() && !itemMoved; slot++) {
                        //if an item stack exists that can be pulled, ask connected ItemPipe neighbors if a destination exists
                        ItemStack itemStack = iItemHandler.extractItem(slot, (itemThroughput < 4) ? 1 : (int) (itemThroughput / 4), true);
                        if (!itemStack.isEmpty()) {
                            //we found a stack that can be extracted
                            //see if there's a place to put it
                            ItemStack itemStack2 = itemStack.copy();
                            PushWrapper<IItemHandler> pushWrapper = getPushWrapper(pipeBlockEntity, itemStack);
                            for (PushWrapper.PushTarget<IItemHandler> pushTarget : pushWrapper.getSortedTargets()) {
                                int pushLimit = pushTarget.getPipe().canAccept(itemStack2.getCount());
                                if (pushLimit > 0) {
                                    //grab capabilities and push
                                    IItemHandler iItemHandler2 = pushTarget.getTarget();
                                    if (iItemHandler2 != null && !iItemHandler2.equals(iItemHandler)) {
                                        ItemStack itemStack3 = itemStack2.copy();
                                        itemStack3.setCount(pushLimit);
                                        for (int pSlot = 0; pSlot < iItemHandler2.getSlots() && !itemStack3.isEmpty(); pSlot++) {
                                            itemStack3 = iItemHandler2.insertItem(pSlot, itemStack3, false);
                                        }
                                        int pushed = pushLimit - itemStack3.getCount();
                                        if (pushed > 0) {
                                            iItemHandler.extractItem(slot, pushed, false);
                                            pushTarget.getPipe().didPush(pushed);
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

    private PushWrapper<IItemHandler> getPushWrapper(PipeBlockEntity pipeBlockEntity, ItemStack itemStack) {
        this.pushWrapper = new PushWrapper<>();
        populatePushWrapper(pipeBlockEntity, null, itemStack, this.pushWrapper, 0);
        return pushWrapper;
    }

    protected void populatePushWrapper(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, ItemStack itemStack, PushWrapper<IItemHandler> pushWrapper, int distance) {
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
                    if (pipeBlockEntity2.getPipe(this.slotPos()) instanceof ItemPipe neighborPipe)
                    //check the next cell
                    neighborPipe.populatePushWrapper(pipeBlockEntity2, direction.getOpposite(), itemStack, pushWrapper, distance + 1);
                } else {
                    //edge of pipeline found, check for a neighboring tile entity
                    pushWrapper.addPushTarget(ModCapabilityManager.getItemHandler(pipeBlockEntity.getLevel(), pushToNeighbor, direction.getOpposite()), this, distance, priority);
                }
            }
        }
    }

    @Override
    public int canAccept(int amount) {
        return (int) Math.min(amount,(((Config.ITEM_THROUGHPUT.get()*getSpeedMultiplier()<4)?1:Config.ITEM_THROUGHPUT.get()*getSpeedMultiplier()/4)-amountPushed));
    }
}
