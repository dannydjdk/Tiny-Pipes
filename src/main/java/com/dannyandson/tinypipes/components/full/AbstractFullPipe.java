package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.IPipe;
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
        for(Direction direction : Direction.values()){

            neighborIsPipeCluster.put(direction,false);
            neighborHasSamePipeType.put(direction,false);

            if (pipeBlockEntity.getLevel().getBlockEntity(pipeBlockEntity.getBlockPos().relative(direction)) instanceof PipeBlockEntity pipeBlockEntity2){
                if(pipeBlockEntity2.pipeCount()>1)
                    neighborIsPipeCluster.put(direction,true);
                 if(pipeBlockEntity2.hasPipe(this.getClass()))
                    neighborHasSamePipeType.put(direction,true);
            }
        }

        return false;
    }

    public Boolean getNeighborIsPipeCluster(Direction direction) {
        return neighborIsPipeCluster.get(direction);
    }

    public Boolean getNeighborHasSamePipeType(Direction direction) {
        return neighborHasSamePipeType.get(direction);
    }

    protected int getColor() {
        return 0xFFFFFFFF;
    }

    public boolean openGUI(Player player){return false;}

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
            return neighborChanged(pipeBlockEntity);
        }
        return false;
    }

    public CompoundTag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        for (Direction direction: sideStatusMap.keySet()){
            nbt.putString(direction.name(), sideStatusMap.get(direction).name());
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
        for (String key : compoundTag.getAllKeys()){
            try {
                Direction direction = Direction.valueOf(key);
                PipeConnectionState status = PipeConnectionState.valueOf(compoundTag.getString(key));
                sideStatusMap.put(direction, status);
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
                TinyPipes.LOGGER.error("Exception attempting to read pipe direction from NBT " + key, exception);
            }
        }
    }
}
