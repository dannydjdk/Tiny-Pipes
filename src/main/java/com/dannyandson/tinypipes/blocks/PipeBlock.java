package com.dannyandson.tinypipes.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends BaseEntityBlock {

    public PipeBlock() {
        super(Properties.of(Material.STONE)
                .sound(SoundType.STONE)
                .strength(1.0f)
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block p60512, BlockPos neighborPos, boolean p_60514_) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity) {
            //TODO tell the pipes about the neighbor change
        }
        super.neighborChanged(state, level, pos, p60512, neighborPos, p_60514_);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity) {
            //TODO check which pipes are in the block and drop all of their items with NBT (for filters)
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity) {
            //TODO check for redstone pipe direct signal
        }
        return super.getDirectSignal(state, level, pos, direction);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity) {
            //TODO check for redstone pipe indirect signal
        }
        return super.getSignal(state, level, pos, direction);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader level, BlockPos pos, Direction side) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSignalSource(@NotNull BlockState p_60571_) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity) {
            //TODO check which sides are active
        }
        return super.getShape(state, level, pos, context);
    }

}
