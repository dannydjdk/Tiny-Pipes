package com.dannyandson.tinypipes.items;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.setup.ModRegistration;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class FullPipeItem extends Item {

    public FullPipeItem(Item.Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos().relative(context.getClickedFace())) instanceof PipeBlockEntity panelTile && context.getPlayer() != null) {
            InteractionResult result = ModRegistration.PIPE_BLOCK.get().useWithoutItem(panelTile.getBlockState(), context.getLevel(), panelTile.getBlockPos(), context.getPlayer(), PipeBlockEntity.getPlayerCollisionHitResult(context.getPlayer(),context.getLevel()));
            if (result==InteractionResult.CONSUME)
                return result;
        }
        BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
        BlockState placeState = context.getLevel().getBlockState(placePos);
        if (placeState.canBeReplaced()) {
            context.getLevel().setBlock(placePos, ModRegistration.PIPE_BLOCK.get().defaultBlockState(), 2);
            if (context.getLevel().getBlockEntity(context.getClickedPos().relative(context.getClickedFace())) instanceof PipeBlockEntity pipeBlockEntity && context.getPlayer() != null) {
                ModRegistration.PIPE_BLOCK.get().useWithoutItem(pipeBlockEntity.getBlockState(), context.getLevel(), pipeBlockEntity.getBlockPos(), context.getPlayer(), PipeBlockEntity.getPlayerCollisionHitResult(context.getPlayer(), context.getLevel()));
            }
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> textConsumer, TooltipFlag flags) {
        if (isShiftKeyDown()) {
            textConsumer.accept(Component.translatable("message." + this.getDescriptionId().replaceAll("full_","")).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA)));
        } else
            textConsumer.accept(Component.translatable("tinypipes.tooltip.press_shift").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
    }

    private static boolean isShiftKeyDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}