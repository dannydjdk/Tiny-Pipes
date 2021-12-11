package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.components.AbstractTinyPipe;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.Side;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PushPipeConnection {
    private final BlockPos pos;
    private final int cellIndex;
    private final AbstractTinyPipe.PipeConnectionState connectionState;
    private final Side side;

    public PushPipeConnection(PanelCellPos cellPos, Side side, AbstractTinyPipe.PipeConnectionState connectionState){
        this.pos = cellPos.getPanelTile().getBlockPos();
        this.cellIndex = cellPos.getIndex();
        this.connectionState = connectionState;
        this.side = side;
    }

    public PushPipeConnection(PacketBuffer buffer)
    {
        this.pos= buffer.readBlockPos();
        this.cellIndex=buffer.readInt();
        this.connectionState =buffer.readEnum(AbstractTinyPipe.PipeConnectionState.class);
        this.side =buffer.readEnum(Side.class);
    }

    public void toBytes(PacketBuffer buf)
    {
        buf.writeBlockPos(pos);
        buf.writeInt(cellIndex);
        buf.writeEnum(connectionState);
        buf.writeEnum(side);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(()-> {
            TileEntity blockEntity = ctx.get().getSender().level.getBlockEntity(pos);
            if (blockEntity instanceof PanelTile)
            {
                PanelCellPos cellPos = PanelCellPos.fromIndex((PanelTile) blockEntity,cellIndex);
                if (cellPos.getIPanelCell() instanceof AbstractTinyPipe){
                    ((AbstractTinyPipe)cellPos.getIPanelCell()).setConnectionState(cellPos,side, connectionState);
                    cellPos.getPanelTile().sync();
                }
            }
            ctx.get().setPacketHandled(true);
        });
        return true;
    }
}
