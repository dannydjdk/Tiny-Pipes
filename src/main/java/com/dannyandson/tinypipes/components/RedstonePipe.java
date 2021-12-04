package com.dannyandson.tinypipes.components;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinyredstone.api.IOverlayBlockInfo;
import com.dannyandson.tinyredstone.api.IPanelCellInfoProvider;
import com.dannyandson.tinyredstone.blocks.*;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

public class RedstonePipe extends AbstractTinyPipe implements IPanelCellInfoProvider {
    public static final ResourceLocation REDSTONE_PIPE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/redstone_pipe");
    private static TextureAtlasSprite sprite = null;

    private static final AtomicLong NEXT_ID = new AtomicLong(0);
    private static long getNextId() {
        return NEXT_ID.getAndIncrement();
    }

    private int inputSignal = 0;
    private int outputSignal = 0;
    private boolean updateFlag = false;

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(REDSTONE_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public boolean neighborChanged(PanelCellPos cellPos) {
        int inputSignal = 0;
        for (Side pullSide : pullSides) {
            PanelCellNeighbor neighbor = cellPos.getNeighbor(pullSide);
            if (neighbor != null)
                inputSignal = Math.max(inputSignal, neighbor.getStrongRsOutputForWire());
        }

        this.inputSignal = inputSignal;
        int outputSignal = getNetworkRsOutput(cellPos, null, getNextId());
        if (outputSignal != this.outputSignal) {
            updateNetwork(cellPos, null, outputSignal, getNextId());
        }

        return false;
    }

    private int getNetworkRsOutput(PanelCellPos cellPos, @Nullable Side side, long queryId){
        //check if we've already replied to this query (to prevent infinite loops if there is a loop in the pipe network)
        if (pushIds.contains(queryId))
            return 0;
        //check if we're connected to the querying component
        if (side!=null && !connectedSides.contains(side))
            return 0;

        //if checks pass, add id to list
        pushIds.add(queryId);

        int rsOutput = this.inputSignal;

        for (Side connectedSide : connectedSides){
            PanelCellNeighbor neighbor = cellPos.getNeighbor(connectedSide);
            if (neighbor!=null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe neighborPipe) {
                int p = neighborPipe.getNetworkRsOutput(neighbor.getCellPos(),neighbor.getNeighborsSide(),queryId);
                if (p>rsOutput)rsOutput=p;
            }
        }

        return rsOutput;
    }

    private void updateNetwork(PanelCellPos cellPos, @Nullable Side side, int rsOutput, long queryId) {
        //check if we've already replied to this query (to prevent infinite loops if there is a loop in the pipe network)
        if (pushIds.contains(queryId))
            return;
        //check if we're connected to the querying component
        if (side != null && !connectedSides.contains(side))
            return;
        //if checks pass, add id to list
        pushIds.add(queryId);

        if (rsOutput != outputSignal) {
            outputSignal = rsOutput;

            for (Side connectedSide : connectedSides) {
                PanelCellNeighbor neighbor = cellPos.getNeighbor(connectedSide);
                if (neighbor!=null && neighbor.getNeighborIPanelCell() instanceof RedstonePipe neighborPipe) {
                    neighborPipe.updateNetwork(neighbor.getCellPos(), neighbor.getNeighborsSide(), rsOutput, queryId);
                }else{
                    updateFlag=true;
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
        return (connectedSides.contains(side) && !pullSides.contains(side)) ? outputSignal : 0;
    }

    @Override
    public int getWeakRsOutput(Side side) {
        return getStrongRsOutput(side);
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
            neighborChanged(cellPos);
            return true;
        }
        return false;
    }

    @Override
    public void readNBT(CompoundTag compoundTag) {
        super.readNBT(compoundTag);
        outputSignal=compoundTag.getInt("output");
        inputSignal=compoundTag.getInt("input");
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();
        nbt.putInt("output",outputSignal);
        nbt.putInt("input",inputSignal);
        return nbt;
    }

    @Override
    public void addInfo(IOverlayBlockInfo iOverlayBlockInfo, PanelTile panelTile, PosInPanelCell posInPanelCell) {
        iOverlayBlockInfo.setPowerOutput(outputSignal);
    }
}
