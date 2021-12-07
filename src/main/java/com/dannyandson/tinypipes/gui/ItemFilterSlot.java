package com.dannyandson.tinypipes.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ItemFilterSlot extends Slot {
    public ItemFilterSlot(IInventory p_40223_, int p_40224_, int p_40225_, int p_40226_) {
        super(p_40223_, p_40224_, p_40225_, p_40226_);
    }

    public ItemStack safeInsert(ItemStack itemStack, int slot) {
        if (itemStack.isEmpty())
            this.set(ItemStack.EMPTY);
        else {
            ItemStack itemStackCopy = itemStack.copy();
            itemStackCopy.setCount(1);
            this.set(itemStackCopy);
        }
        return itemStack;
    }

    public ItemStack safeTake(int p_150648_, int p_150649_, PlayerEntity p_150650_) {
        this.set(ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mayPickup(PlayerEntity p_40228_) {
        ItemStack carried = p_40228_.inventory.getCarried();
        return carried==null || carried.equals(ItemStack.EMPTY) || carried.getItem().equals(Items.AIR);
    }
}
