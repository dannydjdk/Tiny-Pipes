package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.components.ItemFilterPipe;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PushItemFilterFlags {
    private final BlockPos pos;
    private final int cellIndex;
    boolean blacklist;

    public PushItemFilterFlags(PanelCellPos cellPos, boolean blacklist){
        this.pos = cellPos.getPanelTile().getBlockPos();
        this.cellIndex = cellPos.getIndex();
        this.blacklist = blacklist;
    }

    public PushItemFilterFlags(PacketBuffer buffer)
    {
        this.pos= buffer.readBlockPos();
        this.cellIndex=buffer.readInt();
        this.blacklist =buffer.readBoolean();
    }

    public void toBytes(PacketBuffer buf)
    {
        buf.writeBlockPos(pos);
        buf.writeInt(cellIndex);
        buf.writeBoolean(blacklist);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(()-> {
            TileEntity blockEntity = ctx.get().getSender().level.getBlockEntity(pos);
            if (blockEntity instanceof PanelTile)
            {
                PanelCellPos cellPos = PanelCellPos.fromIndex((PanelTile) blockEntity,cellIndex);
                if (cellPos.getIPanelCell() instanceof ItemFilterPipe){
                    ((ItemFilterPipe)cellPos.getIPanelCell()).serverSetBlacklist(blacklist);
                }
            }
            ctx.get().setPacketHandled(true);
        });
        return true;
    }


}
