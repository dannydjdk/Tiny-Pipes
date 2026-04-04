package com.dannyandson.tinypipes.caphandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jspecify.annotations.Nullable;

public class ModCapabilityManager {

    public static IItemHandler getItemHandler(Level world, BlockPos pos, Direction side) {
        return getItemHandler(world, pos, side, null);
    }
    @SuppressWarnings("deprecation")
    public static IItemHandler getItemHandler(Level world, BlockPos pos, Direction side, @Nullable BlockEntity blockEntity) {
        var handler = world.getCapability(Capabilities.Item.BLOCK, pos, side);
        return handler == null ? null : IItemHandler.of(handler);
    }

    public static IFluidHandler getIFluidHandler(Level world, BlockPos pos, Direction side) {
        return getIFluidHandler(world, pos, side, null);
    }
    @SuppressWarnings("deprecation")
    public static IFluidHandler getIFluidHandler(Level world, BlockPos pos, Direction side, BlockEntity blockEntity) {
        var handler = world.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        return handler == null ? null : IFluidHandler.of(handler);
    }

    public static IEnergyStorage getIEnergyStorage(Level world, BlockPos pos, Direction side) {
        return getIEnergyStorage(world, pos, side, null);
    }
    @SuppressWarnings("deprecation")
    public static IEnergyStorage getIEnergyStorage(Level world, BlockPos pos, Direction side, BlockEntity blockEntity) {
        var handler = world.getCapability(Capabilities.Energy.BLOCK, pos, side);
        return handler == null ? null : IEnergyStorage.of(handler);
    }
}
