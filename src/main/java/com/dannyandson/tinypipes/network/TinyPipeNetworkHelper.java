package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.IPipe;
import com.dannyandson.tinypipes.components.tiny.AbstractTinyPipe;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.CheckForNull;

public class TinyPipeNetworkHelper {

    @CheckForNull
    public static IPipe getPipe(BlockGetter level, BlockPos pos, int index) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PanelTile panelTile) {
            PanelCellPos cellPos = PanelCellPos.fromIndex(panelTile, index);
            if (cellPos.getIPanelCell() instanceof IPipe pipe)
            return pipe;
        }
        return null;
    }

    public static void setTinyPipeSideState(BlockGetter level, BlockPos pos, int index, Direction direction, PipeConnectionState connectionState){
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PanelTile panelTile) {
            PanelCellPos cellPos = PanelCellPos.fromIndex(panelTile, index);
            ((AbstractTinyPipe)cellPos.getIPanelCell()).setConnectionState(cellPos,panelTile.getSideFromDirection(direction), connectionState);
            cellPos.getPanelTile().sync();
        }

    }
}
