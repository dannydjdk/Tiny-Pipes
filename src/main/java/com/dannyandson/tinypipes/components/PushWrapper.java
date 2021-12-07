package com.dannyandson.tinypipes.components;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

public class PushWrapper {
    private SortedSet<PushBlockEntity> pushBlockEntities = new TreeSet<>();

    private static final AtomicLong NEXT_ID = new AtomicLong(0);
    private final long id = NEXT_ID.getAndIncrement();
    public long getId() {
        return id;
    }

    public void addBlockEntity(TileEntity blockEntity, int distance, Direction side){
        addBlockEntity(blockEntity, distance, 0,side);
    }

    public void addBlockEntity(TileEntity blockEntity, int distance, int priority, Direction side){
        pushBlockEntities.add(new PushBlockEntity(blockEntity, distance,priority,side));
    }

    public Set<PushBlockEntity> getSortedBlockEntities() {
        return pushBlockEntities;
    }

    public static class PushBlockEntity implements Comparable<PushBlockEntity>{
        private final TileEntity blockEntity;
        private final Direction side;
        private final int distance;
        private final int priority;

        private PushBlockEntity(TileEntity blockEntity, int distance, int priority, Direction side){
            this.blockEntity=blockEntity;
            this.distance=distance;
            this.priority=priority;
            this.side=side;
        }

        public TileEntity getBlockEntity() {
            return blockEntity;
        }

        public Direction getSide() {
            return side;
        }

        private IItemHandler iItemHandler = null;
        private boolean iItemHandlerChecked = false;
        public IItemHandler getIItemHandler()
        {
            if (!iItemHandlerChecked){
                iItemHandler = blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null);
                iItemHandlerChecked=true;
            }
            return iItemHandler;
        }

        private IFluidHandler iFluidHandler = null;
        private boolean iFluidHandlerChecked = false;
        public IFluidHandler getIFluidHandler()
        {
            if (!iFluidHandlerChecked){
                iFluidHandler = blockEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side).orElse(null);
                iFluidHandlerChecked=true;
            }
            return iFluidHandler;
        }

        private IEnergyStorage iEnergyStorage = null;
        private boolean iEnergyStorageChecked = false;
        public IEnergyStorage getIEnergyStorage()
        {
            if (!iEnergyStorageChecked){
                iEnergyStorage = blockEntity.getCapability(CapabilityEnergy.ENERGY, side).orElse(null);
                iEnergyStorageChecked=true;
            }
            return iEnergyStorage;
        }

        /**
         * Compares this object with the specified object for order.
         *
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object
         * is less than, equal to, or greater than the specified object.
         * @throws NullPointerException if the specified object is null
         * @throws ClassCastException   if the specified object's type prevents it
         *                              from being compared to this object.
         */
        @Override
        public int compareTo(PushBlockEntity o) {
            if (this.priority!=o.priority)
                return o.priority-this.priority;
            return this.distance-o.distance;
        }
    }
}
