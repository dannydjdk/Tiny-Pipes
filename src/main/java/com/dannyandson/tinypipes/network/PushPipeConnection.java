package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PushPipeConnection(BlockPos pos, int index, int sideOrdinal, int stateOrdinal) implements CustomPacketPayload {

    public static final Type<PushPipeConnection> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TinyPipes.MODID, "push_pipe_connection"));

    public static final StreamCodec<FriendlyByteBuf, PushPipeConnection> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PushPipeConnection::pos,
                    ByteBufCodecs.INT, PushPipeConnection::index,
                    ByteBufCodecs.INT, PushPipeConnection::sideOrdinal,
                    ByteBufCodecs.INT, PushPipeConnection::stateOrdinal,
                    PushPipeConnection::new);

    // Convenience constructor matching old usage
    public PushPipeConnection(BlockPos pos, int index, Direction side, PipeConnectionState connectionState) {
        this(pos, index, side.ordinal(), connectionState.ordinal());
    }

    public Direction side() { return Direction.values()[sideOrdinal]; }
    public PipeConnectionState connectionState() { return PipeConnectionState.values()[stateOrdinal]; }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PushPipeConnection pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            BlockEntity blockEntity = ctx.player().level().getBlockEntity(pkt.pos());
            if (blockEntity instanceof PipeBlockEntity pipeBlockEntity) {
                AbstractFullPipe pipe = pipeBlockEntity.getPipe(pkt.index());
                if (pipe != null) {
                    pipe.setConnectionState(pkt.side(), pkt.connectionState());
                }
            } else if (ModList.get().isLoaded("tinyredstone")) {
                TinyPipeNetworkHelper.setTinyPipeSideState(ctx.player().level(), pkt.pos(), pkt.index(), pkt.side(), pkt.connectionState());
            }
        });
    }
}
