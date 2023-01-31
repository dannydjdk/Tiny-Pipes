package com.dannyandson.tinypipes.items;

import com.dannyandson.tinypipes.TinyPipes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class PipeWrenchItem extends Item {

    public PipeWrenchItem() {
        super(new Properties().tab(TinyPipes.ITEM_GROUP));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flags) {
        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("message." + this.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA));
        } else
            list.add(Component.translatable("tinypipes.tooltip.press_shift").withStyle(ChatFormatting.DARK_GRAY));
    }

}
