package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.components.ItemFilterPipe;
import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ItemFilterContainerMenu extends AbstractContainerMenu {

    //factories
    public static ItemFilterContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new ItemFilterContainerMenu(containerId,playerInventory,new SimpleContainer(18));
    }
    public static ItemFilterContainerMenu createMenu(int containerId, Inventory playerInventory, Container container) {
        return new ItemFilterContainerMenu(containerId,playerInventory,container);
    }

    private final Container container;

    protected ItemFilterContainerMenu(int containerId, Inventory playerInventory, Container container) {
        super(Registration.ITEM_FILTER_MENU_TYPE.get(),containerId);
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
    public void slotsChanged(Container p_38868_) {
        container.setChanged();
        super.slotsChanged(p_38868_);
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return this.container.stillValid(p_38874_);
    }

    @CheckForNull
    public ItemFilterPipe getItemFilterPipe(){
        if (this.container instanceof ItemFilterPipe itemFilterPipe)
            return itemFilterPipe;
        return null;
    }


    @Override
    public void clicked(int slot, int button, ClickType clickType, Player player) {
        if (slot>=0 && slot<container.getContainerSize()) {
            ItemStack carriedStack = getCarried();
            if (!carriedStack.getItem().equals(Items.AIR) && !carriedStack.equals(ItemStack.EMPTY)) {
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
        } else {
            super.clicked(slot, button, clickType, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (index<=container.getContainerSize() && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            boolean exists = false;
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).getItem().equals(itemstack1.getItem())) {
                    exists = true;
                    break;
                }
            }

            if (!exists)
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


    public static class Provider implements MenuProvider{
        private Container container;
        private static Component displayNameComponent = new TranslatableComponent("tinypipes:item_filter");

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
            return ItemFilterContainerMenu.createMenu(containerId,inventory,container);
        }
    }
}
