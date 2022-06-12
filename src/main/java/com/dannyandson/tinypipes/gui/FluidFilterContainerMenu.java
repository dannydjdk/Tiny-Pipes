package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

public class FluidFilterContainerMenu extends ItemFilterContainerMenu{
    public static FluidFilterContainerMenu createFluidMenu(int containerId, Inventory playerInventory) {
        return new FluidFilterContainerMenu(containerId,playerInventory,new SimpleContainer(18));
    }
    public static ItemFilterContainerMenu createFluidMenu(int containerId, Inventory playerInventory, Container container) {
        return new FluidFilterContainerMenu(containerId,playerInventory,container);
    }

    protected FluidFilterContainerMenu(int containerId, Inventory playerInventory, Container container) {
        super(containerId, playerInventory, container, Registration.FLUID_FILTER_MENU_TYPE.get());
    }

    public static class Provider implements MenuProvider {
        private Container container;
        private static Component displayNameComponent = Component.translatable("tinypipes:fluid_filter");

        public Provider(Container container){
            this.container=container;
        }

        @Override
        public Component getDisplayName() {
            return displayNameComponent;
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return FluidFilterContainerMenu.createFluidMenu(containerId,inventory,container);
        }
    }
}
