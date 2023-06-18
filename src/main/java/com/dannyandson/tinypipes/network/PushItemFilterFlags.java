package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.components.IFilterPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PushItemFilterFlags {
    private final BlockPos pos;
    private final int index;
    boolean blacklist;

    public PushItemFilterFlags(BlockPos blockPos, Integer index, boolean blacklist){
        this.pos = blockPos;
        this.index = index;
        this.blacklist = blacklist;
    }

    public PushItemFilterFlags(FriendlyByteBuf buffer)
    {
        this.pos= buffer.readBlockPos();
        this.index =buffer.readInt();
        this.blacklist =buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(pos);
        buf.writeInt(index);
        buf.writeBoolean(blacklist);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(()-> {
            BlockEntity blockEntity = ctx.get().getSender().level().getBlockEntity(pos);
            IFilterPipe iFilterPipe = null;
            if (blockEntity instanceof PipeBlockEntity pipeBlockEntity)
            {
                if( pipeBlockEntity.getPipe(index) instanceof IFilterPipe pipe )
                    iFilterPipe = pipe;
            }
            else if(ModList.get().isLoaded("tinyredstone"))
            {
                if(TinyPipeNetworkHelper.getPipe(ctx.get().getSender().level(),pos, index) instanceof IFilterPipe pipe)
                    iFilterPipe = pipe;
            }
            if (iFilterPipe!=null)
                iFilterPipe.serverSetBlacklist(blacklist);
            ctx.get().setPacketHandled(true);
        });
        return true;
    }


}
