package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PushPipeConnection {
    private final BlockPos pos;
    private final int index;
    private final PipeConnectionState connectionState;
    private final Direction side;

    public PushPipeConnection(BlockPos pos, Integer index, Direction side, PipeConnectionState connectionState){
        this.pos = pos;
        this.index = index;
        this.connectionState = connectionState;
        this.side = side;
    }

    public PushPipeConnection(FriendlyByteBuf buffer)
    {
        this.pos= buffer.readBlockPos();
        this.index =buffer.readInt();
        this.connectionState =buffer.readEnum(PipeConnectionState.class);
        this.side =buffer.readEnum(Direction.class);
    }

    public void toBytes(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(pos);
        buf.writeInt(index);
        buf.writeEnum(connectionState);
        buf.writeEnum(side);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(()-> {
            BlockEntity blockEntity = ctx.get().getSender().level.getBlockEntity(pos);
            if (blockEntity instanceof PipeBlockEntity pipeBlockEntity) {
                AbstractFullPipe pipe = pipeBlockEntity.getPipe(index);
                if (pipeBlockEntity.getPipe(index) != null) {
                    pipe.setConnectionState(side,connectionState);
                }
            }else if(ModList.get().isLoaded("tinyredstone")) {
                TinyPipeNetworkHelper.setTinyPipeSideState(ctx.get().getSender().level,pos,index,side,connectionState);
            }
            ctx.get().setPacketHandled(true);
        });
        return true;
    }
}
