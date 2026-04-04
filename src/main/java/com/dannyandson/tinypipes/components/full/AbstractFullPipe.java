package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.IPipe;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.gui.PipeConfigGUI;
import com.dannyandson.tinypipes.setup.ClientSetup;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractFullPipe implements IPipe {

    private final Map<Direction, PipeConnectionState> sideStatusMap = new HashMap<>();
    private final Map<Direction,Boolean> neighborIsPipeCluster = new HashMap<>();
    private final Map<Direction,Boolean> neighborHasSamePipeType = new HashMap<>();

    protected int ticks = 0;
    protected List<Long> pushIds = new ArrayList<>();
    private boolean toggled = false;

    private static TextureAtlasSprite sprite = null;
    public TextureAtlasSprite getSprite(){
        if (sprite==null)
            sprite = RenderHelper.getSprite(ClientSetup.PIPE_TEXTURE);
        return sprite;
    }

    public boolean onPlace(PipeBlockEntity pipeBlockEntity, ItemStack itemStack){
        return false;
    }

    /**
     * Called on pipe placement to automatically enable sides facing compatible neighbors.
     * Connects to adjacent PipeBlocks that contain the same pipe type (and enables the neighbor's side too),
     * and to non-pipe blocks that this pipe type can interface with.
     */
    public void autoConnectOnPlace(PipeBlockEntity pipeBlockEntity) {
        Level level = pipeBlockEntity.getLevel();
        BlockPos pos = pipeBlockEntity.getBlockPos();

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);

            if (level.getBlockEntity(neighborPos) instanceof PipeBlockEntity neighborBE) {
                AbstractFullPipe neighborPipe = neighborBE.getPipe(this.slotPos());
                if (neighborPipe != null) {
                    // Enable our side toward the matching neighbor pipe
                    setConnectionState(pipeBlockEntity, direction, PipeConnectionState.ENABLED);
                    // Enable the neighbor's side toward us if it is currently disabled
                    if (neighborPipe.getPipeSideStatus(direction.getOpposite()) == PipeConnectionState.DISABLED) {
                        neighborPipe.setConnectionState(neighborBE, direction.getOpposite(), PipeConnectionState.ENABLED);
                    }
                }
            } else if (canAutoConnectTo(level, neighborPos, direction)) {
                setConnectionState(pipeBlockEntity, direction, PipeConnectionState.ENABLED);
            }
        }
    }

    /**
     * Check whether this pipe type can interface with the non-pipe block at the given position.
     * Override in subclasses to check for appropriate capabilities or block properties.
     * @param level the world
     * @param neighborPos position of the adjacent block
     * @param direction direction from this pipe toward the neighbor
     * @return true if this pipe should auto-connect to the neighbor
     */
    protected boolean canAutoConnectTo(Level level, BlockPos neighborPos, Direction direction) {
        return false;
    }

    public boolean neighborChanged(PipeBlockEntity pipeBlockEntity, @Nullable Direction updateDirection) {
        boolean change = false;
        for(Direction direction : Direction.values()) {
            boolean pipeCluster = false;
            boolean matchingPipe = false;
            if (pipeBlockEntity.getLevel().getBlockEntity(pipeBlockEntity.getBlockPos().relative(direction)) instanceof PipeBlockEntity pipeBlockEntity2) {
                pipeCluster = pipeBlockEntity2.pipeCount() > 1;
                matchingPipe = pipeBlockEntity2.getPipe(this.slotPos())!=null;
            }
            if (neighborIsPipeCluster.get(direction) == null || neighborIsPipeCluster.get(direction) != pipeCluster) {
                neighborIsPipeCluster.put(direction, pipeCluster);
                change = true;
            }
            if (neighborHasSamePipeType.get(direction) == null || neighborHasSamePipeType.get(direction) != matchingPipe) {
                neighborHasSamePipeType.put(direction, matchingPipe);
                if (matchingPipe && sideStatusMap.get(direction)==PipeConnectionState.PULLING)
                    sideStatusMap.put(direction,PipeConnectionState.ENABLED);
                change = true;
            }

        }
        if (change) {
            pipeBlockEntity.sync();
        }
        return false;
    }

    public void onRemoveNeighbor(PipeBlockEntity pipeBlockEntity, Direction direction) {
        this.neighborChanged(pipeBlockEntity, direction);
    }

    public Boolean getNeighborIsPipeCluster(Direction direction) {
        return neighborIsPipeCluster.get(direction)!=null && neighborIsPipeCluster.get(direction);
    }

    public Boolean getNeighborHasSamePipeType(Direction direction) {
        return neighborHasSamePipeType.get(direction) != null && neighborHasSamePipeType.get(direction);
    }

    public int getColor() {
        return 0xFFFFFFFF;
    }

    public void openGUI(PipeBlockEntity pipeBlockEntity,Player player){
        if (player.level().isClientSide()){
            PipeConfigGUI.open(pipeBlockEntity,this);
        }
    }

    //TODO
    public CompoundTag getItemTag(){return new CompoundTag();}

    public abstract int slotPos();

    /**
     * get the status of a side of a pipe (DISABLED, ENABLED, PULLING)
     * @param direction side of pipe being queried
     * @return PipeSideStatus enum
     */
    public PipeConnectionState getPipeSideStatus(Direction direction) {
        PipeConnectionState status = sideStatusMap.get(direction);
        return (status == null) ? PipeConnectionState.DISABLED : status;
    }

    public PipeConnectionState togglePipeSide(PipeBlockEntity pipeBlockEntity, Direction direction) {
        PipeConnectionState state = getNextToggleState(direction);
        setConnectionState(pipeBlockEntity, direction, state);
        return state;
    }

    /**
     * Computes the next toggle state for a side without any side effects.
     * Safe to call on the client for GUI purposes.
     */
    public PipeConnectionState getNextToggleState(Direction direction) {
        if (sideStatusMap.get(direction) == PipeConnectionState.DISABLED || sideStatusMap.get(direction) == null)
            return PipeConnectionState.ENABLED;
        else if (sideStatusMap.get(direction) == PipeConnectionState.ENABLED && neighborHasSamePipeType.get(direction) != null && !neighborHasSamePipeType.get(direction))
            return PipeConnectionState.PULLING;
        else
            return PipeConnectionState.DISABLED;
    }

    /**
     * Computes the previous toggle state (reverse direction) without side effects.
     * Forward:  DISABLED → ENABLED → PULLING → DISABLED
     * Reverse:  DISABLED → PULLING → ENABLED → DISABLED
     */
    public PipeConnectionState getPrevToggleState(Direction direction) {
        PipeConnectionState current = sideStatusMap.get(direction);
        if (current == PipeConnectionState.DISABLED || current == null) {
            // DISABLED → PULLING if the neighbor is not the same pipe type, else ENABLED
            if (neighborHasSamePipeType.get(direction) != null && !neighborHasSamePipeType.get(direction))
                return PipeConnectionState.PULLING;
            else
                return PipeConnectionState.ENABLED;
        } else if (current == PipeConnectionState.PULLING)
            return PipeConnectionState.ENABLED;
        else
            return PipeConnectionState.DISABLED;
    }

    public void setConnectionState(PipeBlockEntity pipeBlockEntity, Direction direction, PipeConnectionState state) {
        sideStatusMap.put(direction, state);
        pipeBlockEntity.markRenderDirty();
        if (this instanceof RedstonePipe redstonePipe){
            redstonePipe.onToggle(pipeBlockEntity, direction);
        }else {
            toggled = true;
        }
    }

    public boolean tick(PipeBlockEntity pipeBlockEntity){
        if (toggled) {
            toggled=false;
            boolean change = neighborChanged(pipeBlockEntity, null);
            pipeBlockEntity.sync();
            if (change){
                pipeBlockEntity.getLevel().updateNeighborsAt(pipeBlockEntity.getBlockPos(),pipeBlockEntity.getBlockState().getBlock());
            }
            pipeBlockEntity.getLevel().updateNeighborsAt(pipeBlockEntity.getBlockPos(),pipeBlockEntity.getBlockState().getBlock());
            return change;
        }
        return false;
    }

    public CompoundTag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (!sideStatusMap.isEmpty()) {
            CompoundTag sideStatusNbt = new CompoundTag();
            for (Direction direction : sideStatusMap.keySet()) {
                sideStatusNbt.putString(direction.name(), sideStatusMap.get(direction).name());
            }
            nbt.put("sideStatus", sideStatusNbt);
        }
        if (!neighborIsPipeCluster.isEmpty()) {
            CompoundTag neighborInfoNBT = new CompoundTag();
            for (Map.Entry<Direction, Boolean> set : neighborIsPipeCluster.entrySet())
                if (set.getKey() != null)
                    neighborInfoNBT.putBoolean(set.getKey().name(), set.getValue());
            nbt.put("neighborIsPipeCluster", neighborInfoNBT);
        }
        if (!neighborHasSamePipeType.isEmpty()) {
            CompoundTag neighborInfoNBT = new CompoundTag();
            for (Map.Entry<Direction, Boolean> set : neighborHasSamePipeType.entrySet())
                if (set.getKey() != null)
                    neighborInfoNBT.putBoolean(set.getKey().name(), set.getValue());
            nbt.put("neighborHasSamePipeType", neighborInfoNBT);
        }

        return nbt;
    }

    public void readNBT(CompoundTag compoundTag) {
        try {
            if (compoundTag.contains("sideStatus")) {
                CompoundTag sideStatusTag = compoundTag.getCompound("sideStatus").orElseGet(CompoundTag::new);
                for (String key : sideStatusTag.keySet()) {
                    Direction direction = Direction.valueOf(key);
                    PipeConnectionState status = PipeConnectionState.valueOf(sideStatusTag.getStringOr(key, "DISABLED"));
                    sideStatusMap.put(direction, status);
                }
            }
            if (compoundTag.contains("neighborIsPipeCluster")){
                CompoundTag neighborTag = compoundTag.getCompound("neighborIsPipeCluster").orElseGet(CompoundTag::new);
                for (String side : neighborTag.keySet()) {
                    neighborIsPipeCluster.put(Direction.valueOf(side), neighborTag.getBooleanOr(side, false));
                }
            }
            if (compoundTag.contains("neighborHasSamePipeType")){
                CompoundTag neighborTag = compoundTag.getCompound("neighborHasSamePipeType").orElseGet(CompoundTag::new);
                for (String side : neighborTag.keySet()) {
                    neighborHasSamePipeType.put(Direction.valueOf(side), neighborTag.getBooleanOr(side, false));
                }
            }

        }catch (IllegalArgumentException exception){
            TinyPipes.LOGGER.error("Exception attempting to read pipe direction from NBT.", exception);
        }
    }
}