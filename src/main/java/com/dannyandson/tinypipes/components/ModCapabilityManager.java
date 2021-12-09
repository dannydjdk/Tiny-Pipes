package com.dannyandson.tinypipes.components;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class ModCapabilityManager {

    private static final Map<Level, Map<BlockPos, Map<Direction, IItemHandler>>> iItemHandlerCache = new HashMap<>();
    private static final Map<Level, Map<BlockPos, Map<Direction, IFluidHandler>>> iFluidHandlerCache = new HashMap<>();
    private static final Map<Level, Map<BlockPos, Map<Direction, IEnergyStorage>>> iEnergyStorageCache = new HashMap<>();

    public static IItemHandler getItemHandler(Level world, BlockPos pos, Direction side) {
        return getItemHandler(world, pos, side,null);
    }
    public static IItemHandler getItemHandler(Level world, BlockPos pos, Direction side, @Nullable BlockEntity blockEntity) {

        if (!iItemHandlerCache.containsKey(world))
            iItemHandlerCache.put(world, new HashMap<>());
        if (!iItemHandlerCache.get(world).containsKey(pos))
            iItemHandlerCache.get(world).put(pos, new EnumMap<>(Direction.class));
        if (!iItemHandlerCache.get(world).get(pos).containsKey(side)) {
            blockEntity = (blockEntity==null)?world.getBlockEntity(pos):blockEntity;
            if (blockEntity!=null) {
                Capability<IItemHandler> iItemHandlerCapability = CapabilityManager.get(new CapabilityToken<>() {
                });
                LazyOptional<IItemHandler> capability = blockEntity.getCapability(iItemHandlerCapability, side);
                iItemHandlerCache.get(world).get(pos).put(side, capability.orElse(null));
                capability.addListener(self -> clearItemCapability(world, pos, side));
            } else
                return null;
        }

        return iItemHandlerCache.get(world).get(pos).get(side);
    }

    private static void clearItemCapability(Level world, BlockPos pos, Direction side) {
        if (
                iItemHandlerCache.containsKey(world) &&
                        iItemHandlerCache.get(world).containsKey(pos)
        )
            iItemHandlerCache.get(world).get(pos).remove(side);
    }

    public static IFluidHandler getIFluidHandler(Level world, BlockPos pos, Direction side) {
        return getIFluidHandler(world, pos, side, null);
    }
    public static IFluidHandler getIFluidHandler(Level world, BlockPos pos, Direction side, BlockEntity blockEntity) {
        if (!iFluidHandlerCache.containsKey(world))
            iFluidHandlerCache.put(world, new HashMap<>());
        if (!iFluidHandlerCache.get(world).containsKey(pos))
            iFluidHandlerCache.get(world).put(pos, new EnumMap<>(Direction.class));
        if (!iFluidHandlerCache.get(world).get(pos).containsKey(side)) {
            blockEntity = (blockEntity==null)?world.getBlockEntity(pos):blockEntity;
            if (blockEntity != null) {
                Capability<IFluidHandler> iFluidHandlerCapability = CapabilityManager.get(new CapabilityToken<>() {
                });
                LazyOptional<IFluidHandler> capability = blockEntity.getCapability(iFluidHandlerCapability, side);
                iFluidHandlerCache.get(world).get(pos).put(side, capability.orElse(null));
                capability.addListener(self -> clearFluidCapability(world, pos, side));
            } else
                return null;
        }

        return iFluidHandlerCache.get(world).get(pos).get(side);
    }

    private static void clearFluidCapability(Level world, BlockPos pos, Direction side) {
        if (
                iFluidHandlerCache.containsKey(world) &&
                        iFluidHandlerCache.get(world).containsKey(pos)
        )
            iFluidHandlerCache.get(world).get(pos).remove(side);
    }

    public static IEnergyStorage getIEnergyStorage(Level world, BlockPos pos, Direction side) {
        return getIEnergyStorage(world, pos, side, null);
    }
    public static IEnergyStorage getIEnergyStorage(Level world, BlockPos pos, Direction side, BlockEntity blockEntity) {
        if (!iEnergyStorageCache.containsKey(world))
            iEnergyStorageCache.put(world, new HashMap<>());
        if (!iEnergyStorageCache.get(world).containsKey(pos))
            iEnergyStorageCache.get(world).put(pos, new EnumMap<>(Direction.class));
        if (!iEnergyStorageCache.get(world).get(pos).containsKey(side)) {
            blockEntity = (blockEntity==null)?world.getBlockEntity(pos):blockEntity;
            if (blockEntity != null) {
                Capability<IEnergyStorage> iItemHandlerCapability = CapabilityManager.get(new CapabilityToken<>() {
                });
                LazyOptional<IEnergyStorage> capability = blockEntity.getCapability(iItemHandlerCapability, side);
                iEnergyStorageCache.get(world).get(pos).put(side, capability.orElse(null));
                capability.addListener(self -> clearEnergyCapability(world, pos, side));
            } else
                return null;
        }

        return iEnergyStorageCache.get(world).get(pos).get(side);
    }

    private static void clearEnergyCapability(Level world, BlockPos pos, Direction side) {
        if (
                iEnergyStorageCache.containsKey(world) &&
                        iEnergyStorageCache.get(world).containsKey(pos)
        )
            iEnergyStorageCache.get(world).get(pos).remove(side);
    }

}
