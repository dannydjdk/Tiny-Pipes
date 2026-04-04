package com.dannyandson.tinypipes.components.tiny;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinyredstone.api.IOverlayBlockInfo;
import com.dannyandson.tinyredstone.api.IPanelCellInfoProvider;
import com.dannyandson.tinyredstone.blocks.*;
import com.dannyandson.tinyredstone.setup.ModRegistration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.core.component.DataComponents;

import org.jspecify.annotations.Nullable;
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

        VertexConsumer builder = buffer.getBuffer((alpha == 1.0) ? Sheets.cutoutBlockSheet() : Sheets.translucentBlockSheet());

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
        if (!nei1IsPipe && !nei2IsPipe) return false; // if both neighbors are not pipes, we don't need to do anything
        RedstonePipe neighborPipe1 = nei1IsPipe ? (RedstonePipe) neighbor1.getNeighborIPanelCell() : null;
        RedstonePipe neighborPipe2 = nei2IsPipe ? (RedstonePipe) neighbor2.getNeighborIPanelCell() : null;

        // if one of the neighbors is a pipe, we need to update the output signal
        for (int frequency : TinyPipes.possibleFrequencies) {
            int s1a = (nei1IsPipe) ? neighborPipe1.outputSignals.getOrDefault(frequency, 0) : 0;
            int s2a = (nei2IsPipe) ? neighborPipe2.outputSignals.getOrDefault(frequency, 0) : 0;
            signal = Math.max(s1a, s2a);
            outputSignals.put(frequency, signal); // update the output signal of this pipe
            if (nei1IsPipe && nei2IsPipe) {
                // if both neighbors are pipes, we need to update the pipes with the minimum signal
                if (s1a < s2a) {
                    neighborPipe1.onInputSignalChange(neighbor1.getCellPos(), getGlobalSide(side1.getOpposite(), cellPos.getCellFacing()), frequency, s1a, signal, false,false,false);
                } else if (s2a < s1a) {
                    neighborPipe2.onInputSignalChange(neighbor2.getCellPos(), getGlobalSide(side2.getOpposite(), cellPos.getCellFacing()), frequency, s2a, signal, false,false,false);
                }
            }
        }

        return false;
    }

    // function to convert the intern side to a global side
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

    // function to convert the global side to an intern side
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
            onInputSignalChange(cellPos, side, frequency, sint, 0,false,false,true);
        }
    }

    @Override
    public boolean neighborChanged(PanelCellPos cellPos) {
        // here we update the input only on the change in input
        if (updateInputSignal(cellPos)){
            // if the input signal changed, we need to update the network as if one of the pulling sides changed
            for (Side pullSide : pullSides) {
                updateSignal(cellPos, pullSide);
            }
        }

        return false;
    }

    public void updateSignal(PanelCellPos cellPos, @Nullable Side side) {
        if (side == null) {
            // update all directions
            for (Side sid : Side.values()) {
                updateSignal(cellPos, sid);
            }
        }
        // update on input signal change

        int frequency = frequencies.getOrDefault(side, TinyPipes.defaultFrequency);
        // signal inside the pipe
        int sint = outputSignals.getOrDefault(frequency, 0);
        int sinp = getInputSignal(cellPos,side);
        onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, sinp,true,false,false);
    }

    public int getInputSignal(PanelCellPos cellPos, Side side){
        PanelCellNeighbor neighbor = cellPos.getNeighbor(side);
        if (neighbor == null || neighbor.getNeighborIPanelCell() instanceof RedstonePipe) {
            return 0;
        }
        return (neighbor.canConnectRedstone())?neighbor.getWeakRsOutput():neighbor.getStrongRsOutputForWire();
    }

    // this method is called for every update of the pipes
    public void onInputSignalChange(PanelCellPos cellPos, @Nullable Side side, int frequency, int sint, int sinp, boolean updateInputList,boolean allowDisabled,boolean sidePipeIsDestroyed) {
        //side must be the global side
        if (updateInputList){
            inputSignals.put(frequency, sinp); // update input signal
        }
        // update on input signal change
        if (sint > sinp) { // signal decreased
            Map<Integer, Integer> p = getNetworkRsOutput(cellPos, null, getNextId(), sidePipeIsDestroyed ? side : null);
            int spul = p.getOrDefault(frequency, 0);
            if (spul < sint) {
                updateOutput(cellPos, side, frequency, spul, getNextId(),allowDisabled);
            }
        } else if (sint < sinp){ // signal increased
            updateOutput(cellPos, side, frequency, sinp, getNextId(),allowDisabled);
        }
    }

    public void updateOutput(PanelCellPos cellPos, Side side, int frequency,int signal,long queryId,boolean allowDisabled) {
        if (pushIds.contains(queryId)) {
            return;
        }
        if (!connectedSides.contains(getInternSide(side, cellPos.getCellFacing())) && !allowDisabled) {
            return;
        }
        pushIds.add(queryId);
        outputSignals.put(frequency, signal);
        updateFlag = true;
        for (Side sid : connectedSides) {
            PanelCellNeighbor neighbor = cellPos.getNeighbor(sid);
            Side globalSide = getGlobalSide(sid, cellPos.getCellFacing());
            if (neighbor != null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe redstonePipe && globalSide != side) {
                redstonePipe.updateOutput(neighbor.getCellPos(), globalSide.getOpposite(), frequency, signal,queryId,false);
            }
        }
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

    private Map<Integer,Integer> getNetworkRsOutput(PanelCellPos cellPos, @Nullable Side side, long queryId, @Nullable Side ignoredSide) {
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
            if (getGlobalSide(connectedSide,cellPos.getCellFacing()) == ignoredSide) {
                continue; // skip the side that is the origin of the update
            }
            PanelCellNeighbor neighbor = cellPos.getNeighbor(connectedSide);
            if (neighbor != null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe neighborPipe) {
                Map<Integer, Integer> p = neighborPipe.getNetworkRsOutput(neighbor.getCellPos(), neighbor.getNeighborsSide(), queryId, null);
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
        return super.hasActivation(player) || player.getMainHandItem().has(DataComponents.DYE);
    }

    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, Player player) {
        if (player.getMainHandItem().getItem() == ModRegistration.REDSTONE_WRENCH.get()) {
            Side sideOfCell = getClickedSide(cellPos, player);
            toggleSideConnection(cellPos, sideOfCell);
            return true;
        } else if (player.getMainHandItem().has(DataComponents.DYE)) {
            Side sideClicked = getClickedSide(cellPos, player);
            DyeColor dyeColor = player.getMainHandItem().get(DataComponents.DYE);
            setColor(cellPos, sideClicked, dyeColor);
        } else {
            super.onBlockActivated(cellPos, segmentClicked, player);
        }
        return false;
    }

    public void setColor(PanelCellPos cellPos, Side side, DyeColor color) {
        //update of the pipe for the old frequency (update as if the input signal is 0)
        if (pullSides.contains(side)) {
            int frequency = frequencies.getOrDefault(side, TinyPipes.defaultFrequency);
            int sint = outputSignals.getOrDefault(frequency, 0);
            onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, 0,true,false,false);
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
        setConnectionState(cellPos, sideOfCell, returnState);

        return  returnState;
    }

    public void onToggle(PanelCellPos cellPos, Side side) {
        PipeConnectionState currentState = getSideConnection(side);
        if (currentState == PipeConnectionState.DISABLED) {
            PanelCellNeighbor neighbor = cellPos.getNeighbor(side);
            if (neighbor != null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe neighborPipe) {
                for (int frequency : TinyPipes.possibleFrequencies) {
                    int sp = neighborPipe.outputSignals.getOrDefault(frequency,0);
                    int sint = this.outputSignals.getOrDefault(frequency, 0);
                    onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, 0,false,true,false);
                    neighborPipe.onInputSignalChange(neighbor.getCellPos(), getGlobalSide(side.getOpposite(),neighbor.getCellPos().getCellFacing()), frequency, sp, 0,false,false,false);
                }
            } else {
                int frequency = frequencies.getOrDefault(side, TinyPipes.defaultFrequency);
                int sinp = getInputSignal(cellPos,side);
                if (sinp != 0){
                    int sint = outputSignals.getOrDefault(frequency, 0);
                    onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, 0,true,true,false);
                }
            }
        } else if (currentState == PipeConnectionState.PULLING) {
            int frequency = frequencies.getOrDefault(side, TinyPipes.defaultFrequency);
            int sinp = getInputSignal(cellPos,side);
            if (sinp != 0) {
                int sint = outputSignals.getOrDefault(frequency, 0);
                onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, sinp, true,false,false);
            }
        } else { //currentState == PipeConnectionState.ENABLED
            PanelCellNeighbor neighbor = cellPos.getNeighbor(side);
            if (neighbor != null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe neighborPipe) {
                if (neighborPipe.connectedSides.contains(side.getOpposite())) {
                    for (int frequency : TinyPipes.possibleFrequencies) {
                        int sp = neighborPipe.outputSignals.getOrDefault(frequency,0);
                        int sint = this.outputSignals.getOrDefault(frequency, 0);
                        if (sp == sint){
                            continue;
                        }
                        if (sp > sint) {
                            onInputSignalChange(cellPos, getGlobalSide(side,cellPos.getCellFacing()), frequency, sint, sp,false,false,false);
                        } else {
                            neighborPipe.onInputSignalChange(neighbor.getCellPos(), getGlobalSide(side.getOpposite(),neighbor.getCellPos().getCellFacing()), frequency, sp, sint,false,false,false);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setConnectionState(PanelCellPos cellPos, Side side, PipeConnectionState state) {
        super.setConnectionState(cellPos, side, state);
        onToggle(cellPos, side);
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
                frequencies.put(Side.valueOf(side), freqTag.getIntOr(side, 0));
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
            if (set.getValue()>0) {
                String color = (set.getKey() == TinyPipes.defaultFrequency) ? "red" : DyeColor.byId(set.getKey()).getName();
                iOverlayBlockInfo.addInfo("Power(" + color + "): " + set.getValue().toString());
            }
        }
    }
}
