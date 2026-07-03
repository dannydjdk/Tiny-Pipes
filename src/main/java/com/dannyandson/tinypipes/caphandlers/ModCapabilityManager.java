package com.dannyandson.tinypipes.caphandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public class ModCapabilityManager {

    public static ResourceHandler<ItemResource> getItemHandler(Level world, BlockPos pos, Direction side) {
        return getItemHandler(world, pos, side, null);
    }
    public static ResourceHandler<ItemResource> getItemHandler(Level world, BlockPos pos, Direction side, @Nullable BlockEntity blockEntity) {
        return world.getCapability(Capabilities.Item.BLOCK, pos, side);
    }

    public static ResourceHandler<FluidResource> getIFluidHandler(Level world, BlockPos pos, Direction side) {
        return getIFluidHandler(world, pos, side, null);
    }
    public static ResourceHandler<FluidResource> getIFluidHandler(Level world, BlockPos pos, Direction side, BlockEntity blockEntity) {
        return world.getCapability(Capabilities.Fluid.BLOCK, pos, side);
    }

    public static EnergyHandler getIEnergyStorage(Level world, BlockPos pos, Direction side) {
        return getIEnergyStorage(world, pos, side, null);
    }
    public static EnergyHandler getIEnergyStorage(Level world, BlockPos pos, Direction side, BlockEntity blockEntity) {
        return world.getCapability(Capabilities.Energy.BLOCK, pos, side);
    }
}