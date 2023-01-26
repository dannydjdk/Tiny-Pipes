package com.dannyandson.tinypipes.components.tiny;

import com.dannyandson.tinypipes.gui.TinyPipeConfigGUI;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.*;
import com.dannyandson.tinyredstone.setup.Registration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3d;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTinyPipe implements IPanelCell {

    protected List<Long> pushIds = new ArrayList<>();
    protected int ticks = 0;
    protected List<Side> connectedSides = new ArrayList<>();
    protected List<Side> pullSides = new ArrayList<>();

    private static TextureAtlasSprite sprite = null;
    protected TextureAtlasSprite getSprite(){
        if (sprite==null)
            sprite = RenderHelper.getSprite(ClientSetup.PIPE_TEXTURE);
        return sprite;
    }
    protected int getColor() {
        return 0xFFFFFFFF;
    }

    protected static final float
            c1=.25f, c2=.75f,
            s1=.30f,s2=.70f,
            s0=0f,s3=1f,
            p1=.1f,p2=.9f;


    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, float alpha) {

        sprite = getSprite();
        VertexConsumer builder = buffer.getBuffer((alpha==1.0)? RenderType.solid():RenderType.translucent());
        int color = getColor();

        com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,c1,c2,c1,c2,c1,c2,combinedLight,color,alpha);

        if (pullSides.contains(Side.FRONT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,p1,p2,p1,p2,c2,s3,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.FRONT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,s1,s2,s1,s2,c2,s3,combinedLight, color,alpha);
        if (pullSides.contains(Side.BACK))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,p1,p2,p1,p2,s0,c1,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.BACK))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,s1,s2,s1,s2,s0,c1,combinedLight, color,alpha);
        if (pullSides.contains(Side.LEFT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,c2,s3,p1,p2,p1,p2,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.LEFT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,c2,s3,s1,s2,s1,s2,combinedLight, color,alpha);
        if (pullSides.contains(Side.RIGHT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,s0,c1,p1,p2,p1,p2,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.RIGHT))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,s0,c1,s1,s2,s1,s2,combinedLight, color,alpha);
        if (pullSides.contains(Side.TOP))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,p1,p2,c2,s3,p1,p2,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.TOP))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,s1,s2,c2,s3,s1,s2,combinedLight, color,alpha);
        if (pullSides.contains(Side.BOTTOM))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,p1,p2,s0,c1,p1,p2,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.BOTTOM))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack,builder,sprite,s1,s2,s0,c1,s1,s2,combinedLight, color,alpha);
    }

    @Override
    public boolean onPlace(PanelCellPos cellPos, Player player) {

        connectedSides.add(Side.FRONT);
        connectedSides.add(Side.BACK);

        return false;
    }

    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, Player player) {
        if (player.getMainHandItem().getItem() == Registration.REDSTONE_WRENCH.get()) {
            Side sideOfCell = getClickedSide(cellPos, player);
            if (sideOfCell != null) {
                toggleSideConnection(cellPos, sideOfCell);
            }
        } else if (player.level.isClientSide){
            TinyPipeConfigGUI.open(cellPos,this);
        }
        return false;
    }

    @Override
    public boolean canPlaceVertical() {
        return true;
    }

    protected Side getClickedSide(PanelCellPos cellPos, Player player){
        PanelTile panelTile = cellPos.getPanelTile();
        BlockPos pos = panelTile.getBlockPos();
        Direction panelFacing = panelTile.getBlockState().getValue(BlockStateProperties.FACING);
        BlockHitResult result = panelTile.getPlayerCollisionHitResult(player);
        Direction rayTraceDirection = result.getDirection().getOpposite();
        Vec3 hitVec;

        if (pos.equals(result.getBlockPos()))
            hitVec = result.getLocation().add((double)rayTraceDirection.getStepX()*.001d,(double)rayTraceDirection.getStepY()*.001d,(double)rayTraceDirection.getStepZ()*.001d);
        else
            hitVec=result.getLocation();

        double relX,relY,relZ;

        if (panelFacing==Direction.NORTH) {
            relX = hitVec.x - pos.getX();
            relY = hitVec.z-pos.getZ();
            relZ = 1 - (hitVec.y - pos.getY());
        }
        else if (panelFacing==Direction.EAST) {
            relX = hitVec.y - pos.getY();
            relY = 1-(hitVec.x - pos.getX());
            relZ = hitVec.z - pos.getZ();
        }
        else if (panelFacing==Direction.SOUTH) {
            relX = hitVec.x - pos.getX();
            relY = 1-(hitVec.z-pos.getZ());
            relZ = hitVec.y - pos.getY();
        }
        else if (panelFacing==Direction.WEST) {
            relX =1-(hitVec.y - pos.getY());
            relY = hitVec.x-pos.getX();
            relZ = hitVec.z - pos.getZ();
        }
        else if (panelFacing==Direction.UP) {
            relX = hitVec.x - pos.getX();
            relY = 1 - (hitVec.y - pos.getY());
            relZ = 1 - (hitVec.z - pos.getZ());
        }
        else{
            relX = hitVec.x - pos.getX();
            relZ = hitVec.z - pos.getZ();
            relY = hitVec.y - pos.getY();
        }


        if (panelTile.hasBase() && relY<.125 && relY>.0625)
            relY+=.002f;
        if (relX==1.0)relX=.99;
        if (relZ==1.0)relZ=.99;
        if (relY==1.0)relY=.99;

        relX = (relX - (cellPos.getRow()/8d))*8d;
        relY = ((relY-1f/8f) - (cellPos.getLevel()/8d))*8d;
        relZ = (relZ - (cellPos.getColumn()/8d))*8d;

        if (relX<0)relX=1+relX;
        if (relY<0)relY=1+relY;
        if (relZ<0)relZ=1+relZ;

        //prioritize pullSides
        for (Side side : pullSides) {
            if (relY > c2 && side == panelTile.getPanelCellSide(cellPos, Side.TOP))
                return side;
            if (relY < c1 && side == panelTile.getPanelCellSide(cellPos, Side.BOTTOM))
                return side;
            if (relX > c2 && side == panelTile.getPanelCellSide(cellPos, Side.RIGHT))
                return side;
            if (relX < c1 && side == panelTile.getPanelCellSide(cellPos, Side.LEFT))
                return side;
            if (relZ > c2 && side == panelTile.getPanelCellSide(cellPos, Side.BACK))
                return side;
            if (relZ < c1 && side == panelTile.getPanelCellSide(cellPos, Side.FRONT))
                return side;
        }

        if (relX > c2) return panelTile.getPanelCellSide(cellPos,Side.RIGHT);
        if (relX < c1) return panelTile.getPanelCellSide(cellPos,Side.LEFT);
        if (relY > c2) return panelTile.getPanelCellSide(cellPos,Side.TOP);
        if (relY < c1) return panelTile.getPanelCellSide(cellPos,Side.BOTTOM);
        if (relZ > c2) return panelTile.getPanelCellSide(cellPos,Side.BACK);
        if (relZ < c1) return panelTile.getPanelCellSide(cellPos,Side.FRONT);
        return panelTile.getPanelCellSide(cellPos,panelTile.getSideFromDirection(rayTraceDirection.getOpposite()));
    }

    public PipeConnectionState toggleSideConnection(PanelCellPos cellPos, Side sideOfCell) {
        PipeConnectionState connectionState = getSideConnection(sideOfCell);
        if (connectionState == PipeConnectionState.DISABLED) {
            connectedSides.add(sideOfCell);
            return PipeConnectionState.ENABLED;
        }
        if (connectionState == PipeConnectionState.PULLING) {
            pullSides.remove(sideOfCell);
            connectedSides.remove(sideOfCell);
            return PipeConnectionState.DISABLED;
        }
        PanelCellNeighbor neighbor = cellPos.getNeighbor(sideOfCell);
        if (neighbor != null && neighbor.getBlockPos() != null) {
            pullSides.add(sideOfCell);
            return PipeConnectionState.PULLING;
        }
        pullSides.remove(sideOfCell);
        connectedSides.remove(sideOfCell);
        return PipeConnectionState.DISABLED;
    }

    public void setConnectionState(PanelCellPos cellPos, Side side, PipeConnectionState state) {
        if (state==PipeConnectionState.ENABLED) {
            if (!connectedSides.contains(side))
                connectedSides.add(side);
            pullSides.remove(side);
        } else if (state==PipeConnectionState.PULLING) {
            if (!connectedSides.contains(side))
                connectedSides.add(side);
            if (!pullSides.contains(side))
                pullSides.add(side);
        } else {
            connectedSides.remove(side);
            pullSides.remove(side);
        }
    }

    public PipeConnectionState getSideConnection(Side side){
        if (pullSides.contains(side))
            return PipeConnectionState.PULLING;
        if (connectedSides.contains(side))
            return PipeConnectionState.ENABLED;
        return PipeConnectionState.DISABLED;
    }

    @Override
    public boolean hasActivation(Player player) {
        Item heldItem = player.getMainHandItem().getItem();
        return heldItem == Registration.REDSTONE_WRENCH.get() || heldItem == Items.AIR;
    }

    @Override
    public boolean neighborChanged(PanelCellPos panelCellPos) {
        return false;
    }

    @Override
    public int getWeakRsOutput(Side side) {
        return 0;
    }

    @Override
    public int getStrongRsOutput(Side side) {
        return 0;
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (!connectedSides.isEmpty()) {
            List<Integer> sides = new ArrayList<>();
            for (Side side : connectedSides)
                if (side != null)
                    sides.add(side.ordinal());
            nbt.putIntArray("connectedSides",sides);
        }
        if (!pullSides.isEmpty()) {
            List<Integer> sides = new ArrayList<>();
            for (Side side : pullSides)
                if (side != null)
                    sides.add(side.ordinal());
            nbt.putIntArray("pullSides",sides);
        }
        return nbt;
    }

    @Override
    public void readNBT(CompoundTag compoundTag) {
        if (compoundTag.contains("connectedSides")){
            for (int i : compoundTag.getIntArray("connectedSides"))
                connectedSides.add(Side.values()[i]);
        }
        if (compoundTag.contains("pullSides")){
            for (int i : compoundTag.getIntArray("pullSides"))
                pullSides.add(Side.values()[i]);
        }
    }

    public static PanelCellVoxelShape[] PIPE_SHAPE = new PanelCellVoxelShape[]{
            new PanelCellVoxelShape(
                    new Vector3d(c1, c1, c1),
                    new Vector3d(c2, c2, c2)
            ),
            new PanelCellVoxelShape(
                    new Vector3d(s0, s1, s1),
                    new Vector3d(s3, s2, s2)
            ),
            new PanelCellVoxelShape(
                    new Vector3d(s1, s0, s1),
                    new Vector3d(s2, s3, s2)
            ),
            new PanelCellVoxelShape(
                    new Vector3d(s1, s1, s0),
                    new Vector3d(s2, s2, s3)
            )

    };
    @Override
    public PanelCellVoxelShape[] getShapes(PanelCellPos cellPos) {

        List<PanelCellVoxelShape> shapes = new ArrayList<>();

        shapes.add(new PanelCellVoxelShape(
                new Vector3d(c1, c1, c1),
                new Vector3d(c2, c2, c2)
        ));

        if (cellPos!=null) {
            PanelTile panelTile = cellPos.getPanelTile();
            if (pullSides.contains(panelTile.getPanelCellSide(cellPos,Side.BACK)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(p1, p1, c2),
                        new Vector3d(p2, p2, s3)
                ));
            else if (connectedSides.contains(panelTile.getPanelCellSide(cellPos,Side.BACK)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(c1, c1, c2),
                        new Vector3d(c2, c2, s3)
                ));
            if (pullSides.contains(panelTile.getPanelCellSide(cellPos,Side.FRONT)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(p1, p1, s0),
                        new Vector3d(p2, p2, c1)
                ));
            else if (connectedSides.contains(panelTile.getPanelCellSide(cellPos,Side.FRONT)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(c1, c1, s0),
                        new Vector3d(c2, c2, c1)
                ));
            if (pullSides.contains(panelTile.getPanelCellSide(cellPos,Side.RIGHT)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(c2, p1, p1),
                        new Vector3d(s3, p2, p2)
                ));
            else if (connectedSides.contains(panelTile.getPanelCellSide(cellPos,Side.RIGHT)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(c2, c1, c1),
                        new Vector3d(s3, c2, c2)
                ));
            if (pullSides.contains(panelTile.getPanelCellSide(cellPos,Side.LEFT)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(s0, p1, p1),
                        new Vector3d(c1, p2, p2)
                ));
            else if (connectedSides.contains(panelTile.getPanelCellSide(cellPos,Side.LEFT)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(s0, c1, c1),
                        new Vector3d(c1, c2, c2)
                ));
            if (pullSides.contains(panelTile.getPanelCellSide(cellPos,Side.TOP)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(p1, c2, p1),
                        new Vector3d(p2, s3, p2)
                ));
            else if (connectedSides.contains(panelTile.getPanelCellSide(cellPos,Side.TOP)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(c1, c2, c1),
                        new Vector3d(c2, s3, c2)
                ));
            if (pullSides.contains(panelTile.getPanelCellSide(cellPos,Side.BOTTOM)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(p1, s0, p1),
                        new Vector3d(p2, c1, p2)
                ));
            else if (connectedSides.contains(panelTile.getPanelCellSide(cellPos,Side.BOTTOM)))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(c1, s0, c1),
                        new Vector3d(c2, c1, c2)
                ));
        }

        return shapes.toArray(new PanelCellVoxelShape[0]);
    }

    public enum PipeConnectionState{ DISABLED, ENABLED, PULLING }

}
