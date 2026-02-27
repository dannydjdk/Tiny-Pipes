package com.dannyandson.tinypipes.items;

import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PipeWrenchItem extends DiggerItem {

    public PipeWrenchItem() {
        super(Tiers.WOOD, Registration.MINEABLE_WITH_WRENCH, new Properties());
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> list, @NotNull TooltipFlag flags) {
        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("message." + this.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA));
        } else
            list.add(Component.translatable("tinypipes.tooltip.press_shift").withStyle(ChatFormatting.DARK_GRAY));
    }
}
