package com.dannyandson.tinypipes.items;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class PipeWrenchItem extends DiggerItem {

    public PipeWrenchItem() {
        super(0, 0F, Tiers.WOOD, Registration.MINEABLE_WITH_WRENCH, new Properties().tab(TinyPipes.ITEM_GROUP));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> list, @NotNull TooltipFlag flags) {
        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("message." + this.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA));
        } else
            list.add(Component.translatable("tinypipes.tooltip.press_shift").withStyle(ChatFormatting.DARK_GRAY));
    }

}
