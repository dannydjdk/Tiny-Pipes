package com.dannyandson.tinypipes.blocks;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.components.full.PipeSide;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PipeBlockEntity extends BlockEntity {

    private final Map<Integer,AbstractFullPipe> pipes = new HashMap<>();
    private TextureAtlasSprite centerSprite=null;
    private BlockState camouflageBlockState=null;
    public static final ModelProperty<BlockState> CAMO_MODEL_PROPERTY = new ModelProperty<>();
    private Map<Direction,TextureAtlasSprite> camouflageSprites =new HashMap<>();

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.PIPE_BLOCK_ENTITY.get(), pos, state);
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

    /**
     * Add pipe from itemstack
     * @param itemStack Item stack containing pipe item
     * @return pipe if successfully added, null if not (pipe type already exists or item is not valid pipe)
     */
    @CheckForNull
    public AbstractFullPipe addPipe(ItemStack itemStack)
    {
        AbstractFullPipe pipe = Registry.getFullPipeFromItem(itemStack.getItem());
        if (pipe==null || slotUsed(pipe.slotPos())) return null;

        if (itemStack.hasTag())
            pipe.readNBT(itemStack.getTag().getCompound("pipe_data"));
        pipes.put(pipe.slotPos(),pipe);
        pipe.onPlace(this, itemStack);
        this.centerSprite=null;
        refresh=true;
        sync();
        return pipe;
    }

    public boolean removePipe(AbstractFullPipe pipe){
        if(pipes.remove(pipe.slotPos())!=null){
            this.centerSprite=null;
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
        sync();
    }

    public BlockState getCamouflageBlockState() {
        return camouflageBlockState;
    }

    /**
     * Loading and saving block entity data from disk and syncing to client
     */

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
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
     }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = new CompoundTag();
        this.saveAdditional(nbt);
        return nbt;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);

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
                this.camouflageBlockState = NbtUtils.readBlockState(nbt.getCompound("camouflage"));
            } catch (Exception exception) {
                TinyPipes.LOGGER.error("Exception attempting to read camouflage nbt.", exception);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
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
        double reachDistance = player.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue();
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
            //getPipes()[0].togglePipeSide(dir);
            //level.blockUpdated(this.worldPosition, this.getBlockState().getBlock());
        } else {
            Direction dir =
                    (x > 0.68) ? Direction.EAST :
                            (x < 0.32) ? Direction.WEST :
                                    (y > 0.68) ? Direction.UP :
                                            (y < 0.32) ? Direction.DOWN :
                                                    (z > 0.68) ? Direction.SOUTH :
                                                            Direction.NORTH;
            int slot = -1;
            if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                slot = (y > .5) ? (x > .5) ? 1 : 0 : (x > .5) ? 3 : 2;
            } else if (dir == Direction.EAST || dir == Direction.WEST) {
                slot = (y > .5) ? (z > .5) ? 0 : 1 : (z > .5) ? 2 : 3;
            } else { //up or down
                slot = (z > .5) ? (x > .5) ? 0 : 1 : (x > .5) ? 2 : 3;
            }
            if (this.slotUsed(slot)) {
                return new PipeSide(this, getPipe(slot), dir);
            }
        }
        return null;
    }

    public void tick() {
        boolean update = false;
        for (AbstractFullPipe pipe : pipes.values())
            if (pipe.tick(this)) update = true;

        if (refresh) {
            update=true;
            onNeighborChange();
            refresh=false;
        }
        if (update) {
            getLevel().blockUpdated(getBlockPos(), getBlockState().getBlock());
            sync();
        }

        if (pipeCount() == 0)
            level.removeBlock(worldPosition, false);
        //level.destroyBlock(worldPosition, false);
    }

    public void onNeighborChange() {
        for (AbstractFullPipe pipe : pipes.values())
            pipe.neighborChanged(this);
    }
}
