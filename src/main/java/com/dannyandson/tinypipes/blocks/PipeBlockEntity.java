package com.dannyandson.tinypipes.blocks;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.blocks.rendering.CachedPipeRenderer;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.components.full.PipeSide;
import com.dannyandson.tinypipes.components.full.RefinedStorageCablePipe;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PipeBlockEntity extends BlockEntity {

    private final Map<Integer,AbstractFullPipe> pipes = new HashMap<>();
    private TextureAtlasSprite centerSprite=null;
    private BlockState camouflageBlockState=null;
    private Map<Direction,TextureAtlasSprite> camouflageSprites =new HashMap<>();

    // Vertex caching for full-pipe rendering (client-side only, lazy-initialized)
    private Object cachedRenderer = null;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.PIPE_BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * Get the cached renderer for this block entity. Client-side only.
     * Lazy-initialized to avoid loading client rendering classes on the dedicated server.
     */
    public CachedPipeRenderer getCachedRenderer() {
        if (cachedRenderer == null) {
            cachedRenderer = new CachedPipeRenderer();
        }
        return (CachedPipeRenderer) cachedRenderer;
    }

    /**
     * Mark the render cache as dirty, forcing a rebuild on the next frame.
     * Must be called from any code path that changes visual state.
     * Safe to call on either side — no-ops on the server since the cache won't exist.
     */
    public void markRenderDirty() {
        if (cachedRenderer != null) {
            ((CachedPipeRenderer) cachedRenderer).markDirty();
        }
    }

    public boolean slotUsed(int slot){
        return pipes.get(slot)!=null;
    }

    public boolean hasPipe(Item item) {
        return hasPipe( Registry.getFullPipeClassFromItem(item) );
    }
    public boolean hasPipe(Class<? extends AbstractFullPipe> pipeClass) {
        if (pipeClass != null)
            for (AbstractFullPipe pipe : pipes.values())
                if (pipe.getClass() == pipeClass)
                    return true;
        return false;
    }

    @CheckForNull
    public AbstractFullPipe getPipe(int slot)
    {
        return pipes.get(slot);
    }

    public AbstractFullPipe[] getPipes(){
        return pipes.values().toArray(new AbstractFullPipe[0]);
    }

    public int pipeCount(){return pipes.size();}

    private boolean refresh = false;

    @CheckForNull
    public AbstractFullPipe addPipe(ItemStack itemStack)
    {
        AbstractFullPipe pipe = Registry.getFullPipeFromItem(itemStack.getItem());
        if (pipe==null || slotUsed(pipe.slotPos())) return null;

        // In 1.21.1 NBT is stored via components - check for legacy tag data
        CompoundTag tag = null;
        if (itemStack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            tag = itemStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
        }
        if (tag != null && tag.contains("pipe_data"))
            pipe.readNBT(tag.getCompound("pipe_data"));
        pipes.put(pipe.slotPos(),pipe);
        pipe.onPlace(this, itemStack);
        this.centerSprite=null;
        refresh=true;
        markRenderDirty();
        sync();
        return pipe;
    }

    public boolean removePipe(AbstractFullPipe pipe){
        if(pipes.remove(pipe.slotPos())!=null){
            // Notify the pipe it is being removed while its state is still valid (before any block removal below).
            pipe.onRemove(this);
            this.centerSprite=null;
            markRenderDirty();
            if (pipes.size()==0)
                level.removeBlock(worldPosition,false);
            else {
                refresh=true;
                sync();
                getLevel().updateNeighborsAt(getBlockPos(),getBlockState().getBlock());
            }
            return true;
        }
        return false;
    }

    public void setCamouflage(BlockState camouflageBlockState){
        this.camouflageBlockState=camouflageBlockState;
        camouflageSprites.clear();
        markRenderDirty();
        sync();
    }

    public BlockState getCamouflageBlockState() {
        return camouflageBlockState;
    }

    public void sync() {
        if (!level.isClientSide)
            this.level.sendBlockUpdated(worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        this.setChanged();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();
        this.saveAdditional(nbt, registries);
        return nbt;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);

        CompoundTag pipesData = nbt.getCompound("pipes");
        this.pipes.clear();
        for (String key : pipesData.getAllKeys()) {
            try {
                AbstractFullPipe pipe = (AbstractFullPipe) Class.forName(key).getConstructor().newInstance();
                pipe.readNBT(pipesData.getCompound(key));
                this.pipes.put(pipe.slotPos(),pipe);
            } catch (Exception exception) {
                TinyPipes.LOGGER.error("Exception attempting to construct Pipe object " + key, exception);
            }
        }
        if (nbt.contains("camouflage")){
            try {
                this.camouflageBlockState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK),nbt.getCompound("camouflage"));
            } catch (Exception exception) {
                TinyPipes.LOGGER.error("Exception attempting to read camouflage nbt.", exception);
            }
        }
        // Invalidate render cache on NBT load (world load or server->client sync)
        this.centerSprite = null;
        markRenderDirty();

        // Rotation consumption
        if (level != null && !level.isClientSide) {
            Rotation pendingRotation = PipeBlock.PENDING_ROTATION.get();
            if (pendingRotation != null) {
                PipeBlock.PENDING_ROTATION.remove();
                if (pendingRotation != Rotation.NONE) {
                    rotate(pendingRotation);
                    sync();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        CompoundTag pipeData = new CompoundTag();
        for (AbstractFullPipe pipe : this.pipes.values()) {
            pipeData.put(pipe.getClass().getCanonicalName(), pipe.writeNBT());
        }
        nbt.put("pipes", pipeData);
        if (camouflageBlockState!=null)
            nbt.put("camouflage",NbtUtils.writeBlockState(camouflageBlockState));
    }

    public static BlockHitResult getPlayerCollisionHitResult(Player player, Level level) {
        float xRotation = player.getXRot();
        float yRotation = player.getYRot();
        Vec3 eyePosition = player.getEyePosition();
        float v = -Mth.cos(-xRotation * ((float)Math.PI / 180F));
        float x = (Mth.sin(-yRotation * ((float)Math.PI / 180F) - (float)Math.PI)) * v;
        float y = Mth.sin(-xRotation * ((float)Math.PI / 180F));
        float z = (Mth.cos(-yRotation * ((float)Math.PI / 180F) - (float)Math.PI)) * v;
        double reachDistance = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Vec3 vec31 = eyePosition.add((double)x * reachDistance, (double)y * reachDistance, (double)z * reachDistance);
        return level.clip(new ClipContext(eyePosition, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
    }

    public TextureAtlasSprite getCenterSprite() {
        if (this.centerSprite==null) {
            if (pipeCount()==1)
                this.centerSprite = this.getPipes()[0].getSprite();
            else
                this.centerSprite = RenderHelper.getSprite(ClientSetup.PIPE_BUNDLE_TEXTURE);
        }
        return this.centerSprite;
    }

    public TextureAtlasSprite getCamouflageSprite(Direction direction){
        if (camouflageBlockState==null)
            return null;

        if(camouflageSprites.get(direction)==null)
            camouflageSprites.put(direction,RenderHelper.getSprite(camouflageBlockState,direction));
        return camouflageSprites.get(direction);
    }

    private static TextureAtlasSprite whitePipeSprite;
    public static TextureAtlasSprite getWhitePipeSprite() {
        if (whitePipeSprite==null)
            whitePipeSprite=RenderHelper.getSprite(ClientSetup.PIPE_TEXTURE);
        return whitePipeSprite;
    }

    private static TextureAtlasSprite pullSprite;
    public static TextureAtlasSprite getPullSprite() {
        if (pullSprite==null)
            pullSprite=RenderHelper.getSprite(ClientSetup.PIPE_PULL_TEXTURE);
        return pullSprite;
    }

    private static TextureAtlasSprite chevronSprite;
    public static TextureAtlasSprite getChevronSprite() {
        if (chevronSprite==null)
            chevronSprite=RenderHelper.getSprite(ClientSetup.CHEVRON_TEXTURE);
        return chevronSprite;
    }

    @CheckForNull
    public PipeSide getPipeAtHitVector(BlockHitResult hitResult) {
        Direction rayTraceDirection = hitResult.getDirection().getOpposite();
        Vec3 hitVec = hitResult.getLocation().add((double) rayTraceDirection.getStepX() * .001d, (double) rayTraceDirection.getStepY() * .001d, (double) rayTraceDirection.getStepZ() * .001d);
        double x = hitVec.x - this.worldPosition.getX(),
                y = hitVec.y - this.worldPosition.getY(),
                z = hitVec.z - this.worldPosition.getZ();
        if (pipes.size() == 1) {
            Direction dir =
                    (x > 0.5703125) ? Direction.EAST :
                            (x < 0.4296875) ? Direction.WEST :
                                    (y > 0.5703125) ? Direction.UP :
                                            (y < 0.4296875) ? Direction.DOWN :
                                                    (z > 0.5703125) ? Direction.SOUTH :
                                                            Direction.NORTH;
            return new PipeSide(this,getPipes()[0],dir);
        } else {
            Direction dir =
                    (x > 0.68) ? Direction.EAST :
                            (x < 0.32) ? Direction.WEST :
                                    (y > 0.68) ? Direction.UP :
                                            (y < 0.32) ? Direction.DOWN :
                                                    (z > 0.68) ? Direction.SOUTH :
                                                            Direction.NORTH;
            // Refined Storage cable runs through the center of each face. If the ray lands on the
            // center band (and a cable is present), select it instead of a 2x2 quadrant slot.
            if (slotUsed(RefinedStorageCablePipe.SLOT)) {
                double a, b;
                if (dir.getAxis() == Direction.Axis.X) { a = y; b = z; }
                else if (dir.getAxis() == Direction.Axis.Y) { a = x; b = z; }
                else { a = x; b = y; }
                if (a > 0.4296875 && a < 0.5703125 && b > 0.4296875 && b < 0.5703125)
                    return new PipeSide(this, getPipe(RefinedStorageCablePipe.SLOT), dir);
            }
            int slot = -1;
            if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                slot = (y > .5) ? (x > .5) ? 1 : 0 : (x > .5) ? 3 : 2;
            } else if (dir == Direction.EAST || dir == Direction.WEST) {
                slot = (y > .5) ? (z > .5) ? 0 : 1 : (z > .5) ? 2 : 3;
            } else {
                slot = (z > .5) ? (x > .5) ? 0 : 1 : (x > .5) ? 2 : 3;
            }
            if (this.slotUsed(slot)) {
                return new PipeSide(this, getPipe(slot), dir);
            }
        }
        return null;
    }

    public void tick() {
        if (level.isClientSide) return;
        boolean update = false;
        for (AbstractFullPipe pipe : pipes.values())
            if (pipe.tick(this)) update = true;

        if (refresh) {
            update=true;
            onNeighborChange(null);
            refresh=false;
        }
        if (update) {
            getLevel().blockUpdated(getBlockPos(), getBlockState().getBlock());
            sync();
        }

        if (pipeCount() == 0)
            level.removeBlock(worldPosition, false);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        // Notify each pipe its block entity has entered the world (placement or chunk load).
        for (AbstractFullPipe pipe : pipes.values())
            pipe.onLoad(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Notify each pipe its block entity has left the world (block removed or chunk unload).
        for (AbstractFullPipe pipe : pipes.values())
            pipe.onUnload(this);
        if (cachedRenderer != null) {
            ((CachedPipeRenderer) cachedRenderer).clear();
            cachedRenderer = null;
        }
    }

    public void onNeighborChange(@Nullable Direction direction) {
        for (AbstractFullPipe pipe : pipes.values())
            pipe.neighborChanged(this, direction);
    }

    /**
     * Apply a rotation to this block entity's direction-keyed internal state.
     * Rotates each contained pipe's per-side data, the camouflage {@link BlockState}
     * (if any), and invalidates render caches.
     *
     * <p>Called from {@link #loadAdditional} when a Sable assembly that contained
     * this block is disassembled at a different orientation than it was assembled at.
     * See {@link PipeBlock#PENDING_ROTATION} for the full flow.
     */
    public void rotate(Rotation rotation) {
        if (rotation == Rotation.NONE) return;
        for (AbstractFullPipe pipe : pipes.values()) {
            pipe.rotate(rotation);
        }
        if (camouflageBlockState != null) {
            camouflageBlockState = camouflageBlockState.rotate(rotation);
            camouflageSprites.clear();
        }
        this.centerSprite = null;
        markRenderDirty();
    }
}