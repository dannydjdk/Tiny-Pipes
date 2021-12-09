package com.dannyandson.tinypipes.components;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
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

    public void addBlockEntity(BlockEntity blockEntity, int distance, Direction side){
        addBlockEntity(blockEntity, distance, 0,side);
    }

    public void addBlockEntity(BlockEntity blockEntity, int distance, int priority, Direction side){
        pushBlockEntities.add(new PushBlockEntity(blockEntity, distance,priority,side));
    }

    public Set<PushBlockEntity> getSortedBlockEntities() {
        return pushBlockEntities;
    }

    public static class PushBlockEntity implements Comparable<PushBlockEntity>{
        private final BlockEntity blockEntity;
        private final Direction side;
        private final int distance;
        private final int priority;

        private PushBlockEntity(BlockEntity blockEntity, int distance, int priority, Direction side){
            this.blockEntity=blockEntity;
            this.distance=distance;
            this.priority=priority;
            this.side=side;
        }

        public BlockEntity getBlockEntity() {
            return blockEntity;
        }

        public Direction getSide() {
            return side;
        }

        public IItemHandler getIItemHandler()
        {
             return ModCapabilityManager.getItemHandler(blockEntity.getLevel(),blockEntity.getBlockPos(),side,blockEntity);
        }

        public IFluidHandler getIFluidHandler()
        {
            return ModCapabilityManager.getIFluidHandler(blockEntity.getLevel(),blockEntity.getBlockPos(),side,blockEntity);
        }

        public IEnergyStorage getIEnergyStorage()
        {
            return ModCapabilityManager.getIEnergyStorage(blockEntity.getLevel(),blockEntity.getBlockPos(),side,blockEntity);
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
