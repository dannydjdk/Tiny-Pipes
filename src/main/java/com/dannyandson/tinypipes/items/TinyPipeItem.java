package com.dannyandson.tinypipes.items;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinyredstone.api.AbstractPanelCellItem;
import com.dannyandson.tinyredstone.blocks.PanelBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class TinyPipeItem extends AbstractPanelCellItem {

    public TinyPipeItem() {
        super(new Properties().tab(TinyPipes.ITEM_GROUP));
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {
        return player.level.getBlockState(pos).getBlock() instanceof PanelBlock;
    }

    @Override
    public  void  appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flags)
    {
        if (Screen.hasShiftDown()) {
            list.add(new TranslatableComponent("message.item.tiny_pipe").withStyle(ChatFormatting.GRAY));
            list.add(new TranslatableComponent("message." + this.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA));
        } else
            list.add(new TranslatableComponent("tinyredstone.tooltip.press_shift").withStyle(ChatFormatting.DARK_GRAY));    }
}
