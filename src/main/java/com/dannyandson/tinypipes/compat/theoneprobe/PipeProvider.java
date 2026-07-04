package com.dannyandson.tinypipes.compat.theoneprobe;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.blocks.PipeBlock;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.components.full.PipeSide;

import mcjty.theoneprobe.Tools;
import mcjty.theoneprobe.api.*;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The One Probe integration for full-size pipe blocks. Registered via IMC and only when TOP is present
 * (see {@code CompatHandler}), so this class never loads otherwise. Resolves the exact pipe + end the
 * player is looking at, overrides the header to the pipe's type, and lists its overlay details.
 */
public class PipeProvider implements Function<ITheOneProbe, Void>, IProbeInfoProvider, IBlockDisplayOverride {

    @Override
    public ResourceLocation getID() {
        return ResourceLocation.fromNamespaceAndPath(TinyPipes.MODID, "pipe");
    }

    @Override
    public Void apply(ITheOneProbe theOneProbe) {
        theOneProbe.registerProvider(this);
        theOneProbe.registerBlockDisplayOverride(this);
        return null;
    }

    // Resolve which pipe (and end) the probe ray landed on, or null when it isn't a pipe-slot hit.
    private static PipeSide resolve(Level level, IProbeHitData hitData) {
        if (level.getBlockEntity(hitData.getPos()) instanceof PipeBlockEntity pipeBlockEntity) {
            BlockHitResult hit = new BlockHitResult(hitData.getHitVec(), hitData.getSideHit(), hitData.getPos(), false);
            return pipeBlockEntity.getPipeAtHitVector(hit);
        }
        return null;
    }

    private static ItemStack pipeStack(AbstractFullPipe pipe) {
        Item item = Registry.getFullPipeItemFromClass(pipe.getClass());
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Override
    public boolean overrideStandardInfo(ProbeMode probeMode, IProbeInfo probeInfo, Player player, Level level, BlockState blockState, IProbeHitData hitData) {
        if (!(blockState.getBlock() instanceof PipeBlock)) return false;
        PipeSide pipeSide = resolve(level, hitData);
        if (pipeSide == null) return false;

        ItemStack itemStack = pipeStack(pipeSide.getPipe());
        String modName = Tools.getModName(blockState.getBlock());
        probeInfo.horizontal()
                .item(itemStack)
                .vertical()
                .itemLabel(itemStack)
                .text(CompoundText.create().style(TextStyleClass.MODNAME).text(modName));
        return true;
    }

    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo probeInfo, Player player, Level level, BlockState blockState, IProbeHitData hitData) {
        if (!(blockState.getBlock() instanceof PipeBlock)) return;
        PipeSide pipeSide = resolve(level, hitData);
        if (pipeSide == null) return;

        List<Component> lines = new ArrayList<>();
        pipeSide.getPipe().appendOverlayInfo(lines, pipeSide.getPipeBlockEntity(), pipeSide.getDirection());
        for (Component line : lines)
            probeInfo.text(line);
    }
}