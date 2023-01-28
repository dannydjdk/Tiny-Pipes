package com.dannyandson.tinypipes.items;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class FullPipeItem extends Item {

    public FullPipeItem() {
        super(new Properties().tab(TinyPipes.ITEM_GROUP));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        //if using on a PipeBlock that does not contain this item's pipe, put it in there
        if (context.getLevel().getBlockEntity(context.getClickedPos().offset(context.getClickedFace().getNormal())) instanceof PipeBlockEntity panelTile && context.getPlayer() != null) {
            InteractionResult result = Registration.PIPE_BLOCK.get().use(panelTile.getBlockState(), context.getLevel(), panelTile.getBlockPos(), context.getPlayer(), context.getHand(), PipeBlockEntity.getPlayerCollisionHitResult(context.getPlayer(),context.getLevel()));
            if (result==InteractionResult.CONSUME)
                return result;
        }
        //otherwise, place it in world
        BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
        BlockState placeState = context.getLevel().getBlockState(placePos);
        if (placeState.getMaterial().isReplaceable()) {
            context.getLevel().setBlock(placePos, Registration.PIPE_BLOCK.get().defaultBlockState(), 2);
            if (context.getLevel().getBlockEntity(context.getClickedPos().offset(context.getClickedFace().getNormal())) instanceof PipeBlockEntity pipeBlockEntity && context.getPlayer() != null) {
                Registration.PIPE_BLOCK.get().use(pipeBlockEntity.getBlockState(), context.getLevel(), pipeBlockEntity.getBlockPos(), context.getPlayer(), context.getHand(), PipeBlockEntity.getPlayerCollisionHitResult(context.getPlayer(), context.getLevel()));
            }
        }
         return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flags) {
        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("message." + this.getDescriptionId().replaceAll("full_","")).withStyle(ChatFormatting.DARK_AQUA));
        } else
            list.add(Component.translatable("tinypipes.tooltip.press_shift").withStyle(ChatFormatting.DARK_GRAY));
    }

}
