package com.dannyandson.tinypipes.caphandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

public class ModCapabilityManager {

    public static IItemHandler getItemHandler(Level world, BlockPos pos, Direction side) {
        return getItemHandler(world, pos, side, null);
    }
    public static IItemHandler getItemHandler(Level world, BlockPos pos, Direction side, @Nullable BlockEntity blockEntity) {
        return world.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    public static IFluidHandler getIFluidHandler(Level world, BlockPos pos, Direction side) {
        return getIFluidHandler(world, pos, side, null);
    }
    public static IFluidHandler getIFluidHandler(Level world, BlockPos pos, Direction side, BlockEntity blockEntity) {
        return world.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    public static IEnergyStorage getIEnergyStorage(Level world, BlockPos pos, Direction side) {
        return getIEnergyStorage(world, pos, side, null);
    }
    public static IEnergyStorage getIEnergyStorage(Level world, BlockPos pos, Direction side, BlockEntity blockEntity) {
        return world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
    }
}
