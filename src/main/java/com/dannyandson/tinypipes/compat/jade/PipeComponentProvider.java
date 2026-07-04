package com.dannyandson.tinypipes.compat.jade;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.components.full.PipeSide;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Jade tooltip content and header icon for full-size pipe blocks. Resolves the exact pipe + end the
 * crosshair is on, replaces the block name with the pipe's type, and lists its overlay details.
 */
public enum PipeComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(TinyPipes.MODID, "pipe");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    // Resolve which pipe (and end) the crosshair is on, or null when it isn't a pipe-slot hit.
    private static PipeSide resolve(BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof PipeBlockEntity pipeBlockEntity) {
            BlockHitResult hit = accessor.getHitResult();
            return pipeBlockEntity.getPipeAtHitVector(hit);
        }
        return null;
    }

    private static ItemStack pipeStack(AbstractFullPipe pipe) {
        Item item = Registry.getFullPipeItemFromClass(pipe.getClass());
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        PipeSide pipeSide = resolve(accessor);
        if (pipeSide == null) return;

        AbstractFullPipe pipe = pipeSide.getPipe();
        ItemStack itemStack = pipeStack(pipe);
        if (!itemStack.isEmpty())
            tooltip.replace(JadeIds.CORE_OBJECT_NAME, itemStack.getHoverName());

        List<Component> lines = new ArrayList<>();
        pipe.appendOverlayInfo(lines, pipeSide.getPipeBlockEntity(), pipeSide.getDirection());
        for (Component line : lines)
            tooltip.add(line);
    }

    @Override
    public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
        PipeSide pipeSide = resolve(accessor);
        if (pipeSide == null) return currentIcon;
        ItemStack itemStack = pipeStack(pipeSide.getPipe());
        return itemStack.isEmpty() ? currentIcon : IElementHelper.get().item(itemStack);
    }
}