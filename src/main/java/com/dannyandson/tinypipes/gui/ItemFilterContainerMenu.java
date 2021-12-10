package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.components.IFilterPipe;
import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.*;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FishBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ItemFilterContainerMenu extends Container {

    //factories
    public static ItemFilterContainerMenu createMenu(int containerId, PlayerInventory playerInventory) {
        return new ItemFilterContainerMenu(containerId,playerInventory,new Inventory(18));
    }
     public static ItemFilterContainerMenu createMenu(int containerId, PlayerInventory playerInventory, IInventory container) {
        return new ItemFilterContainerMenu(containerId,playerInventory,container);
    }

    private final IInventory container;

    protected ItemFilterContainerMenu(int containerId, PlayerInventory playerInventory, IInventory container){
        this(containerId,playerInventory,container,Registration.ITEM_FILTER_MENU_TYPE.get());
    }

    protected ItemFilterContainerMenu(int containerId, PlayerInventory playerInventory, IInventory container, @Nullable ContainerType<?> p_i50105_1_) {
        super(p_i50105_1_,containerId);
        this.container=container;

        int leftCol = 12;
        int ySize = ItemFilterGUI.HEIGHT;

        //filter slots
        for(int i = 0; i<container.getContainerSize(); i++){
            this.addSlot(new Slot(container, i, leftCol+(i%9)*18, Math.floorDiv(i,9)*18 + 20));
        }

        //player inventory slots
        for (int playerInvRow = 0; playerInvRow < 3; playerInvRow++) {
            for (int playerInvCol = 0; playerInvCol < 9; playerInvCol++) {
                this.addSlot(new Slot(playerInventory, playerInvCol + playerInvRow * 9 + 9, leftCol + playerInvCol * 18, ySize - (4 - playerInvRow) * 18 - 10));
            }

        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            this.addSlot(new Slot(playerInventory, hotbarSlot, leftCol + hotbarSlot * 18, ySize - 24));
        }

    }

    @Override
    public void slotsChanged(IInventory p_38868_) {
        container.setChanged();
        super.slotsChanged(p_38868_);
    }

    @Override
    public boolean stillValid(PlayerEntity p_38874_) {
        return this.container.stillValid(p_38874_);
    }

    @CheckForNull
    public IFilterPipe getIFilterPipe(){
        if (this.container instanceof IFilterPipe)
            return (IFilterPipe) this.container;
        return null;
    }

    @Override
    public ItemStack clicked(int slot, int button, ClickType clickType, PlayerEntity player) {
        if (slot>=0 && slot<container.getContainerSize()) {
            ItemStack carriedStack = player.inventory.getCarried();
            if (!carriedStack.getItem().equals(Items.AIR) && !carriedStack.equals(ItemStack.EMPTY) &&
                    (!(this instanceof FluidFilterContainerMenu)||
                            (carriedStack.getItem() instanceof BucketItem &&
                                    !((BucketItem) carriedStack.getItem()).getFluid().equals(Fluids.EMPTY) &&
                                    !(carriedStack.getItem() instanceof FishBucketItem)))
            ) {
                boolean exists = false;
                for(int i = 0 ; i<container.getContainerSize() ; i++) {
                    if (container.getItem(i).getItem().equals(carriedStack.getItem())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    ItemStack filterStack = carriedStack.copy();
                    filterStack.setCount(1);
                    container.setItem(slot, filterStack);
                }
            }else
            {
                container.removeItemNoUpdate(slot);
            }
            return carriedStack;
        }

        return super.clicked(slot, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        Slot slot = this.slots.get(index);
        if (slot != null && index>=container.getContainerSize() && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            boolean exists = false;
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).getItem().equals(itemstack1.getItem())) {
                    exists = true;
                    break;
                }
            }

            if (!exists && (!(this instanceof FluidFilterContainerMenu)||
                    (itemstack1.getItem() instanceof BucketItem && !((BucketItem) itemstack1.getItem()).getFluid().equals(Fluids.EMPTY) && !(itemstack1.getItem() instanceof FishBucketItem))))
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (container.getItem(i).equals(ItemStack.EMPTY)) {
                        ItemStack filterStack = itemstack1.copy();
                        filterStack.setCount(1);
                        container.setItem(i, filterStack);
                        break;
                    }
                }
        }

        return ItemStack.EMPTY;
    }


    public static class Provider implements INamedContainerProvider {
        private IInventory container;
        private static TextComponent displayNameComponent = new TranslationTextComponent("tinypipes:item_filter");

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
            return ItemFilterContainerMenu.createMenu(containerId,inventory,container);
        }
    }
}
