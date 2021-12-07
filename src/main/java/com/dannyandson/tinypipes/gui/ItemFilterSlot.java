package com.dannyandson.tinypipes.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

//TODO this may no longer be needed
public class ItemFilterSlot extends Slot {
    public ItemFilterSlot(Container p_40223_, int p_40224_, int p_40225_, int p_40226_) {
        super(p_40223_, p_40224_, p_40225_, p_40226_);
    }

    @Override
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

    @Override
    public ItemStack safeTake(int p_150648_, int p_150649_, Player p_150650_) {
        this.set(ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mayPickup(Player p_40228_) {
        ItemStack carried = p_40228_.containerMenu.getCarried();
        return carried.equals(ItemStack.EMPTY) || carried.getItem().equals(Items.AIR);
    }
}
