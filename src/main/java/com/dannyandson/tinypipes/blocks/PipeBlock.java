package com.dannyandson.tinypipes.blocks;

import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.components.full.AbstractCapFullPipe;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.components.full.PipeSide;
import com.dannyandson.tinypipes.components.full.RedstonePipe;
import com.dannyandson.tinypipes.items.SpeedUpgradeItem;
import com.dannyandson.tinypipes.setup.Registration;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends BaseEntityBlock {

    public static final MapCodec<PipeBlock> CODEC = simpleCodec(p -> new PipeBlock());
    private static final net.minecraft.tags.TagKey<Item> WRENCH_TAG =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    public PipeBlock() {
        super(Properties.of()
                .sound(SoundType.STONE)
                .strength(1.0f)
                .dynamicShape()
                .forceSolidOn()
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * Stashes the rotation requested by a {@code Block.rotate} call so that
     * {@link PipeBlockEntity#loadAdditional} can apply and consume.
     * Block#rotate has no BlockEntity access, so we hand the rotation off to
     * the BE's NBT-load on the same thread (the pattern Sable sub-level disassembly uses).
     */
    public static final ThreadLocal<Rotation> PENDING_ROTATION = new ThreadLocal<>();

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        if (rotation != Rotation.NONE) PENDING_ROTATION.set(rotation);
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation) {
        if (rotation != Rotation.NONE) PENDING_ROTATION.set(rotation);
        return state;
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
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean p_60514_) {
        // if the neighbor block is a pipe as argument and is null by blockstate, we update using the onRemoveNeighbor method
        Direction direction = Direction.getNearest(
                neighborPos.getX() - pos.getX(),
                neighborPos.getY() - pos.getY(),
                neighborPos.getZ() - pos.getZ()
        );
        if (level.getBlockState(neighborPos).getBlock() == Blocks.AIR && block instanceof PipeBlock && level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            for (AbstractFullPipe pipe : pipeBlockEntity.getPipes()) {
                pipe.onRemoveNeighbor(pipeBlockEntity, direction);
            }
            return;
        }
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            pipeBlockEntity.onNeighborChange(direction);
        }
        super.neighborChanged(state, level, pos, block, neighborPos, p_60514_);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader levelReader, BlockPos pos, BlockPos neighborPos) {
        if (levelReader instanceof Level level && level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity
                    && pipeBlockEntity.getCamouflageBlockState() != null) {
                pipeBlockEntity.markRenderDirty();
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            if (!player.isCreative()) {
                if (pipeBlockEntity.getCamouflageBlockState() != null) {
                    ItemStack itemStack = pipeBlockEntity.getCamouflageBlockState().getBlock().asItem().getDefaultInstance();
                    ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY() + .5, pos.getZ(), itemStack);
                    level.addFreshEntity(itemEntity);
                    itemEntity.setPos(player.getX(), player.getY(), player.getZ());
                }
                for (AbstractFullPipe pipe : pipeBlockEntity.getPipes()) {
                    if (pipe instanceof AbstractCapFullPipe abstractCapFullPipe && abstractCapFullPipe.getSpeedUpgradeCount() > 0) {
                        Item item = Registration.SPEED_UPGRADE_ITEM.get();
                        ItemStack itemStack = item.getDefaultInstance();
                        itemStack.setCount(abstractCapFullPipe.getSpeedUpgradeCount());
                        ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY() + .5, pos.getZ(), itemStack);
                        level.addFreshEntity(itemEntity);
                        if (player != null)
                            itemEntity.setPos(player.getX(), player.getY(), player.getZ());
                    }

                    Item item = Registry.getFullPipeItemFromClass(pipe.getClass());
                    ItemStack itemStack = item.getDefaultInstance();
                    ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY() + .5, pos.getZ(), itemStack);
                    level.addFreshEntity(itemEntity);
                    if (player != null)
                        itemEntity.setPos(player.getX(), player.getY(), player.getZ());
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            if (pipeBlockEntity.getPipe(3) instanceof RedstonePipe pipe)
                return pipe.getStrongRsOutput(direction.getOpposite());
        }
        return super.getDirectSignal(state, level, pos, direction);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            if (pipeBlockEntity.getPipe(3) instanceof RedstonePipe pipe)
                return pipe.getWeakRsOutput(direction.getOpposite());
        }
        return super.getSignal(state, level, pos, direction);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
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

    @Override
    public InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            ItemStack heldStack = player.getMainHandItem();
            if (Registry.getFullPipeClassFromItem(heldStack.getItem()) != null) {
                AbstractFullPipe pipe = pipeBlockEntity.addPipe(heldStack);
                if (pipe!=null) {
                    if (!player.isCreative())
                        heldStack.setCount(heldStack.getCount()-1);
                    pipe.autoConnectOnPlace(pipeBlockEntity);
                    return InteractionResult.CONSUME;
                }
            } else if (heldStack.is(WRENCH_TAG)) {
                if (player.getOffhandItem().getItem() instanceof BlockItem blockItem){
                    BlockState blockState1 = blockItem.getBlock().getStateForPlacement(new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,player.getOffhandItem(),hitResult));
                    boolean isFullBlock = blockState1.isCollisionShapeFullBlock(level, pos);
                    if (isFullBlock && !blockState1.hasBlockEntity()) {
                        pipeBlockEntity.setCamouflage(blockItem.getBlock().getStateForPlacement(new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, player.getOffhandItem(), hitResult)));
                        if (!player.isCreative()) {
                            player.getOffhandItem().setCount(player.getOffhandItem().getCount() - 1);
                        }
                    }
                    return InteractionResult.CONSUME;
                }
                PipeSide pipeSide = pipeBlockEntity.getPipeAtHitVector(hitResult);
                if (pipeSide!=null){
                    pipeSide.toggleSideStatus(pipeBlockEntity);
                }
                return InteractionResult.CONSUME;
            } else if (heldStack.getItem() instanceof DyeItem dyeItem) {
                PipeSide pipeSide = pipeBlockEntity.getPipeAtHitVector(hitResult);
                if (pipeSide!=null && pipeSide.getPipe() instanceof RedstonePipe redstonePipe){
                    // set the new frequency signal and update the pipe block regarding the old frequency signal in the pipe network
                    redstonePipe.setColor(pipeBlockEntity, pipeSide.getDirection(), dyeItem.getDyeColor().getId());
                    // update the pipe block regarding the new frequency signal in the pipe network
                    redstonePipe.neighborChanged(pipeBlockEntity, pipeSide.getDirection());
                    level.blockUpdated(pos,this);
                }
                return InteractionResult.CONSUME;
            } else if (heldStack.getItem() instanceof SpeedUpgradeItem) {
                PipeSide pipeSide = pipeBlockEntity.getPipeAtHitVector(hitResult);
                if (pipeSide.applySpeedUpgrade() && !player.isCreative()
                ){
                    heldStack.setCount(heldStack.getCount()-1);
                    return InteractionResult.CONSUME;
                }
            } else {
                PipeSide pipeSide = pipeBlockEntity.getPipeAtHitVector(hitResult);
                if (pipeSide!=null) {
                    pipeSide.getPipe().openGUI(pipeBlockEntity,player);
                    return InteractionResult.CONSUME;
                }
            }
        }
        return super.useWithoutItem(blockState, level, pos, player, hitResult);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            ItemStack heldStack = player.getMainHandItem();
            if (heldStack.is(WRENCH_TAG) || Registry.getFullPipeClassFromItem(heldStack.getItem()) != null) {
                if (pipeBlockEntity.getCamouflageBlockState() != null) {
                    ItemStack itemStack = pipeBlockEntity.getCamouflageBlockState().getBlock().asItem().getDefaultInstance();
                    pipeBlockEntity.setCamouflage(null);
                    if (!player.isCreative()) {
                        ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY() + .5, pos.getZ(), itemStack);
                        level.addFreshEntity(itemEntity);
                        itemEntity.setPos(player.getX(), player.getY(), player.getZ());
                    }
                } else {
                    PipeSide pipeSide = pipeBlockEntity.getPipeAtHitVector(PipeBlockEntity.getPlayerCollisionHitResult(player, level));
                    if (pipeSide != null) {
                        if (player.isCrouching()) {
                            pipeSide.getPipe().openGUI(pipeBlockEntity, player);
                        } else if (pipeSide.removeSpeedUpgrade()) {
                            if (!player.isCreative()) {
                                Item item = Registration.SPEED_UPGRADE_ITEM.get();
                                ItemStack itemStack = item.getDefaultInstance();
                                ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY() + .5, pos.getZ(), itemStack);
                                level.addFreshEntity(itemEntity);
                                itemEntity.setPos(player.getX(), player.getY(), player.getZ());
                            }
                        } else if (pipeSide.removePipe() && !player.isCreative()) {
                            Item item = Registry.getFullPipeItemFromClass(pipeSide.getPipe().getClass());
                            ItemStack itemStack = item.getDefaultInstance();
                            ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY() + .5, pos.getZ(), itemStack);
                            level.addFreshEntity(itemEntity);
                            itemEntity.setPos(player.getX(), player.getY(), player.getZ());
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            BlockState camouflage = pipeBlockEntity.getCamouflageBlockState();
            if (camouflage!=null)
                return Shapes.block();

            AbstractFullPipe[] pipes = pipeBlockEntity.getPipes();
            boolean single = pipes.length == 1;
            VoxelShape shape = (single)
                    ?Block.box(6.4, 6.4, 6.4, 9.6, 9.6, 9.6)
                    :Block.box(5, 5, 5, 11, 11, 11);

            for (AbstractFullPipe pipe : pipes) {
                if(pipe.getPipeSideStatus(Direction.NORTH)!= PipeConnectionState.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(6.875, 6.875, 0, 9.125, 9.125, 6.875))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(5.75, 8, 0, 8, 10.25, 5))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(8, 8, 0, 10.25, 10.25, 5))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(5.75, 5.75, 0, 8, 8, 5))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(8, 5.75, 0, 10.25, 8, 5))
                            :Shapes.or(shape,Block.box(5.75, 5.75, 0, 10.25, 10.25, 5));
                }
                if(pipe.getPipeSideStatus(Direction.SOUTH)!= PipeConnectionState.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(6.875, 6.875, 9.125, 9.125, 9.125, 16))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(5.75, 8, 11, 8, 10.25, 16))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(8, 8, 11, 10.25, 10.25, 16))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(5.75, 5.75, 11, 8, 8, 16))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(8, 5.75, 11, 10.25, 8, 16))
                            :Shapes.or(shape,Block.box(5.75, 5.75, 11, 10.25, 10.25, 16));
                }
                if(pipe.getPipeSideStatus(Direction.EAST)!= PipeConnectionState.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(9.125, 6.875, 6.875, 16, 9.125, 9.125))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(11, 8, 8, 16, 10.25, 10.25))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(11, 8, 5.75, 16, 10.25, 8))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(11, 5.75, 8, 16, 8, 10.25))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(11, 5.75, 5.75, 16, 8, 8))
                            :Shapes.or(shape,Block.box(11, 5.75, 5.75, 16, 10.25, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.WEST)!= PipeConnectionState.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(0, 6.875, 6.875, 6.875, 9.125, 9.125))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(0, 8, 8, 5, 10.25, 10.25))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(0, 8, 5.75, 5, 10.25, 8))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(0, 5.75, 8, 5, 8, 10.25))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(0, 5.75, 5.75, 5, 8, 8))
                            :Shapes.or(shape,Block.box(0, 5.75, 5.75, 5, 10.25, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.UP)!= PipeConnectionState.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(6.875, 9.125, 6.875, 9.125, 16, 9.125))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(8, 11,8, 10.25, 16,10.25))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(5.75, 11,8, 8, 16, 10.25))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(8, 11,5.75, 10.25, 16, 8))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(5.75, 11,5.75, 8, 16, 8))
                            :Shapes.or(shape,Block.box(5.75, 11, 5.75, 10.25, 16, 10.25));
                }
                if(pipe.getPipeSideStatus(Direction.DOWN)!= PipeConnectionState.DISABLED){
                    shape = (single)
                            ?Shapes.or(shape,Block.box(6.875, 0, 6.875, 9.125, 6.875, 9.125))
                            :(pipe.slotPos()==0)?Shapes.or(shape,Block.box(8, 0,8, 10.25, 5,10.25))
                            :(pipe.slotPos()==1)?Shapes.or(shape,Block.box(5.75, 0,8, 8, 5, 10.25))
                            :(pipe.slotPos()==2)?Shapes.or(shape,Block.box(8, 0,5.75, 10.25, 5, 8))
                            :(pipe.slotPos()==3)?Shapes.or(shape,Block.box(5.75, 0,5.75, 8, 5, 8))
                            :Shapes.or(shape,Block.box(5.75, 0, 5.75, 10.25, 5, 10.25));
                }
            }
            return shape;
        }
        return super.getShape(state, level, pos, context);
    }
}