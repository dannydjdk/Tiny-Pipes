package com.dannyandson.tinypipes.items;

import com.dannyandson.tinypipes.Config;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class SpeedUpgradeItem extends Item {

    public SpeedUpgradeItem(Item.Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> textConsumer, TooltipFlag flags) {
        if (isShiftKeyDown()) {
            String message = Component.translatable("message." + this.getDescriptionId()).getString()
                    .replaceFirst("_speed_upgrade_multiplier_", Config.SPEED_UPGRADE_MULTIPLIER.get().toString())
                    .replaceFirst("_speed_upgrade_max_",Config.SPEED_UPGRADE_MAX.get().toString());
            textConsumer.accept(Component.translatable(message).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA)));
        } else
            textConsumer.accept(Component.translatable("tinypipes.tooltip.press_shift").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
    }

    private static boolean isShiftKeyDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}