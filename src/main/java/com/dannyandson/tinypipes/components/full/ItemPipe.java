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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

import static com.dannyandson.tinypipes.components.RenderHelper.ITEM_PIPE_TEXTURE;

public class ItemPipe extends AbstractCapFullPipe<ResourceHandler<ItemResource>>{

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
    protected boolean canAutoConnectTo(net.minecraft.world.level.Level level, BlockPos neighborPos, Direction direction) {
        return ModCapabilityManager.getItemHandler(level, neighborPos, direction.getOpposite()) != null;
    }

    @Override
    public Component getSpeedDescription() {
        int rate = (int) (Config.ITEM_THROUGHPUT.get() * getSpeedMultiplier());
        return Component.translatable("tinypipes.gui.pipe_config.speed.item", rate);
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

                ResourceHandler<ItemResource> iItemHandler = ModCapabilityManager.getItemHandler(pipeBlockEntity.getLevel(), neighborBlockPos, direction.getOpposite());
                if (iItemHandler != null) {
                    boolean itemMoved = false;
                    int pullAmount = (itemThroughput < 4) ? 1 : (int) (itemThroughput / 4);
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
                            PushWrapper<ResourceHandler<ItemResource>> pushWrapper = getPushWrapper(pipeBlockEntity, itemStack);
                            //track how many items this pulling pipe is still allowed to move this operation
                            //so leftover capacity spills into the next-closest target instead of stopping
                            //only deliver to output sides sharing this pull side's channel
                            int pullFrequency = getFrequency(direction);
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
                                        if (pushed > 0) {
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

    private PushWrapper<ResourceHandler<ItemResource>> getPushWrapper(PipeBlockEntity pipeBlockEntity, ItemStack itemStack) {
        this.pushWrapper = new PushWrapper<>();
        populatePushWrapper(pipeBlockEntity, null, itemStack, this.pushWrapper, 0);
        return pushWrapper;
    }

    protected void populatePushWrapper(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, ItemStack itemStack, PushWrapper<ResourceHandler<ItemResource>> pushWrapper, int distance) {
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
                    pushWrapper.addPushTarget(ModCapabilityManager.getItemHandler(pipeBlockEntity.getLevel(), pushToNeighbor, direction.getOpposite()), this, distance, priority, getFrequency(direction));
                }
            }
        }
    }

    @Override
    public int canAccept(int amount) {
        return (int) Math.min(amount,(((Config.ITEM_THROUGHPUT.get()*getSpeedMultiplier()<4)?1:Config.ITEM_THROUGHPUT.get()*getSpeedMultiplier()/4)-amountPushed));
    }
}