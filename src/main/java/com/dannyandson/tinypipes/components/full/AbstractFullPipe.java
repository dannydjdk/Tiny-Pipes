package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.IPipe;
import com.dannyandson.tinypipes.gui.PipeConfigGUI;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinyredstone.blocks.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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

    public boolean neighborChanged(PipeBlockEntity pipeBlockEntity) {
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
        if (player.level.isClientSide){
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

    public PipeConnectionState togglePipeSide(Direction direction) {
        if (sideStatusMap.get(direction) == PipeConnectionState.DISABLED || sideStatusMap.get(direction) == null)
            sideStatusMap.put(direction, PipeConnectionState.ENABLED);
        else if (sideStatusMap.get(direction) == PipeConnectionState.ENABLED && neighborHasSamePipeType.get(direction) != null && !neighborHasSamePipeType.get(direction))
            sideStatusMap.put(direction, PipeConnectionState.PULLING);
        else
            sideStatusMap.put(direction, PipeConnectionState.DISABLED);
        toggled = true;
        return sideStatusMap.get(direction);
    }

    public void setConnectionState(Direction direction, PipeConnectionState state) {
        sideStatusMap.put(direction, state);
        toggled = true;
    }

    public boolean tick(PipeBlockEntity pipeBlockEntity){
        if (toggled) {
            toggled=false;
            boolean change = neighborChanged(pipeBlockEntity);
            pipeBlockEntity.sync();
            if (change){
                pipeBlockEntity.getLevel().blockUpdated(pipeBlockEntity.getBlockPos(),pipeBlockEntity.getBlockState().getBlock());
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
                    for (String key : compoundTag.getCompound("sideStatus").getAllKeys()) {
                        Direction direction = Direction.valueOf(key);
                        PipeConnectionState status = PipeConnectionState.valueOf(compoundTag.getCompound("sideStatus").getString(key));
                        sideStatusMap.put(direction, status);
                    }
                }
                if (compoundTag.contains("neighborIsPipeCluster")){
                    for (String side : compoundTag.getCompound("neighborIsPipeCluster").getAllKeys()) {
                        neighborIsPipeCluster.put(Direction.valueOf(side), compoundTag.getCompound("neighborIsPipeCluster").getBoolean(side));
                    }
                }
                if (compoundTag.contains("neighborHasSamePipeType")){
                    for (String side : compoundTag.getCompound("neighborHasSamePipeType").getAllKeys()) {
                        neighborHasSamePipeType.put(Direction.valueOf(side), compoundTag.getCompound("neighborHasSamePipeType").getBoolean(side));
                    }
                }

            }catch (IllegalArgumentException exception){
                TinyPipes.LOGGER.error("Exception attempting to read pipe direction from NBT.", exception);
            }
    }
}
