package com.dannyandson.tinypipes.components.tiny;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinyredstone.api.IOverlayBlockInfo;
import com.dannyandson.tinyredstone.api.IPanelCellInfoProvider;
import com.dannyandson.tinyredstone.blocks.*;
import com.dannyandson.tinyredstone.setup.Registration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class RedstonePipe extends AbstractTinyPipe implements IPanelCellInfoProvider {
    private static TextureAtlasSprite sprite = null;
    private static TextureAtlasSprite sprite_color = null;

    private static final AtomicLong NEXT_ID = new AtomicLong(0);
    private static long getNextId() {
        return NEXT_ID.getAndIncrement();
    }

    //saved fields
    private Map<Integer,Integer> inputSignals = new HashMap<>();
    private Map<Integer,Integer> outputSignals = new HashMap<>();
    private final Map<Side,Integer> frequencies = new HashMap<>();

    private boolean updateFlag = false;

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, float alpha) {

        TextureAtlasSprite sprite = getSprite();
        if (sprite_color == null)
            sprite_color = RenderHelper.getSprite(ClientSetup.PIPE_TEXTURE);

        VertexConsumer builder = buffer.getBuffer((alpha == 1.0) ? RenderType.solid() : RenderType.translucent());

        com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite, c1, c2, c1, c2, c1, c2, combinedLight, 0xFFFFFFFF, alpha);

        if (pullSides.contains(Side.FRONT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, p1, p2, p1, p2, c2, s3, combinedLight, getColor(Side.FRONT), alpha);
        else if (connectedSides.contains(Side.FRONT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, s1, s2, s1, s2, c2, s3, combinedLight, getColor(Side.FRONT), alpha);
        if (pullSides.contains(Side.BACK))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, p1, p2, p1, p2, s0, c1, combinedLight, getColor(Side.BACK), alpha);
        else if (connectedSides.contains(Side.BACK))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, s1, s2, s1, s2, s0, c1, combinedLight, getColor(Side.BACK), alpha);
        if (pullSides.contains(Side.LEFT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, c2, s3, p1, p2, p1, p2, combinedLight, getColor(Side.LEFT), alpha);
        else if (connectedSides.contains(Side.LEFT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, c2, s3, s1, s2, s1, s2, combinedLight, getColor(Side.LEFT), alpha);
        if (pullSides.contains(Side.RIGHT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, s0, c1, p1, p2, p1, p2, combinedLight, getColor(Side.RIGHT), alpha);
        else if (connectedSides.contains(Side.RIGHT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, s0, c1, s1, s2, s1, s2, combinedLight, getColor(Side.RIGHT), alpha);
        if (pullSides.contains(Side.TOP))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, p1, p2, c2, s3, p1, p2, combinedLight, getColor(Side.TOP), alpha);
        else if (connectedSides.contains(Side.TOP))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, s1, s2, c2, s3, s1, s2, combinedLight, getColor(Side.TOP), alpha);
        if (pullSides.contains(Side.BOTTOM))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, p1, p2, s0, c1, p1, p2, combinedLight, getColor(Side.BOTTOM), alpha);
        else if (connectedSides.contains(Side.BOTTOM))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, sprite_color, s1, s2, s0, c1, s1, s2, combinedLight, getColor(Side.BOTTOM), alpha);
    }

    private int getColor(Side side) {
        return (frequencies.containsKey(side)) ? DyeColor.byId(frequencies.get(side)).getMapColor().col : TinyPipes.defaultFrequency;
    }

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(com.dannyandson.tinypipes.components.RenderHelper.REDSTONE_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public boolean onPlace(PanelCellPos cellPos, Player player) {
        super.onPlace(cellPos, player);
        // update of connected pipes when this pipe is placed
        int signal = 0;
        if (connectedSides.size() != 2) throw new IllegalStateException("RedstonePipe must have exactly two connected sides when placed, found: " + connectedSides.size());
        Side side1 = connectedSides.get(0);
        Side side2 = connectedSides.get(1);
        PanelCellNeighbor neighbor1 = cellPos.getNeighbor(side1);
        PanelCellNeighbor neighbor2 = cellPos.getNeighbor(side2);
        boolean nei1IsPipe = neighbor1 != null && neighbor1.getNeighborIPanelCell() instanceof RedstonePipe;
        boolean nei2IsPipe = neighbor2 != null && neighbor2.getNeighborIPanelCell() instanceof RedstonePipe;
        if (!nei1IsPipe && !nei2IsPipe)return false; // if both neighbors are not pipes, we don't need to do anything
        RedstonePipe neighborPipe1 = nei1IsPipe ? (RedstonePipe) neighbor1.getNeighborIPanelCell() : null;
        RedstonePipe neighborPipe2 = nei2IsPipe ? (RedstonePipe) neighbor2.getNeighborIPanelCell() : null;

        // if one of the neighbors is a pipe, we need to update the output signal
        for (int frequency : TinyPipes.possibleFrequencies) {
            int s1 = (nei1IsPipe) ? neighborPipe1.outputSignals.getOrDefault(frequency, 0) : 0;
            int s2 = (nei2IsPipe) ? neighborPipe2.outputSignals.getOrDefault(frequency, 0) : 0;
            signal = Math.max(s1, s2);
            outputSignals.put(frequency, signal); // update the output signal of this pipe
            if (nei1IsPipe && nei2IsPipe) {
                // if both neighbors are pipes, we need to update the pipes with the minimum signal
                if (s1 < s2) {
                    neighborPipe1.onInputSignalChange(neighbor1.getCellPos(), getGlobalSide(side1.getOpposite(), cellPos.getCellFacing()), frequency, s1, signal, false,false);
                } else if (s2 < s1) {
                    neighborPipe2.onInputSignalChange(neighbor2.getCellPos(), getGlobalSide(side2.getOpposite(), cellPos.getCellFacing()), frequency, s2, signal, false,false);
                }
            }
        }

        return false;
    }

    // function to convert the intern side to a global side (e.g. side LEFT is global side LEFT only if the cell facing is FRONT and it is the global RIGHT side if the cell facing is BACK)
    public static Side getGlobalSide(Side side, Side cellFacing) {
        switch (cellFacing) {
            case FRONT:
                return side;
            case BACK:
                return side.rotateYCCW().rotateYCCW();
            case LEFT:
                return side.rotateYCCW();
            case RIGHT:
                return side.rotateYCW();
            case TOP:
                return side.rotateBack();
            case BOTTOM:
                return side.rotateForward();
            default:
                throw new IllegalArgumentException("Unknown side: " + side);
        }
    }

    // function to convert the global side to an intern side (e.g. global side LEFT is intern side LEFT only if the cell facing is FRONT and it is the intern RIGHT side if the cell facing is BACK)
    public static Side getInternSide(Side globalSide, Side cellFacing) {
        switch (cellFacing) {
            case FRONT:
                return globalSide;
            case BACK:
                return globalSide.rotateYCCW().rotateYCCW();
            case LEFT:
                return globalSide.rotateYCW();
            case RIGHT:
                return globalSide.rotateYCCW();
            case TOP:
                return globalSide.rotateForward();
            case BOTTOM:
                return globalSide.rotateBack();
            default:
                throw new IllegalArgumentException("Unknown side: " + globalSide);
        }
    }

    @Override
    public void onRemove(PanelCellPos cellPos) {
        // update of connected pipes when this pipe is removed
        for (Side connectedSide : connectedSides) {
            PanelCellNeighbor neighbor = cellPos.getNeighbor(connectedSide);
            if (neighbor != null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe neighborPipe) {
                neighborPipe.onRemoveNeighbor(neighbor.getCellPos(),getGlobalSide(connectedSide.getOpposite(), cellPos.getCellFacing()));
            }
        }
    }

    public void onRemoveNeighbor(PanelCellPos cellPos, Side side) {
        // on remove is called when a neighbor pipe is removed
        if (!connectedSides.contains(getInternSide(side, cellPos.getCellFacing()))) {
            return; // if the side is not connected, we don't need to do anything
        }
        for (int frequency : TinyPipes.possibleFrequencies) {
            // we need to update the signal considering that the signal is now 0
            int sint = outputSignals.getOrDefault(frequency, 0);
            onInputSignalChange(cellPos, side, frequency, sint, 0,false,false);
        }
    }

    @Override
    public boolean neighborChanged(PanelCellPos cellPos) {
        // here we update the input only on the change in input (will be called when pipe is destroyed but nothing will be done)
        if (updateInputSignal(cellPos)){
            // if the input signal changed, we need to update the network as if one of the pulling sides changed
            for (Side pullSide : pullSides) {
                updateSignal(cellPos,pullSide);
            }
        }


        return false;
    }

    public boolean updateSignal(PanelCellPos cellPos, @Nullable Side side) {
        if (side == null) {
            // update all directions
            boolean changed = false;
            for (Side sid : Side.values()) {
                if (updateSignal(cellPos, sid)) {
                    changed = true;
                }
            }
            return changed;
        }
        // update on input signal change

        int frequency = frequencies.getOrDefault(side, TinyPipes.defaultFrequency);
        // signal inside the pipe
        int sint = outputSignals.getOrDefault(frequency, 0);
        int sinp = getInputSignal(cellPos,side);
        onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, sinp,true,false);
        //todo : return true only if the input signal changed or return void
        return true;
    }

    public int getInputSignal(PanelCellPos cellPos,Side side){
        PanelCellNeighbor neighbor = cellPos.getNeighbor(side);
        if (neighbor == null || neighbor.getNeighborIPanelCell() instanceof RedstonePipe) {
            return 0;
        }
        return (neighbor.canConnectRedstone())?neighbor.getWeakRsOutput():neighbor.getStrongRsOutputForWire();
    }

    public void onInputSignalChange(PanelCellPos cellPos, @Nullable Side side, int frequency, int sint, int sinp, boolean updateInputList,boolean allowDisabled) {
        //side must be the global side
        if (updateInputList){
            inputSignals.put(frequency, sinp); // update input signal
        }
        // update on input signal change
        if (sint > sinp) { // signal decreased
            Map<Integer, Integer> p = getNetworkRsOutput(cellPos, null, getNextId());
            int spul = p.getOrDefault(frequency, 0);
            // spul is the signal that was pulled from the neighbor pulling
            // spul can only be equals to or less than sint
            if (spul < sint) {
                // if the pulled signal is less than the input signal, we need to update the output of the network with the pulled signal
                updateOutput(cellPos, side, frequency, spul, getNextId(),allowDisabled);
            }
        } else if (sint < sinp){ // signal increased
            updateOutput(cellPos, side, frequency, sinp, getNextId(),allowDisabled);
        }
    }

    public boolean updateOutput(PanelCellPos cellPos, Side side, int frequency,int signal,long queryId,boolean allowDisabled) {
        if (pushIds.contains(queryId)) {
            // if we've already replied to this query, we don't need to do anything
            return false;
        }
        if (!connectedSides.contains(getInternSide(side, cellPos.getCellFacing())) && !allowDisabled) {
            // if the side is not connected, we don't need to do anything
            return false;
        }
        pushIds.add(queryId);
        // update signal in the network in all directions (except the one that is the origin of the update)
        outputSignals.put(frequency, signal);
        updateFlag = true;
        for (Side sid : connectedSides) {
            PanelCellNeighbor neighbor = cellPos.getNeighbor(sid);
            Side globalSide = getGlobalSide(sid, cellPos.getCellFacing());
            if (neighbor != null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe redstonePipe && globalSide != side) {
                redstonePipe.updateOutput(neighbor.getCellPos(), globalSide.getOpposite(), frequency, signal,queryId,false);
            }
        }

        return false; // no change
    }

    private boolean updateInputSignal(PanelCellPos cellPos) {
        Map<Integer, Integer> signals = new HashMap<>();

        for (Side pullSide : pullSides) {
            PanelCellNeighbor neighbor = cellPos.getNeighbor(pullSide);
            if (neighbor != null && !(neighbor.getNeighborIPanelCell() instanceof RedstonePipe)) {
                int signal = (neighbor.canConnectRedstone())?neighbor.getWeakRsOutput():neighbor.getStrongRsOutputForWire();
                int frequency = frequencies.getOrDefault(pullSide, TinyPipes.defaultFrequency);
                if (!signals.containsKey(frequency) || signal > signals.get(frequency))
                    signals.put(frequency, signal);
            }
        }

        if (!signals.equals(inputSignals)) {
            inputSignals = signals;
            return true;
        }
        return false;
    }

    private Map<Integer,Integer> getNetworkRsOutput(PanelCellPos cellPos, @Nullable Side side, long queryId) {
        //check if we've already replied to this query (to prevent infinite loops if there is a loop in the pipe network)
        if (pushIds.contains(queryId))
            return new HashMap<>();
        //check if we're connected to the querying component
        if (side != null && !connectedSides.contains(side))
            return new HashMap<>();

        //if checks pass, add id to list
        pushIds.add(queryId);

        Map<Integer, Integer> rsOutputs = new HashMap<>(this.inputSignals);

        for (Side connectedSide : connectedSides) {
            PanelCellNeighbor neighbor = cellPos.getNeighbor(connectedSide);
            if (neighbor != null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe neighborPipe) {
                Map<Integer, Integer> p = neighborPipe.getNetworkRsOutput(neighbor.getCellPos(), neighbor.getNeighborsSide(), queryId);
                for (Map.Entry<Integer, Integer> entry : p.entrySet()) {
                    if (!rsOutputs.containsKey(entry.getKey()) || entry.getValue() > rsOutputs.get(entry.getKey()))
                        rsOutputs.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return rsOutputs;
    }

    @Override
    public boolean tick(PanelCellPos cellPos) {
        if (updateFlag){
            updateFlag=false;
            return true;
        }
        return false;
    }

    @Override
    public int getStrongRsOutput(Side side) {
        return (connectedSides.contains(side) && !pullSides.contains(side)) ? outputSignals.getOrDefault(frequencies.getOrDefault(side, TinyPipes.defaultFrequency), 0) : 0;
    }

    @Override
    public int getWeakRsOutput(Side side) {
        return getStrongRsOutput(side);
    }

    @Override
    public boolean hasActivation(Player player) {
        return super.hasActivation(player) || player.getMainHandItem().getItem() instanceof DyeItem;
    }

    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, Player player) {
        if (player.getMainHandItem().getItem() == Registration.REDSTONE_WRENCH.get()) {
            Side sideOfCell = getClickedSide(cellPos, player);
            toggleSideConnection(cellPos, sideOfCell);
            return true;
        } else if (player.getMainHandItem().getItem() instanceof DyeItem dyeItem) {
            Side sideClicked = getClickedSide(cellPos, player);
            DyeColor dyeColor = dyeItem.getDyeColor();
            setColor(cellPos, sideClicked, dyeColor);
        } else {
            super.onBlockActivated(cellPos, segmentClicked, player);
        }
        return false;
    }

    public void setColor(PanelCellPos cellPos, Side side, DyeColor color) {
        //update of the pipe for the old frequency (update as if the input signal is 0)
        if (pullSides.contains(side)) {
            int frequency = frequencies.getOrDefault(side,TinyPipes.defaultFrequency);
            int sint = outputSignals.getOrDefault(frequency, 0);
            onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, 0,true,false);
        }
        updateFlag = true; // we need to update the pipe neighbor redstone components
        if (color == DyeColor.RED) {
            frequencies.remove(side);
        } else {
            frequencies.put(side, color.getId());
        }
    }

    @Override
    public PipeConnectionState toggleSideConnection(PanelCellPos cellPos, Side sideOfCell) {
        PipeConnectionState connectionState = getSideConnection(sideOfCell);
        PipeConnectionState returnState;

        if (connectionState == PipeConnectionState.DISABLED) {
            returnState = PipeConnectionState.ENABLED;
        } else if (connectionState == PipeConnectionState.PULLING) {
            returnState = PipeConnectionState.DISABLED;
        } else {
            PanelCellNeighbor neighbor = cellPos.getNeighbor(sideOfCell);
            if (neighbor == null || !(neighbor.getNeighborIPanelCell() instanceof RedstonePipe)) {
                returnState = PipeConnectionState.PULLING;
            } else {
                returnState = PipeConnectionState.DISABLED;
            }
        }
        setConnectionState(cellPos,sideOfCell,returnState);

        return  returnState;
    }

    public void onToggle(PanelCellPos cellPos, Side side) {
        PipeConnectionState currentState = getSideConnection(side);
        if (currentState == PipeConnectionState.DISABLED) {
            // the old state was pulling or enabled if we have a pipe,
            // so we need to do an update if the input signal is not zero if pulling,
            // or we need to do an update if the pipe nearby has a signal if enabled
            PanelCellNeighbor neighbor = cellPos.getNeighbor(side);
            if (neighbor != null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe neighborPipe) {
                // if the neighbor has the same pipe type, the last state is enabled, and it is as if an input signal is suddenly 0
                for (int frequency : TinyPipes.possibleFrequencies) {
                    int sp = neighborPipe.outputSignals.getOrDefault(frequency,0);
                    int sint = this.outputSignals.getOrDefault(frequency, 0);
                    // update of the toggled pipe as if input signal is 0
                    onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, 0,false,true);
                    // update of the neighbor pipe as if input signal is 0
                    neighborPipe.onInputSignalChange(neighbor.getCellPos(), getGlobalSide(side.getOpposite(),neighbor.getCellPos().getCellFacing()), frequency, sp, 0,false,false);
                }
            } else {
                // if the neighbor does not have the same pipe type, the last state is pulling, and we need to update the input signal
                int frequency = frequencies.getOrDefault(side, TinyPipes.defaultFrequency);
                int sinp = getInputSignal(cellPos,side);
                if (sinp != 0){
                    int sint = outputSignals.getOrDefault(frequency, 0);
                    onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, 0,true,true);
                }
            }
        } else if (currentState == PipeConnectionState.PULLING) {
            // the old state was enabled, so we need to update if the input signal is not zero as first if condition
            int frequency = frequencies.getOrDefault(side, TinyPipes.defaultFrequency);
            int sinp = getInputSignal(cellPos,side);
            if (sinp != 0) {
                int sint = outputSignals.getOrDefault(frequency, 0);
                onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, sinp, true,false);
            }
        } else { //currentState == PipeConnectionState.ENABLED
            // the old state was disabled,
            // we need to update both pipe if the nearby block is a pipe
            PanelCellNeighbor neighbor = cellPos.getNeighbor(side);
            if (neighbor != null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe neighborPipe) {
                // if the neighbor has the same pipe type and is connected, the last state is enabled,
                // and it is as if an input signal was not 0 for the pipe with the lower signal (for each frequency)
                if (neighborPipe.connectedSides.contains(side.getOpposite())) {
                    for (int frequency : TinyPipes.possibleFrequencies) {
                        int sp = neighborPipe.outputSignals.getOrDefault(frequency,0);
                        int sint = this.outputSignals.getOrDefault(frequency, 0);
                        if (sp == sint){
                            continue; // no signal to update
                        }
                        if (sp > sint) {
                            // if the neighbor pipe has a higher signal, we need to update the signal of the toggled pipe
                            onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, sp,false,false);
                        } else { // sp < sint
                            // if the toggled pipe has a higher signal, we need to update the input signal of the neighbor pipe
                            neighborPipe.onInputSignalChange(neighbor.getCellPos(), getGlobalSide(side.getOpposite(),neighbor.getCellPos().getCellFacing()), frequency, sp, sint,false,false);
                        }
                    }
                }
            } // if the neighbor does not have the same pipe type, we don't update the input signal
        }
    }

    @Override
    public void setConnectionState(PanelCellPos cellPos, Side side, PipeConnectionState state) {
        super.setConnectionState(cellPos, side, state);
        onToggle(cellPos,side);
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
                frequencies.put(Side.valueOf(side), compoundTag.getCompound("frequencies").getInt(side));
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
            for (Map.Entry<Side, Integer> set : frequencies.entrySet())
                if (set.getKey() != null)
                    frequenciesNBT.putInt(set.getKey().name(), set.getValue());
            nbt.put("frequencies", frequenciesNBT);
        }

        return nbt;
    }

    @Override
    public void addInfo(IOverlayBlockInfo iOverlayBlockInfo, PanelTile panelTile, PosInPanelCell posInPanelCell) {
        for (Map.Entry<Integer, Integer> set : outputSignals.entrySet()) {
            String color = (set.getKey() == TinyPipes.defaultFrequency) ? "red" : DyeColor.byId(set.getKey()).getName();
            iOverlayBlockInfo.addInfo("Power(" + color + "): " + set.getValue().toString());
        }
    }
}
