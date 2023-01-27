package com.dannyandson.tinypipes.blocks;

import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.components.full.RedstonePipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends BaseEntityBlock {

    public PipeBlock() {
        super(Properties.of(Material.STONE)
                .sound(SoundType.STONE)
                .strength(1.0f)
                .dynamicShape()
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
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            pipeBlockEntity.onNeighborChange();
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
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            if (pipeBlockEntity.getPipe(3) instanceof RedstonePipe pipe)
                return pipe.getStrongRsOutput(direction);
        }
        return super.getDirectSignal(state, level, pos, direction);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            if (pipeBlockEntity.getPipe(3) instanceof RedstonePipe pipe)
                return pipe.getWeakRsOutput(direction);
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

    @Override
    public VoxelShape getOcclusionShape(BlockState p_60578_, BlockGetter p_60579_, BlockPos p_60580_) {
        return Shapes.empty();
    }

    @Override
    protected void spawnDestroyParticles(Level p_152422_, Player p_152423_, BlockPos p_152424_, BlockState p_152425_) {
        super.spawnDestroyParticles(p_152422_, p_152423_, p_152424_, Blocks.OBSIDIAN.defaultBlockState());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (level1, blockPos, blockState, t) -> {
            if (t instanceof PipeBlockEntity pipeBlockEntity)
                pipeBlockEntity.tick();
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            ItemStack heldStack = player.getItemInHand(hand);
            if (Registry.getFullPipeClassFromItem(heldStack.getItem()) != null) {
                //using with a pipe in hand
                if (pipeBlockEntity.addPipe(heldStack))
                    return InteractionResult.CONSUME;
            } else if (heldStack.is(ItemTags.create(new ResourceLocation("forge", "tools/wrench")))) {
                //using with a wrench in hand
                AbstractFullPipe[] pipes = pipeBlockEntity.getPipes();
                Direction rayTraceDirection = hitResult.getDirection().getOpposite();
                Vec3 hitVec = hitResult.getLocation().add((double) rayTraceDirection.getStepX() * .001d, (double) rayTraceDirection.getStepY() * .001d, (double) rayTraceDirection.getStepZ() * .001d);
                double x = hitVec.x - pos.getX(),
                        y = hitVec.y - pos.getY(),
                        z = hitVec.z - pos.getZ();
                if (pipes.length == 1) {
                    Direction dir =
                            (x > 0.5703125) ? Direction.EAST :
                                    (x < 0.4296875) ? Direction.WEST :
                                            (y > 0.5703125) ? Direction.UP :
                                                    (y < 0.4296875) ? Direction.DOWN :
                                                            (z > 0.5703125) ? Direction.SOUTH :
                                                                    Direction.NORTH;
                    pipes[0].togglePipeSide(dir);
                } else {
                    Direction dir =
                            (x > 0.68) ? Direction.EAST :
                                    (x < 0.32) ? Direction.WEST :
                                            (y > 0.68) ? Direction.UP :
                                                    (y < 0.32) ? Direction.DOWN :
                                                            (z > 0.68) ? Direction.SOUTH :
                                                                    Direction.NORTH;
                    int slot = -1;
                    if(dir==Direction.NORTH || dir==Direction.SOUTH){
                        slot = (y>.5)?(x>.5)?1:0:(x>.5)?3:2;
                    } else if (dir==Direction.EAST || dir==Direction.WEST){
                        slot = (y>.5)?(z>.5)?0:1:(z>.5)?2:3;
                    } else { //up or down
                        slot = (z>.5)?(x>.5)?0:1:(x>.5)?2:3;
                    }
                    if (pipeBlockEntity.slotUsed(slot))
                        pipeBlockEntity.getPipe(slot).togglePipeSide(dir);
                }
            }
        }
        return super.use(blockState, level, pos, player, hand, hitResult);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            AbstractFullPipe[] pipes = pipeBlockEntity.getPipes();
            boolean single = pipes.length == 1;
            VoxelShape shape = (single)
                    ?Block.box(6.4, 6.4, 6.4, 9.6, 9.6, 9.6)
                    :Block.box(5, 5, 5, 11, 11, 11);

            for (AbstractFullPipe pipe : pipes) {
                if(pipe.getPipeSideStatus(Direction.NORTH)!=PipeSideStatus.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(6.875, 6.875, 0, 9.125, 9.125, 6.875))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(5.75, 8, 0, 8, 10.25, 5))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(8, 8, 0, 10.25, 10.25, 5))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(5.75, 5.75, 0, 8, 8, 5))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(8, 5.75, 0, 10.25, 8, 5))
                            :Shapes.or(shape,Block.box(5.75, 5.75, 0, 10.25, 10.25, 5));
                }
                if(pipe.getPipeSideStatus(Direction.NORTH)==PipeSideStatus.PULLING){
                    //shape = Shapes.or(shape,Block.box(4, 4, 0, 12, 12, 2));
                }
                if(pipe.getPipeSideStatus(Direction.SOUTH)!=PipeSideStatus.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(6.875, 6.875, 9.125, 9.125, 9.125, 16))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(5.75, 8, 11, 8, 10.25, 16))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(8, 8, 11, 10.25, 10.25, 16))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(5.75, 5.75, 11, 8, 8, 16))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(8, 5.75, 11, 10.25, 8, 16))
                            :Shapes.or(shape,Block.box(5.75, 5.75, 11, 10.25, 10.25, 16));
                }
                if(pipe.getPipeSideStatus(Direction.SOUTH)==PipeSideStatus.PULLING){
                    //shape = Shapes.or(shape,Block.box(4, 4, 14, 12, 12, 16));
                }
                if(pipe.getPipeSideStatus(Direction.EAST)!=PipeSideStatus.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(9.125, 6.875, 6.875, 16, 9.125, 9.125))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(11, 8, 8, 16, 10.25, 10.25))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(11, 8, 5.75, 16, 10.25, 8))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(11, 5.75, 8, 16, 8, 10.25))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(11, 5.75, 5.75, 16, 8, 8))
                            :Shapes.or(shape,Block.box(11, 5.75, 5.75, 16, 10.25, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.EAST)==PipeSideStatus.PULLING){
                    //shape = Shapes.or(shape,Block.box(14, 4, 4, 16, 12, 12));
                }
                if(pipe.getPipeSideStatus(Direction.WEST)!=PipeSideStatus.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(0, 6.875, 6.875, 6.875, 9.125, 9.125))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(0, 8, 8, 5, 10.25, 10.25))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(0, 8, 5.75, 5, 10.25, 8))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(0, 5.75, 8, 5, 8, 10.25))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(0, 5.75, 5.75, 5, 8, 8))
                            :Shapes.or(shape,Block.box(0, 5.75, 5.75, 5, 10.25, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.WEST)==PipeSideStatus.PULLING){
                    //shape = Shapes.or(shape,Block.box(0, 4, 4, 2, 12, 12));
                }
                if(pipe.getPipeSideStatus(Direction.UP)!=PipeSideStatus.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(6.875, 9.125, 6.875, 9.125, 16, 9.125))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(8, 11,8, 10.25, 16,10.25))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(5.75, 11,8, 8, 16, 10.25))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(8, 11,5.75, 10.25, 16, 8))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(5.75, 11,5.75, 8, 16, 8))
                            :Shapes.or(shape,Block.box(5.75, 11, 5.75, 10.25, 16, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.UP)==PipeSideStatus.PULLING){
                    //shape = Shapes.or(shape,Block.box(4, 14, 4, 12, 16, 12));
                }
                if(pipe.getPipeSideStatus(Direction.DOWN)!=PipeSideStatus.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(6.875, 0, 6.875, 9.125, 6.875, 9.125))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(8, 0,8, 10.25, 5,10.25))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(5.75, 0,8, 8, 5, 10.25))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(8, 0,5.75, 10.25, 5, 8))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(5.75, 0,5.75, 8, 5, 8))
                            :Shapes.or(shape,Block.box(5.75, 0, 5.75, 10.25, 5, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.DOWN)==PipeSideStatus.PULLING){
                    //shape = Shapes.or(shape,Block.box(4, 0, 4, 12, 2, 12));
                }

            }

            return shape;
        }
        return super.getShape(state, level, pos, context);
    }

}
