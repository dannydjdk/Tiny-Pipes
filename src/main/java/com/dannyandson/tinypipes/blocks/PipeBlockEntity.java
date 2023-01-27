package com.dannyandson.tinypipes.blocks;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PipeBlockEntity extends BlockEntity {

    private final Map<Integer,AbstractFullPipe> pipes = new HashMap<>();
    private TextureAtlasSprite centerSprite=null;

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

    public AbstractFullPipe getPipe(int slot)
    {
        return pipes.get(slot);
    }

    public AbstractFullPipe[] getPipes(){
        return pipes.values().toArray(new AbstractFullPipe[0]);
    }

    public int pipeCount(){return pipes.size();}

    /**
     * Add pipe from itemstack
     * @param itemStack Item stack containing pipe item
     * @return true if pipe was successfully added, false if not (for instance pipe type already exists or item is not valid pipe)
     */
    public boolean addPipe(ItemStack itemStack)
    {
        AbstractFullPipe pipe = Registry.getFullPipeFromItem(itemStack.getItem());
        if (pipe==null || slotUsed(pipe.slotPos())) return false;

        if (itemStack.hasTag())
            pipe.readNBT(itemStack.getTag().getCompound("pipe_data"));
        pipes.put(pipe.slotPos(),pipe);
        pipe.onPlace(this);
        this.centerSprite=null;
        return true;
    }

    public boolean removePipe(AbstractFullPipe pipe){
        if(pipes.remove(pipe.slotPos())!=null){
            this.centerSprite=null;
            return true;
        }
        return false;
    }

    /**
     * Loading and saving block entity data from disk and syncing to client
     */

    protected void sync() {
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
        for (String key : pipesData.getAllKeys()) {
            try {
                AbstractFullPipe pipe = (AbstractFullPipe) Class.forName(key).getConstructor().newInstance();
                pipe.readNBT(pipesData.getCompound(key));
                this.pipes.put(pipe.slotPos(),pipe);
            } catch (Exception exception) {
                TinyPipes.LOGGER.error("Exception attempting to construct Pipe object " + key, exception);
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

    private static TextureAtlasSprite pullSprite;
    public static TextureAtlasSprite getPullSprite() {
        if (pullSprite==null)
            pullSprite=RenderHelper.getSprite(ClientSetup.PIPE_PULL_TEXTURE);
        return pullSprite;
    }

    public void tick() {
        for (AbstractFullPipe pipe : pipes.values())
            pipe.tick(this);
    }

    public void onNeighborChange() {
        for (AbstractFullPipe pipe : pipes.values())
            pipe.neighborChanged(this);
    }
}
