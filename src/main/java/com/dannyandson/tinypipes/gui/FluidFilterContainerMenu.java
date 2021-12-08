package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class FluidFilterContainerMenu extends ItemFilterContainerMenu{
    public static FluidFilterContainerMenu createFluidMenu(int containerId, PlayerInventory playerInventory) {
        return new FluidFilterContainerMenu(containerId,playerInventory,new Inventory(18){
            @Override
            public boolean canAddItem(ItemStack itemStack) {
                return itemStack.getItem() instanceof BucketItem &&  super.canAddItem(itemStack);
            }
        });
    }
    public static ItemFilterContainerMenu createFluidMenu(int containerId, PlayerInventory playerInventory, IInventory container) {
        return new FluidFilterContainerMenu(containerId,playerInventory,container);
    }

    protected FluidFilterContainerMenu(int containerId, PlayerInventory playerInventory, IInventory container) {
        super(containerId, playerInventory, container, Registration.FLUID_FILTER_MENU_TYPE.get());
    }

    public static class Provider implements INamedContainerProvider {
        private IInventory container;
        private static TextComponent displayNameComponent = new TranslationTextComponent("tinypipes:fluid_filter");

        public Provider(IInventory container){
            this.container=container;
        }

        @Override
        public ITextComponent getDisplayName() {
            return displayNameComponent;
        }

        @Nullable
        @Override
        public Container createMenu(int containerId, PlayerInventory inventory, PlayerEntity player) {
            return FluidFilterContainerMenu.createFluidMenu(containerId,inventory,container);
        }
    }
}
