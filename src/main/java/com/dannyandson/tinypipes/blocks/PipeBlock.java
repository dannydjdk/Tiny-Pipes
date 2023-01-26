package com.dannyandson.tinypipes.blocks;

import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
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
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            VoxelShape shape = Block.box(5, 5, 5, 11, 11, 11);

            AbstractFullPipe[] pipes = pipeBlockEntity.getPipes();
            boolean single = pipes.length == 1;

            for (AbstractFullPipe pipe : pipes) {
                if(pipe.getPipeSideStatus(Direction.NORTH)!=PipeSideStatus.DISABLED){
                    shape = Shapes.or(shape,Block.box(5.75, 5.75, 0, 10.25, 10.25, 5));
                }
                if(pipe.getPipeSideStatus(Direction.NORTH)==PipeSideStatus.PULLING){
                    shape = Shapes.or(shape,Block.box(4, 4, 0, 12, 12, 2));
                }
                if(pipe.getPipeSideStatus(Direction.SOUTH)!=PipeSideStatus.DISABLED){
                    shape = Shapes.or(shape,Block.box(5.75, 5.75, 11, 10.25, 10.25, 16));
                }
                if(pipe.getPipeSideStatus(Direction.SOUTH)==PipeSideStatus.PULLING){
                    shape = Shapes.or(shape,Block.box(4, 4, 14, 12, 12, 16));
                }
                if(pipe.getPipeSideStatus(Direction.EAST)!=PipeSideStatus.DISABLED){
                    shape = Shapes.or(shape,Block.box(11, 5.75, 5.75, 16, 10.25, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.EAST)==PipeSideStatus.PULLING){
                    shape = Shapes.or(shape,Block.box(14, 4, 4, 16, 12, 12));
                }
                if(pipe.getPipeSideStatus(Direction.WEST)!=PipeSideStatus.DISABLED){
                    shape = Shapes.or(shape,Block.box(0, 5.75, 5.75, 5, 10.25, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.WEST)==PipeSideStatus.PULLING){
                    shape = Shapes.or(shape,Block.box(0, 4, 4, 2, 12, 12));
                }
                if(pipe.getPipeSideStatus(Direction.UP)!=PipeSideStatus.DISABLED){
                    shape = Shapes.or(shape,Block.box(5.75, 11, 5.75, 10.25, 16, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.UP)==PipeSideStatus.PULLING){
                    shape = Shapes.or(shape,Block.box(4, 14, 4, 12, 16, 12));
                }
                if(pipe.getPipeSideStatus(Direction.DOWN)!=PipeSideStatus.DISABLED){
                    shape = Shapes.or(shape,Block.box(5.75, 0, 5.75, 10.25, 5, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.DOWN)==PipeSideStatus.PULLING){
                    shape = Shapes.or(shape,Block.box(4, 0, 4, 12, 2, 12));
                }

            }

            return shape;
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState p_60578_, BlockGetter p_60579_, BlockPos p_60580_) {
        return Shapes.empty();
    }

    @Override
    protected void spawnDestroyParticles(Level p_152422_, Player p_152423_, BlockPos p_152424_, BlockState p_152425_) {
        super.spawnDestroyParticles(p_152422_, p_152423_, p_152424_, Blocks.OBSIDIAN.defaultBlockState());
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
            }else if (heldStack.is(ItemTags.create(new ResourceLocation("forge", "tools/wrench")))){
                //using with a wrench in hand
                AbstractFullPipe[] pipes = pipeBlockEntity.getPipes();
                if (pipes.length==1){
                    Direction rayTraceDirection = hitResult.getDirection().getOpposite();
                    Vec3 hitVec = hitResult.getLocation().add((double)rayTraceDirection.getStepX()*.001d,(double)rayTraceDirection.getStepY()*.001d,(double)rayTraceDirection.getStepZ()*.001d);
                    double x = hitVec.x-pos.getX(),
                            y=hitVec.y-pos.getY(),
                            z=hitVec.z-pos.getZ();
                    Direction dir =
                            (x>.875)?Direction.EAST:
                                    (x<.125)?Direction.WEST:
                                            (y>.875)?Direction.UP:
                                                    (y<.125)?Direction.DOWN:
                                                            (z>.875)?Direction.SOUTH:
                                                                    (z<.125)?Direction.NORTH:
                                                                            (x>=0.686)?Direction.EAST:
                                                                                    (x<=0.314)?Direction.WEST:
                                                                                            (y>=0.686)?Direction.UP:
                                                                                                    (y<=0.314)?Direction.DOWN:
                                                                                                            (z>=0.686)?Direction.SOUTH:
                                                                                                                    Direction.NORTH;
                    pipes[0].togglePipeSide(dir);
                }
            }
        }
        return super.use(blockState, level, pos, player, hand, hitResult);
    }
}
