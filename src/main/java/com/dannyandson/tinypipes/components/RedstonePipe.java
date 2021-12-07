package com.dannyandson.tinypipes.components;

import com.dannyandson.tinypipes.TinyPipes;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class RedstonePipe extends AbstractTinyPipe implements IPanelCellInfoProvider {
    public static final ResourceLocation REDSTONE_PIPE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/redstone_pipe");
    private static TextureAtlasSprite sprite = null;
    private static TextureAtlasSprite sprite_color = null;
    private static final int defaultFrequency = 0x810E0C;

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
            sprite_color = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(ClientSetup.PIPE_TEXTURE);

        VertexConsumer builder = buffer.getBuffer((alpha == 1.0) ? RenderType.solid() : RenderType.translucent());

        RenderHelper.drawCube(poseStack, builder, sprite, c1, c2, c1, c2, c1, c2, combinedLight, 0xFFFFFFFF, alpha);

        if (pullSides.contains(Side.FRONT))
            RenderHelper.drawCube(poseStack, builder, sprite_color, p1, p2, p1, p2, c2, s3, combinedLight, getColor(Side.FRONT), alpha);
        else if (connectedSides.contains(Side.FRONT))
            RenderHelper.drawCube(poseStack, builder, sprite_color, s1, s2, s1, s2, c2, s3, combinedLight, getColor(Side.FRONT), alpha);
        if (pullSides.contains(Side.BACK))
            RenderHelper.drawCube(poseStack, builder, sprite_color, p1, p2, p1, p2, s0, c1, combinedLight, getColor(Side.BACK), alpha);
        else if (connectedSides.contains(Side.BACK))
            RenderHelper.drawCube(poseStack, builder, sprite_color, s1, s2, s1, s2, s0, c1, combinedLight, getColor(Side.BACK), alpha);
        if (pullSides.contains(Side.LEFT))
            RenderHelper.drawCube(poseStack, builder, sprite_color, c2, s3, p1, p2, p1, p2, combinedLight, getColor(Side.LEFT), alpha);
        else if (connectedSides.contains(Side.LEFT))
            RenderHelper.drawCube(poseStack, builder, sprite_color, c2, s3, s1, s2, s1, s2, combinedLight, getColor(Side.LEFT), alpha);
        if (pullSides.contains(Side.RIGHT))
            RenderHelper.drawCube(poseStack, builder, sprite_color, s0, c1, p1, p2, p1, p2, combinedLight, getColor(Side.RIGHT), alpha);
        else if (connectedSides.contains(Side.RIGHT))
            RenderHelper.drawCube(poseStack, builder, sprite_color, s0, c1, s1, s2, s1, s2, combinedLight, getColor(Side.RIGHT), alpha);
        if (pullSides.contains(Side.TOP))
            RenderHelper.drawCube(poseStack, builder, sprite_color, p1, p2, c2, s3, p1, p2, combinedLight, getColor(Side.TOP), alpha);
        else if (connectedSides.contains(Side.TOP))
            RenderHelper.drawCube(poseStack, builder, sprite_color, s1, s2, c2, s3, s1, s2, combinedLight, getColor(Side.TOP), alpha);
        if (pullSides.contains(Side.BOTTOM))
            RenderHelper.drawCube(poseStack, builder, sprite_color, p1, p2, s0, c1, p1, p2, combinedLight, getColor(Side.BOTTOM), alpha);
        else if (connectedSides.contains(Side.BOTTOM))
            RenderHelper.drawCube(poseStack, builder, sprite_color, s1, s2, s0, c1, s1, s2, combinedLight, getColor(Side.BOTTOM), alpha);
    }

    private int getColor(Side side) {
        return (frequencies.containsKey(side)) ? DyeColor.byId(frequencies.get(side)).getMaterialColor().col : defaultFrequency;
    }

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(REDSTONE_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public boolean onPlace(PanelCellPos cellPos, Player player) {
        super.onPlace(cellPos, player);

        updateInputSignal(cellPos);
        Map<Integer,Integer> outputs = getNetworkRsOutput(cellPos, null, getNextId());
        if (!outputs.equals(outputSignals)) {
            updateNetwork(cellPos, null, outputs, getNextId());
        }

        return false;
    }

    @Override
    public boolean neighborChanged(PanelCellPos cellPos) {

        updateInputSignal(cellPos);
        Map<Integer, Integer> outputs = getNetworkRsOutput(cellPos, null, getNextId());
        if (!outputs.equals(outputSignals)) {
            updateNetwork(cellPos, null, outputs, getNextId());
        }


        return false;
    }

    private boolean updateInputSignal(PanelCellPos cellPos) {
        Map<Integer, Integer> signals = new HashMap<>();

        for (Side pullSide : pullSides) {
            PanelCellNeighbor neighbor = cellPos.getNeighbor(pullSide);
            if (neighbor != null && !(neighbor.getNeighborIPanelCell() instanceof RedstonePipe)) {
                int signal = neighbor.getStrongRsOutputForWire();
                int frequency = frequencies.getOrDefault(pullSide, defaultFrequency);
                if (signal > 0) {
                    if (!signals.containsKey(frequency) || signal > signals.get(frequency))
                        signals.put(frequency, signal);
                }

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

    private void updateNetwork(PanelCellPos cellPos, @Nullable Side side, Map<Integer,Integer> rsOutputs, long queryId) {
        //check if we've already replied to this query (to prevent infinite loops if there is a loop in the pipe network)
        if (pushIds.contains(queryId))
            return;
        //check if we're connected to the querying component
        if (side != null && !connectedSides.contains(side))
            return;
        //if checks pass, add id to list
        pushIds.add(queryId);

        if (!this.outputSignals.equals(rsOutputs)) {
            this.outputSignals = rsOutputs;
            updateFlag=true;

            for (Side connectedSide : connectedSides) {
                PanelCellNeighbor neighbor = cellPos.getNeighbor(connectedSide);
                if (neighbor!=null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe neighborPipe) {
                    neighborPipe.updateNetwork(neighbor.getCellPos(), neighbor.getNeighborsSide(), rsOutputs, queryId);
                }
            }

        }

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
        return (connectedSides.contains(side) && !pullSides.contains(side)) ? outputSignals.getOrDefault(frequencies.getOrDefault(side, defaultFrequency), 0) : 0;
    }

    @Override
    public int getWeakRsOutput(Side side) {
        return getStrongRsOutput(side);
    }

    @Override
    public boolean hasActivation(Player player) {
        return player.getMainHandItem().getItem() == Registration.REDSTONE_WRENCH.get() || player.getMainHandItem().getItem() instanceof DyeItem;
    }

    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, Player player) {
        if (player.getMainHandItem().getItem()== Registration.REDSTONE_WRENCH.get())
        {
            Side sideOfCell = getClickedSide(cellPos,player);
            if (connectedSides.contains(sideOfCell)){
                PanelCellNeighbor neighbor = cellPos.getNeighbor(sideOfCell);
                if (!pullSides.contains(sideOfCell) && (neighbor==null || !(neighbor.getNeighborIPanelCell() instanceof RedstonePipe)))
                    pullSides.add(sideOfCell);
                else{
                    pullSides.remove(sideOfCell);
                    connectedSides.remove(sideOfCell);
                }
            }else
                connectedSides.add(sideOfCell);
            updateInputSignal(cellPos);
            Map<Integer,Integer> outputs = getNetworkRsOutput(cellPos, null, getNextId());
            if (!outputs.equals(outputSignals)) {
                updateNetwork(cellPos, null, outputs, getNextId());
            }
            return true;
        } else if (player.getMainHandItem().getItem() instanceof DyeItem dyeItem) {
            Side sideClicked = getClickedSide(cellPos, player);
            DyeColor dyeColor = dyeItem.getDyeColor();
            if (dyeColor == DyeColor.RED)
                frequencies.remove(sideClicked);
            else
                frequencies.put(sideClicked, dyeItem.getDyeColor().getId());
            if (pullSides.contains(sideClicked))
                neighborChanged(cellPos);
            else if (connectedSides.contains(sideClicked))
                updateFlag=true;
        }
        return false;
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
            String color = (set.getKey() == defaultFrequency) ? "red" : DyeColor.byId(set.getKey()).getName();
            iOverlayBlockInfo.addInfo("Power(" + color + "): " + set.getValue().toString());
        }
    }
}
