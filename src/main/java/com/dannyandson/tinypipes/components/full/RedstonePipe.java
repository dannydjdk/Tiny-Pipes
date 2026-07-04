package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;
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
    private final Map<Integer,Integer> outputSignals = new HashMap<>();
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

    @Override
    protected boolean canAutoConnectTo(Level level, BlockPos neighborPos, Direction direction) {
        BlockState neighborState = level.getBlockState(neighborPos);
        return neighborState.getBlock().canConnectRedstone(neighborState, level, neighborPos, direction.getOpposite());
    }

    @Nullable
    public Integer getColor(Direction side) {
        if (getNeighborHasSamePipeType(side)==null || getNeighborHasSamePipeType(side)) return null;

        return (frequencies.containsKey(side)) ? DyeColor.byId(frequencies.get(side)).getMapColor().col : TinyPipes.defaultFrequency;
    }

    public void setColor(PipeBlockEntity pipeBlockEntity, Direction side, Integer color){
        // update of the pipe for the old frequency (update as if the input signal is 0)
        int oldFrequency = frequencies.getOrDefault(side, TinyPipes.defaultFrequency);
        if (getNeighborHasSamePipeType(side)!=null && !getNeighborHasSamePipeType(side))
            if (color == DyeColor.RED.getId())
                this.frequencies.remove(side);
            else
                this.frequencies.put(side,color);
        int newFrequency = frequencies.getOrDefault(side, TinyPipes.defaultFrequency);
        if (oldFrequency == newFrequency) {
            // no change in frequency, so no need to update the output signal
            return;
        }

        if (this.getPipeSideStatus(side) == PipeConnectionState.PULLING){
            int sintOldFreq = outputSignals.getOrDefault(oldFrequency, 0);
            int sintNewFreq = outputSignals.getOrDefault(newFrequency, 0);
            int sinp = getInputSignal(pipeBlockEntity,side);
            // update of the pipe for the old frequency from sinp to 0
            onInputSignalChange(pipeBlockEntity, side, oldFrequency, sintOldFreq, 0,true, false);
            // update of the pipe for the new frequency from 0 to sinp
            onInputSignalChange(pipeBlockEntity, side, newFrequency, sintNewFreq, sinp,true, false);
        }
        updateFlag = true;
    }

    @Override
    public boolean onPlace(PipeBlockEntity pipeBlockEntity, ItemStack itemStack) {
        super.onPlace(pipeBlockEntity, itemStack);
        return true; // on place update is done when the pipe is toggled (always done)
    }

    public void onToggle(PipeBlockEntity pipeBlockEntity, Direction direction) {
        PipeConnectionState currentState = getPipeSideStatus(direction);
        if (currentState == PipeConnectionState.DISABLED) {
            if (getNeighborHasSamePipeType(direction) != null && getNeighborHasSamePipeType(direction)) {
                BlockPos neighbor = pipeBlockEntity.getBlockPos().relative(direction);
                Level level = pipeBlockEntity.getLevel();
                if (level.getBlockEntity(neighbor) instanceof PipeBlockEntity neighborPipeBE) {
                    RedstonePipe neighborPipe = (RedstonePipe) neighborPipeBE.getPipe(this.slotPos());
                    for (int frequency : TinyPipes.possibleFrequencies) {
                        int sp = neighborPipe.outputSignals.getOrDefault(frequency,0);
                        int sint = this.outputSignals.getOrDefault(frequency, 0);
                        onInputSignalChange(pipeBlockEntity, direction, frequency, sint, 0,false,true);
                        neighborPipe.onInputSignalChange(neighborPipeBE, direction.getOpposite(), frequency, sp, 0,false,false);
                    }
                }
            } else {
                int frequency = frequencies.getOrDefault(direction, TinyPipes.defaultFrequency);
                int sinp = getInputSignal(pipeBlockEntity,direction);
                if (sinp != 0){
                    int sint = outputSignals.getOrDefault(frequency, 0);
                    onInputSignalChange(pipeBlockEntity, direction, frequency, sint, 0,true,true);
                }
            }
        } else if (currentState == PipeConnectionState.PULLING) {
            int frequency = frequencies.getOrDefault(direction, TinyPipes.defaultFrequency);
            int sinp = getInputSignal(pipeBlockEntity,direction);
            if (sinp != 0) {
                int sint = outputSignals.getOrDefault(frequency, 0);
                onInputSignalChange(pipeBlockEntity, direction, frequency, sint, sinp, true,false);
            }
            updateFlag = true;
        } else { //currentState == PipeConnectionState.ENABLED
            BlockPos neighbor = pipeBlockEntity.getBlockPos().relative(direction);
            Level level = pipeBlockEntity.getLevel();
            if (level.getBlockEntity(neighbor) instanceof PipeBlockEntity neighborPipeBE) {
                if (neighborPipeBE.getPipe(this.slotPos()) instanceof RedstonePipe neighborPipe && neighborPipe.getPipeSideStatus(direction.getOpposite()) == PipeConnectionState.ENABLED) {
                    for (int frequency : TinyPipes.possibleFrequencies) {
                        int sp = neighborPipe.outputSignals.getOrDefault(frequency,0);
                        int sint = this.outputSignals.getOrDefault(frequency, 0);
                        if (sp == sint){
                            continue;
                        }
                        if (sp > sint) {
                            onInputSignalChange(pipeBlockEntity, direction, frequency, sint, sp,false,false);
                        } else {
                            neighborPipe.onInputSignalChange(neighborPipeBE, direction.getOpposite(), frequency, sp, sint,false,false);
                        }
                    }
                }
            } else {
                updateFlag = true;
            }
        }
    }

    @Override
    public void onRemoveNeighbor(PipeBlockEntity pipeBlockEntity, Direction direction) {
        for (int frequency : TinyPipes.possibleFrequencies) {
            if (getPipeSideStatus(direction) == PipeConnectionState.ENABLED) {
                int sint = outputSignals.getOrDefault(frequency, 0);
                onInputSignalChange(pipeBlockEntity, direction, frequency, sint, 0,false, false);
            }
        }
        super.onRemoveNeighbor(pipeBlockEntity,direction);
    }

    @Override
    public void rotate(Rotation rotation) {
        super.rotate(rotation);
        if (rotation == Rotation.NONE) return;
        rotateMap(frequencies, rotation);
    }

    @Override
    public boolean neighborChanged(PipeBlockEntity pipeBlockEntity, @Nullable Direction direction) {
        super.neighborChanged(pipeBlockEntity, direction);
        return updateSignal(pipeBlockEntity, direction);
    }

    public boolean updateSignal(PipeBlockEntity pipeBlockEntity, @Nullable Direction direction) {
        if (direction == null) {
            boolean changed = false;
            for (Direction dir : Direction.values()) {
                if (updateSignal(pipeBlockEntity, dir)) {
                    changed = true;
                }
            }
            return changed;
        }
        boolean isPulling = getPipeSideStatus(direction) == PipeConnectionState.PULLING;
        if (isPulling){
            int frequency = frequencies.getOrDefault(direction, TinyPipes.defaultFrequency);
            int sint = outputSignals.getOrDefault(frequency, 0);
            int sinp = getInputSignal(pipeBlockEntity,direction);
            onInputSignalChange(pipeBlockEntity, direction, frequency, sint, sinp,true,false);
        }
        return false;
    }

    public int getInputSignal(PipeBlockEntity pipeBlockEntity, Direction direction){
        BlockPos neighbor = pipeBlockEntity.getBlockPos().relative(direction);
        BlockState neighborState = pipeBlockEntity.getLevel().getBlockState(neighbor);
        return  (neighborState.getBlock().canConnectRedstone(neighborState, pipeBlockEntity.getLevel(),neighbor,direction.getOpposite()))
                ? pipeBlockEntity.getLevel().getSignal(neighbor,direction)
                : ((neighborState.isRedstoneConductor(pipeBlockEntity.getLevel(),neighbor))
                ?pipeBlockEntity.getLevel().getBestNeighborSignal(neighbor)
                :pipeBlockEntity.getLevel().getDirectSignal(neighbor,direction)
        );
    }

    private boolean updateInputSignal(PipeBlockEntity pipeBlockEntity) {
        Map<Integer, Integer> signals = new HashMap<>();

        for (Direction direction : Direction.values()) {
            if (getPipeSideStatus(direction) != PipeConnectionState.PULLING) {
                continue;
            }
            BlockPos pos = pipeBlockEntity.getBlockPos().relative(direction);
            if (pipeBlockEntity.getLevel().getBlockEntity(pos) instanceof PipeBlockEntity) {
                continue;
            }
            int signal = getInputSignal(pipeBlockEntity,direction);
            int frequency = frequencies.getOrDefault(direction, TinyPipes.defaultFrequency);
            if (!signals.containsKey(frequency) || signal > signals.get(frequency))
                signals.put(frequency, signal);
        }

        if (!signals.equals(inputSignals)) {
            inputSignals = signals;
            return true;
        }
        return false;
    }

    public boolean onInputSignalChange(PipeBlockEntity pipeBlockEntity, @Nullable Direction direction,int frequency, int sint, int sinp,boolean updateInputList,boolean allowDisabled) {
        if (updateInputList){
            updateInputSignal(pipeBlockEntity);
        }
        if (sint > sinp) {
            Map<Integer, Integer> p = getNetworkRsOutput(pipeBlockEntity, null, getNextId());
            int spul = p.getOrDefault(frequency, 0);
            if (spul < sint) {
                updateOutput(pipeBlockEntity, direction, frequency, spul, getNextId(),allowDisabled);
                return true;
            } else {
                return false;
            }
        } else if (sint < sinp){
            updateOutput(pipeBlockEntity, direction, frequency, sinp, getNextId(),allowDisabled);
            return true;
        }
        return false;
    }

    public void updateOutput(PipeBlockEntity pipeBlockEntity, Direction direction, int frequency,int signal,long queryId,boolean allowDisabled) {
        if (pushIds.contains(queryId)) {
            return;
        }
        if (getPipeSideStatus(direction) == PipeConnectionState.DISABLED && !allowDisabled) {
            return;
        }
        pushIds.add(queryId);
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

    private Map<Integer,Integer> getNetworkRsOutput(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, long queryId) {
        if (pushIds.contains(queryId))
            return new HashMap<>();
        if (side != null && getPipeSideStatus(side) == PipeConnectionState.DISABLED)
            return new HashMap<>();

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
    public void appendOverlayInfo(List<Component> lines, PipeBlockEntity pipeBlockEntity, Direction side) {
        super.appendOverlayInfo(lines, pipeBlockEntity, side);
        // channel of the looked-at end, shown only on I/O sides dyed off the default (red)
        if ((getNeighborHasSamePipeType(side) == null || !getNeighborHasSamePipeType(side)) && frequencies.containsKey(side))
            lines.add(Component.translatable("tinypipes.overlay.channel", DyeColor.byId(frequencies.get(side)).getName()));
        // per-frequency output signal carried by this pipe (mirrors the tiny redstone pipe overlay)
        for (Map.Entry<Integer, Integer> entry : outputSignals.entrySet()) {
            if (entry.getValue() > 0) {
                String colorName = (entry.getKey() == TinyPipes.defaultFrequency) ? "red" : DyeColor.byId(entry.getKey()).getName();
                lines.add(Component.translatable("tinypipes.overlay.power", colorName, entry.getValue()));
            }
        }
    }

    @Override
    public void readNBT(CompoundTag compoundTag) {
        super.readNBT(compoundTag);
        if (compoundTag.contains("outputs")) {
            CompoundTag outputsTag = compoundTag.getCompound("outputs").orElseGet(CompoundTag::new);
            for (String frequency : outputsTag.keySet()) {
                outputSignals.put(Integer.parseInt(frequency), outputsTag.getIntOr(frequency, 0));
            }
        }
        if (compoundTag.contains("inputs")) {
            CompoundTag inputsTag = compoundTag.getCompound("inputs").orElseGet(CompoundTag::new);
            for (String frequency : inputsTag.keySet()) {
                inputSignals.put(Integer.parseInt(frequency), inputsTag.getIntOr(frequency, 0));
            }
        }
        if (compoundTag.contains("frequencies")) {
            CompoundTag freqTag = compoundTag.getCompound("frequencies").orElseGet(CompoundTag::new);
            for (String side : freqTag.keySet()) {
                frequencies.put(Direction.valueOf(side), freqTag.getIntOr(side, 0));
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