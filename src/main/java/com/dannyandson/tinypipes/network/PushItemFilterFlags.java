package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.components.IFilterPipe;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

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

    public PushItemFilterFlags(FriendlyByteBuf buffer)
    {
        this.pos= buffer.readBlockPos();
        this.cellIndex=buffer.readInt();
        this.blacklist =buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(pos);
        buf.writeInt(cellIndex);
        buf.writeBoolean(blacklist);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(()-> {
            BlockEntity blockEntity = ctx.get().getSender().level.getBlockEntity(pos);
            if (blockEntity instanceof PanelTile panelTile)
            {
                PanelCellPos cellPos = PanelCellPos.fromIndex(panelTile,cellIndex);
                if (cellPos.getIPanelCell() instanceof IFilterPipe iFilterPipe){
                    iFilterPipe.serverSetBlacklist(blacklist);
                }
            }
            ctx.get().setPacketHandled(true);
        });
        return true;
    }


}
