package com.dannyandson.tinypipes.items;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class PipeWrenchItem extends Item {

    public PipeWrenchItem(Item.Properties props) {
        super(props);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> textConsumer, TooltipFlag flags) {
        if (isShiftKeyDown()) {
            textConsumer.accept(Component.translatable("message." + this.getDescriptionId()).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA)));
        } else
            textConsumer.accept(Component.translatable("tinypipes.tooltip.press_shift").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
    }

    private static boolean isShiftKeyDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}