package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.components.IFilterPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PushItemFilterFlags(BlockPos pos, int index, boolean blacklist) implements CustomPacketPayload {

    public static final Type<PushItemFilterFlags> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TinyPipes.MODID, "push_item_filter_flags"));

    public static final StreamCodec<FriendlyByteBuf, PushItemFilterFlags> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PushItemFilterFlags::pos,
                    ByteBufCodecs.INT, PushItemFilterFlags::index,
                    ByteBufCodecs.BOOL, PushItemFilterFlags::blacklist,
                    PushItemFilterFlags::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PushItemFilterFlags pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            BlockEntity blockEntity = ctx.player().level().getBlockEntity(pkt.pos());
            IFilterPipe iFilterPipe = null;
            if (blockEntity instanceof PipeBlockEntity pipeBlockEntity) {
                if (pipeBlockEntity.getPipe(pkt.index()) instanceof IFilterPipe pipe)
                    iFilterPipe = pipe;
            } else if (ModList.get().isLoaded("tinyredstone")) {
                if (TinyPipeNetworkHelper.getPipe(ctx.player().level(), pkt.pos(), pkt.index()) instanceof IFilterPipe pipe)
                    iFilterPipe = pipe;
            }
            if (iFilterPipe != null)
                iFilterPipe.serverSetBlacklist(pkt.blacklist());
        });
    }
}
