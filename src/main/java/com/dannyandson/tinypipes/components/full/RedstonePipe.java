package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeSideStatus;
import com.dannyandson.tinypipes.components.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.dannyandson.tinypipes.components.RenderHelper.REDSTONE_PIPE_TEXTURE;

public class RedstonePipe extends AbstractFullPipe{

    private static final int defaultFrequency = 0x810E0C;

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

    //@Override
    private int getColor(Direction side) {
        return (frequencies.containsKey(side)) ? DyeColor.byId(frequencies.get(side)).getMaterialColor().col : defaultFrequency;
    }

    @Override
    public boolean onPlace(PipeBlockEntity pipeBlockEntity) {
        super.onPlace(pipeBlockEntity);

        updateInputSignal(pipeBlockEntity);
        Map<Integer,Integer> outputs = getNetworkRsOutput(pipeBlockEntity, null, getNextId());
        if (!outputs.equals(outputSignals)) {
            updateNetwork(pipeBlockEntity, null, outputs, getNextId());
        }

        return false;
    }

    @Override
    public boolean neighborChanged(PipeBlockEntity pipeBlockEntity) {

        updateInputSignal(pipeBlockEntity);
        Map<Integer, Integer> outputs = getNetworkRsOutput(pipeBlockEntity, null, getNextId());
        if (!outputs.equals(outputSignals)) {
            updateNetwork(pipeBlockEntity, null, outputs, getNextId());
        }


        return false;
    }

    private boolean updateInputSignal(PipeBlockEntity pipeBlockEntity) {
        Map<Integer, Integer> signals = new HashMap<>();

        for (Direction direction : Direction.values()) {
            if (getPipeSideStatus(direction) == PipeSideStatus.PULLING && pipeBlockEntity.getLevel() != null) {
                BlockPos neighbor = pipeBlockEntity.getBlockPos().relative(direction);
                BlockState neighborState = pipeBlockEntity.getLevel().getBlockState(neighbor);
                if (!(pipeBlockEntity.getLevel().getBlockEntity(neighbor) instanceof PipeBlockEntity)) {
                    int signal = (neighborState.canRedstoneConnectTo(pipeBlockEntity.getLevel(),pipeBlockEntity.getBlockPos(),direction.getOpposite()))
                            ? pipeBlockEntity.getLevel().getSignal(neighbor,direction.getOpposite())
                            : pipeBlockEntity.getLevel().getDirectSignal(neighbor,direction.getOpposite()) ;
                    int frequency = frequencies.getOrDefault(direction, defaultFrequency);
                    if (signal > 0) {
                        if (!signals.containsKey(frequency) || signal > signals.get(frequency))
                            signals.put(frequency, signal);
                    }

                }
            }
        }

        if (!signals.equals(inputSignals)) {
            inputSignals = signals;
            return true;
        }
        return false;
    }

    private Map<Integer,Integer> getNetworkRsOutput(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, long queryId) {
        //check if we've already replied to this query (to prevent infinite loops if there is a loop in the pipe network)
        if (pushIds.contains(queryId))
            return new HashMap<>();
        //check if we're connected to the querying component
        if (side != null && getPipeSideStatus(side) == PipeSideStatus.DISABLED)
            return new HashMap<>();

        //if checks pass, add id to list
        pushIds.add(queryId);

        Map<Integer, Integer> rsOutputs = new HashMap<>(this.inputSignals);

        for (Direction direction : Direction.values()) {
            if (getPipeSideStatus(direction) == PipeSideStatus.ENABLED) {
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

    private void updateNetwork(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, Map<Integer,Integer> rsOutputs, long queryId) {
        //check if we've already replied to this query (to prevent infinite loops if there is a loop in the pipe network)
        if (pushIds.contains(queryId))
            return;
        //check if we're connected to the querying component
        if (side != null && getPipeSideStatus(side) == PipeSideStatus.DISABLED)
            return;
        //if checks pass, add id to list
        pushIds.add(queryId);

        if (!this.outputSignals.equals(rsOutputs)) {
            this.outputSignals = rsOutputs;
            updateFlag=true;

            for (Direction direction : Direction.values()) {
                if (getPipeSideStatus(direction) == PipeSideStatus.ENABLED) {
                    BlockPos neighbor = pipeBlockEntity.getBlockPos().relative(direction);
                    if (pipeBlockEntity.getLevel().getBlockEntity(neighbor) instanceof PipeBlockEntity pipeBlockEntity2 && pipeBlockEntity2.hasPipe(RedstonePipe.class)) {
                        ((RedstonePipe)pipeBlockEntity2.getPipe(this.slotPos())).updateNetwork(pipeBlockEntity2, direction.getOpposite(), rsOutputs, queryId);
                    }
                }
            }
        }

    }

    @Override
    public boolean tick(PipeBlockEntity pipeBlockEntity) {
        if (updateFlag){
            updateFlag=false;
            return true;
        }
        return false;
    }

    //@Override
    public int getStrongRsOutput(Direction side) {
        return (this.getPipeSideStatus(side)==PipeSideStatus.ENABLED) ? outputSignals.getOrDefault(frequencies.getOrDefault(side, defaultFrequency), 0) : 0;
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
                outputSignals.put(Integer.getInteger(frequency), compoundTag.getCompound("outputs").getInt(frequency));
            }
        }
        if (compoundTag.contains("inputs")) {
            for (String frequency : compoundTag.getCompound("inputs").getAllKeys()) {
                inputSignals.put(Integer.getInteger(frequency), compoundTag.getCompound("inputs").getInt(frequency));
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
