package com.dannyandson.tinypipes.blocks;

import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PipeBlockEntity extends BlockEntity {

    private List<AbstractFullPipe> pipes = new ArrayList();
    private Map<Direction,PipeSideStatus> sideStatusMap = new HashMap<>();

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.PIPE_BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * Loading and saving block entity data from disk and syncing to client
     */

    protected void sync() {
        if (!level.isClientSide)
            this.level.sendBlockUpdated(worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        this.setChanged();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = new CompoundTag();
        this.saveAdditional(nbt);
        return nbt;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        //TODO: load pipes contained, sides activated, nbt for pipes
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        //TODO: save pipes contained, sides activated, nbt from pipes
    }


}
