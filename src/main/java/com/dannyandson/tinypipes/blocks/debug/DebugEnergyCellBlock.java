package com.dannyandson.tinypipes.blocks.debug;

import com.dannyandson.tinypipes.setup.ModRegistration;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Debug-only finite energy cell. Right-click reports stored energy; sneak-right-click toggles between
 * empty and full, so you can stand up a full source and an empty sink for transfer-conservation tests.
 */
public class DebugEnergyCellBlock extends BaseEntityBlock {

    public static final MapCodec<DebugEnergyCellBlock> CODEC = simpleCodec(DebugEnergyCellBlock::new);

    public DebugEnergyCellBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DebugEnergyCellBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof DebugEnergyCellBlockEntity cell)) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            // toggle empty <-> full to set up a source (full) and a sink (empty)
            boolean fill = cell.getStored() < cell.getCapacity();
            cell.setStored(fill ? DebugEnergyCellBlockEntity.CAPACITY : 0);
            player.sendSystemMessage(Component.literal("[Debug Energy] " + (fill ? "FULL" : "EMPTY") + ": " + cell.getStored() + " FE"));
        } else {
            player.sendSystemMessage(Component.literal("[Debug Energy] " + cell.getStored() + " / " + cell.getCapacity() + " FE"));
        }
        return InteractionResult.CONSUME;
    }

    /** Registers the energy capability for the debug cell. Added to the mod bus only when the flag is on. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModRegistration.DEBUG_ENERGY_CELL_BE.get(),
                (be, side) -> be.getEnergy());
    }
}