package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.dannyandson.tinypipes.components.RenderHelper.REDSTONE_PIPE_TEXTURE;

public class RedstonePipe extends AbstractFullPipe{

    private static final AtomicLong NEXT_ID = new AtomicLong(0);
    private static long getNextId() {
        return NEXT_ID.getAndIncrement();
    }

    //saved fields
    private Map<Integer,Integer> inputSignals = new HashMap<>();
    private Map<Integer,Integer> outputSignals = new HashMap<>();
    private final Map<Direction,Integer> frequencies = new HashMap<>();

    private boolean updateFlag = false;

    private static TextureAtlasSprite sprite = null;
    @Override
    public TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(REDSTONE_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public int slotPos() {
        return 3;
    }

    @CheckForNull
    public Integer getColor(Direction side) {
        if (getNeighborHasSamePipeType(side)==null || getNeighborHasSamePipeType(side)) return null;

        return (frequencies.containsKey(side)) ? DyeColor.byId(frequencies.get(side)).getMaterialColor().col : TinyPipes.defaultFrequency;
    }

    public void setColor(PipeBlockEntity pipeBlockEntity,Direction side, Integer color){
        // update of the pipe for the old frequency (update as if the input signal is 0)
        if (this.getPipeSideStatus(side) == PipeConnectionState.PULLING){
            int frequency = frequencies.getOrDefault(side, TinyPipes.defaultFrequency);
            int sint = outputSignals.getOrDefault(frequency, 0);
            onInputSignalChange(pipeBlockEntity, side, frequency, sint, 0,true, false);
        }

        if (getNeighborHasSamePipeType(side)!=null && !getNeighborHasSamePipeType(side))
            if (color == DyeColor.RED.getId())
                this.frequencies.remove(side);
            else
                this.frequencies.put(side,color);
    }

    @Override
    public boolean onPlace(PipeBlockEntity pipeBlockEntity, ItemStack itemStack) {
        super.onPlace(pipeBlockEntity, itemStack);
        return true; // on place update is done when the pipe is toggled (always done)
    }

    public void onToggle(PipeBlockEntity pipeBlockEntity, Direction direction) {
        PipeConnectionState currentState = getPipeSideStatus(direction);
        if (currentState == PipeConnectionState.DISABLED) {
            // the old state was pulling or enabled if we have a pipe,
            // so we need to do an update if the input signal is not zero if pulling,
            // or we need to do an update if the pipe nearby has a signal if enabled
            if (getNeighborHasSamePipeType(direction) != null && getNeighborHasSamePipeType(direction)) {
                // if the neighbor has the same pipe type, the last state is enabled, and it is as if an input signal is suddenly 0
                BlockPos neighbor = pipeBlockEntity.getBlockPos().relative(direction);
                Level level = pipeBlockEntity.getLevel();
                if (level.getBlockEntity(neighbor) instanceof PipeBlockEntity neighborPipeBE) {
                    // if the neighbor is a pipe,
                    RedstonePipe neighborPipe = (RedstonePipe) neighborPipeBE.getPipe(this.slotPos());
                    for (int frequency : TinyPipes.possibleFrequencies) {
                        int sp = neighborPipe.outputSignals.getOrDefault(frequency,0);
                        int sint = this.outputSignals.getOrDefault(frequency, 0);
                        // update of the toggled pipe as if input signal is 0
                        onInputSignalChange(pipeBlockEntity, direction, frequency, sint, 0,false,true);
                        // update of the neighbor pipe as if input signal is 0
                        neighborPipe.onInputSignalChange(neighborPipeBE, direction.getOpposite(), frequency, sp, 0,false,false);
                    }
                }
            } else {
                // if the neighbor does not have the same pipe type, the last state is pulling, and we need to update the input signal
                int frequency = frequencies.getOrDefault(direction, TinyPipes.defaultFrequency);
                int sinp = getInputSignal(pipeBlockEntity,direction);
                if (sinp != 0){
                    int sint = outputSignals.getOrDefault(frequency, 0);
                    onInputSignalChange(pipeBlockEntity, direction, frequency, sint, 0,true,true);
                }
            }
        } else if (currentState == PipeConnectionState.PULLING) {
            // the old state was enabled, so we need to update if the input signal is not zero as first if condition
            int frequency = frequencies.getOrDefault(direction, TinyPipes.defaultFrequency);
            int sinp = getInputSignal(pipeBlockEntity,direction);
            if (sinp != 0) {
                int sint = outputSignals.getOrDefault(frequency, 0);
                onInputSignalChange(pipeBlockEntity, direction, frequency, sint, sinp, true,false);
            }
            updateFlag = true; // we need to update the redstone component as the pipe is not enabled anymore (no signal will come from it)
        } else { //currentState == PipeConnectionState.ENABLED
            // the old state was disabled,
            // we need to update both pipe if the nearby block is a pipe
            BlockPos neighbor = pipeBlockEntity.getBlockPos().relative(direction);
            Level level = pipeBlockEntity.getLevel();
            // test here cannot be the same as the one in the if condition above
            // because the pipe can be just placed and no element exists in the list neighborHasSamePipeType
            if (level.getBlockEntity(neighbor) instanceof PipeBlockEntity neighborPipeBE) {
                // if the neighbor has the same pipe type, the last state is enabled,
                // and it is as if an input signal was not 0 for the pipe with the lower signal (for each frequency)
                if (neighborPipeBE.getPipe(this.slotPos()) instanceof RedstonePipe neighborPipe && neighborPipe.getPipeSideStatus(direction.getOpposite()) == PipeConnectionState.ENABLED) {
                    for (int frequency : TinyPipes.possibleFrequencies) {
                        int sp = neighborPipe.outputSignals.getOrDefault(frequency,0);
                        int sint = this.outputSignals.getOrDefault(frequency, 0);
                        if (sp == sint){
                            continue; // no signal to update
                        }
                        if (sp > sint) {
                            // if the neighbor pipe has a higher signal, we need to update the signal of the toggled pipe
                            onInputSignalChange(pipeBlockEntity, direction, frequency, sint, sp,false,false);
                        } else { // sp < sint
                            // if the toggled pipe has a higher signal, we need to update the input signal of the neighbor pipe
                            neighborPipe.onInputSignalChange(neighborPipeBE, direction.getOpposite(), frequency, sp, sint,false,false);
                        }
                    }
                }
            } else {
                // if the neighbor is not a pipe, we need to update the potential redstone component as the pipe can deliver a signal
                updateFlag = true;
            }
        }
    }

    public boolean onRemoveNeighbor(PipeBlockEntity pipeBlockEntity, Direction direction) {
        // on remove is called when a neighbor pipe is removed
        for (int frequency : TinyPipes.possibleFrequencies) {
            // if the neighbor pipe was enabled, we need to update the signal considering that the signal is now 0
            if (getPipeSideStatus(direction) == PipeConnectionState.ENABLED) {
                int sint = outputSignals.getOrDefault(frequency, 0);
                onInputSignalChange(pipeBlockEntity, direction, frequency, sint, 0,false, false);
            }
        }
        return false;
    }

    public boolean updateSignal(PipeBlockEntity pipeBlockEntity, @Nullable Direction direction) {
        if (direction == null) {
            // update all directions
            boolean changed = false;
            for (Direction dir : Direction.values()) {
                if (updateSignal(pipeBlockEntity, dir)) {
                    changed = true;
                }
            }
            return changed;
        }
        // update on input signal change
        boolean isPulling = getPipeSideStatus(direction) == PipeConnectionState.PULLING;
        if (isPulling){
            int frequency = frequencies.getOrDefault(direction, TinyPipes.defaultFrequency);
            // signal inside the pipe
            int sint = outputSignals.getOrDefault(frequency, 0);
            int sinp = getInputSignal(pipeBlockEntity,direction);
            onInputSignalChange(pipeBlockEntity, direction, frequency, sint, sinp,true,false);
        }
        return false;
    }

    public int getInputSignal(PipeBlockEntity pipeBlockEntity,Direction direction){
        BlockPos neighbor = pipeBlockEntity.getBlockPos().relative(direction);
        BlockState neighborState = pipeBlockEntity.getLevel().getBlockState(neighbor);
        return  (neighborState.canRedstoneConnectTo(pipeBlockEntity.getLevel(),pipeBlockEntity.getBlockPos(),direction.getOpposite()))
                ? pipeBlockEntity.getLevel().getSignal(neighbor,direction)
                : ((neighborState.isRedstoneConductor(pipeBlockEntity.getLevel(),neighbor))
                ?pipeBlockEntity.getLevel().getBestNeighborSignal(neighbor)
                :pipeBlockEntity.getLevel().getDirectSignal(neighbor,direction)
        );
    }

    public boolean onInputSignalChange(PipeBlockEntity pipeBlockEntity, @Nullable Direction direction,int frequency, int sint, int sinp,boolean updateInputList,boolean allowDisabled) {
        if (updateInputList){
            inputSignals.put(frequency, sinp); // update input signal
        }
        // update on input signal change
        if (sint > sinp) { // signal decreased
            Map<Integer, Integer> p = getNetworkRsOutput(pipeBlockEntity, null, getNextId());
            int spul = p.getOrDefault(frequency, 0);
            // spul is the signal that was pulled from the neighbor pulling
            // spul can only be equals to or less than sint
            if (spul < sint) {
                // if the pulled signal is less than the input signal, we need to update the output of the network with the pulled signal
                updateOutput(pipeBlockEntity, direction, frequency, spul, getNextId(),allowDisabled);
                return true; // signal changed
            } else {
                // if the pulled signal is equal to the input signal, we don't need to update the network
                return false; // no change
            }
        } else if (sint < sinp){ // signal increased
            updateOutput(pipeBlockEntity, direction, frequency, sinp, getNextId(),allowDisabled);
            return true; // signal changed
        }
        return false; // no change
    }

    public void updateOutput(PipeBlockEntity pipeBlockEntity, Direction direction, int frequency,int signal,long queryId,boolean allowDisabled) {
        if (pushIds.contains(queryId)) {
            // if we've already replied to this query, we don't need to do anything
            return;
        }
        //allowDisabled is used to allow the update of the output signal even if the pipe is not enabled : it is true only when toggle is done
        if (getPipeSideStatus(direction) == PipeConnectionState.DISABLED && !allowDisabled) {
            // if the pipe is not enabled, we don't need to do anything
            return;
        }
        pushIds.add(queryId);
        // update signal in the network in all directions (except the one that is the origin of the update)
        outputSignals.put(frequency, signal);
        updateFlag = true;
        pipeBlockEntity.sync();
        for (Direction dir : Direction.values()) {
            if (getPipeSideStatus(dir) == PipeConnectionState.ENABLED && dir != direction) {
                BlockPos neighbor = pipeBlockEntity.getBlockPos().relative(dir);
                if (pipeBlockEntity.getLevel().getBlockEntity(neighbor) instanceof PipeBlockEntity pipeBlockEntity2 && pipeBlockEntity2.hasPipe(RedstonePipe.class)) {
                    ((RedstonePipe)pipeBlockEntity2.getPipe(this.slotPos())).updateOutput(pipeBlockEntity2, dir.getOpposite(), frequency, signal,queryId,false);
                }
            }
        }
    }

    @Override
    public boolean neighborChanged(PipeBlockEntity pipeBlockEntity, @Nullable Direction direction) {
        super.neighborChanged(pipeBlockEntity,direction);
        return updateSignal(pipeBlockEntity,direction);
    }

    private Map<Integer,Integer> getNetworkRsOutput(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, long queryId) {
        //check if we've already replied to this query (to prevent infinite loops if there is a loop in the pipe network)
        if (pushIds.contains(queryId))
            return new HashMap<>();
        //check if we're connected to the querying component
        if (side != null && getPipeSideStatus(side) == PipeConnectionState.DISABLED)
            return new HashMap<>();

        //if checks pass, add id to list
        pushIds.add(queryId);

        Map<Integer, Integer> rsOutputs = new HashMap<>(this.inputSignals);

        for (Direction direction : Direction.values()) {
            if (getPipeSideStatus(direction) == PipeConnectionState.ENABLED) {
                BlockPos neighbor = pipeBlockEntity.getBlockPos().relative(direction);
                if (pipeBlockEntity.getLevel().getBlockEntity(neighbor) instanceof PipeBlockEntity pipeBlockEntity2 && pipeBlockEntity2.hasPipe(RedstonePipe.class)) {
                    Map<Integer, Integer> p = ((RedstonePipe)pipeBlockEntity2.getPipe(this.slotPos())).getNetworkRsOutput(pipeBlockEntity2, direction.getOpposite(), queryId);
                    for (Map.Entry<Integer, Integer> entry : p.entrySet()) {
                        if (!rsOutputs.containsKey(entry.getKey()) || entry.getValue() > rsOutputs.get(entry.getKey()))
                            rsOutputs.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        return rsOutputs;
    }

    @Override
    public boolean tick(PipeBlockEntity pipeBlockEntity) {
        if (updateFlag){
            updateFlag=false;
            super.tick(pipeBlockEntity);
            return true;
        }
        return super.tick(pipeBlockEntity);
    }

    //@Override
    public int getStrongRsOutput(Direction side) {
        return (this.getPipeSideStatus(side)== PipeConnectionState.ENABLED && (getNeighborHasSamePipeType(side)==null || !getNeighborHasSamePipeType(side)))
                ? outputSignals.getOrDefault(frequencies.getOrDefault(side, TinyPipes.defaultFrequency), 0)
                : 0;
    }

    //@Override
    public int getWeakRsOutput(Direction side) {
        return getStrongRsOutput(side);
    }

    @Override
    public void readNBT(CompoundTag compoundTag) {
        super.readNBT(compoundTag);
        if (compoundTag.contains("outputs")) {
            for (String frequency : compoundTag.getCompound("outputs").getAllKeys()) {
                outputSignals.put(Integer.parseInt(frequency), compoundTag.getCompound("outputs").getInt(frequency));
            }
        }
        if (compoundTag.contains("inputs")) {
            for (String frequency : compoundTag.getCompound("inputs").getAllKeys()) {
                inputSignals.put(Integer.parseInt(frequency), compoundTag.getCompound("inputs").getInt(frequency));
            }
        }
        if (compoundTag.contains("frequencies")) {
            for (String side : compoundTag.getCompound("frequencies").getAllKeys()) {
                frequencies.put(Direction.valueOf(side), compoundTag.getCompound("frequencies").getInt(side));
            }
        }
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();
        if (!inputSignals.isEmpty()) {
            CompoundTag inputNBT = new CompoundTag();
            for (Map.Entry<Integer, Integer> set : inputSignals.entrySet())
                if (set.getKey() != null)
                    inputNBT.putInt(set.getKey().toString(), set.getValue());
            nbt.put("inputs", inputNBT);
        }
        if (!outputSignals.isEmpty()) {
            CompoundTag outputNBT = new CompoundTag();
            for (Map.Entry<Integer, Integer> set : outputSignals.entrySet())
                if (set.getKey() != null)
                    outputNBT.putInt(set.getKey().toString(), set.getValue());
            nbt.put("outputs", outputNBT);
        }
        if (!frequencies.isEmpty()) {
            CompoundTag frequenciesNBT = new CompoundTag();
            for (Map.Entry<Direction, Integer> set : frequencies.entrySet())
                if (set.getKey() != null)
                    frequenciesNBT.putInt(set.getKey().name(), set.getValue());
            nbt.put("frequencies", frequenciesNBT);
        }

        return nbt;
    }


}
