package com.dannyandson.tinypipes.components;

import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.*;
import com.dannyandson.tinyredstone.setup.Registration;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTinyPipe implements IPanelCell {

    protected List<Long> pushIds = new ArrayList<>();
    protected PushWrapper pushWrapper = null;
    protected int ticks = 0;
    protected List<Side> connectedSides = new ArrayList<>();
    protected List<Side> pullSides = new ArrayList<>();

    private static TextureAtlasSprite sprite = null;
    protected TextureAtlasSprite getSprite(){
        if (sprite==null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(ClientSetup.PIPE_TEXTURE);
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
    public void render(MatrixStack poseStack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay, float alpha) {

        sprite = getSprite();
        IVertexBuilder builder = buffer.getBuffer((alpha==1.0)? RenderType.solid():RenderType.translucent());
        int color = getColor();

        RenderHelper.drawCube(poseStack,builder,sprite,c1,c2,c1,c2,c1,c2,combinedLight,color,alpha);

        if (pullSides.contains(Side.FRONT))
            RenderHelper.drawCube(poseStack,builder,sprite,p1,p2,p1,p2,c2,s3,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.FRONT))
            RenderHelper.drawCube(poseStack,builder,sprite,s1,s2,s1,s2,c2,s3,combinedLight, color,alpha);
        if (pullSides.contains(Side.BACK))
            RenderHelper.drawCube(poseStack,builder,sprite,p1,p2,p1,p2,s0,c1,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.BACK))
            RenderHelper.drawCube(poseStack,builder,sprite,s1,s2,s1,s2,s0,c1,combinedLight, color,alpha);
        if (pullSides.contains(Side.LEFT))
            RenderHelper.drawCube(poseStack,builder,sprite,c2,s3,p1,p2,p1,p2,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.LEFT))
            RenderHelper.drawCube(poseStack,builder,sprite,c2,s3,s1,s2,s1,s2,combinedLight, color,alpha);
        if (pullSides.contains(Side.RIGHT))
            RenderHelper.drawCube(poseStack,builder,sprite,s0,c1,p1,p2,p1,p2,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.RIGHT))
            RenderHelper.drawCube(poseStack,builder,sprite,s0,c1,s1,s2,s1,s2,combinedLight, color,alpha);
        if (pullSides.contains(Side.TOP))
            RenderHelper.drawCube(poseStack,builder,sprite,p1,p2,c2,s3,p1,p2,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.TOP))
            RenderHelper.drawCube(poseStack,builder,sprite,s1,s2,c2,s3,s1,s2,combinedLight, color,alpha);
        if (pullSides.contains(Side.BOTTOM))
            RenderHelper.drawCube(poseStack,builder,sprite,p1,p2,s0,c1,p1,p2,combinedLight, color,alpha);
        else if (connectedSides.contains(Side.BOTTOM))
            RenderHelper.drawCube(poseStack,builder,sprite,s1,s2,s0,c1,s1,s2,combinedLight, color,alpha);
    }

    @Override
    public boolean onPlace(PanelCellPos cellPos, PlayerEntity player) {

        if(RotationLock.getServerRotationLock(player)==null) {
            Direction panelFacing = cellPos.getPanelTile().getBlockState().getValue(BlockStateProperties.FACING);
            double playerToPanel;
            switch (panelFacing) {
                case NORTH:
                    playerToPanel = -player.getLookAngle().z;
                    break;
                case SOUTH:
                    playerToPanel = player.getLookAngle().z;
                    break;
                case WEST:
                    playerToPanel = -player.getLookAngle().x;
                    break;
                case EAST:
                    playerToPanel = player.getLookAngle().x;
                    break;
                case UP:
                    playerToPanel = player.getLookAngle().y;
                    break;
                default:
                    playerToPanel = -player.getLookAngle().y;
            }
            if (playerToPanel > 0.90 || playerToPanel < -0.7){
                connectedSides.add(Side.TOP);
                connectedSides.add(Side.BOTTOM);
                return false;
            }
        }
        connectedSides.add(Side.FRONT);
        connectedSides.add(Side.BACK);

        return false;
    }

    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, PlayerEntity player) {
        if (player.getMainHandItem().getItem()== Registration.REDSTONE_WRENCH.get())
        {
            Side sideOfCell = getClickedSide(cellPos,player);
            if (connectedSides.contains(sideOfCell)){
                PanelCellNeighbor neighbor = cellPos.getNeighbor(sideOfCell);
                if (neighbor!=null && neighbor.getBlockPos()!=null && !pullSides.contains(sideOfCell))
                    pullSides.add(sideOfCell);
                else{
                    pullSides.remove(sideOfCell);
                    connectedSides.remove(sideOfCell);
                }
            }else
                connectedSides.add(sideOfCell);
        }
        return false;
    }

    protected Side getClickedSide(PanelCellPos cellPos,PlayerEntity player){
        PanelTile panelTile = cellPos.getPanelTile();
        BlockPos pos = panelTile.getBlockPos();
        Direction panelFacing = panelTile.getBlockState().getValue(BlockStateProperties.FACING);
        BlockRayTraceResult result = panelTile.getPlayerCollisionHitResult(player);
        Direction rayTraceDirection = result.getDirection().getOpposite();
        Vector3d hitVec;

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
            if (relY>c2 && side == panelTile.getPanelCellSide(cellPos,Side.TOP))
                return side;
            if (relY<c1 && side == panelTile.getPanelCellSide(cellPos,Side.BOTTOM))
                return side;
            if (relX>c2 && side == panelTile.getPanelCellSide(cellPos,Side.RIGHT))
                return side;
            if (relX<c1 && side == panelTile.getPanelCellSide(cellPos,Side.LEFT))
                return side;
            if (relZ>c2 && side == panelTile.getPanelCellSide(cellPos,Side.BACK))
                return side;
            if (relZ<c1 && side == panelTile.getPanelCellSide(cellPos,Side.FRONT))
                return side;
        }

        if (relX>c2) return panelTile.getPanelCellSide(cellPos,Side.RIGHT);
        if (relX<c1) return panelTile.getPanelCellSide(cellPos,Side.LEFT);
        if (relY>c2) return panelTile.getPanelCellSide(cellPos,Side.TOP);
        if (relY<c1) return panelTile.getPanelCellSide(cellPos,Side.BOTTOM);
        if (relZ>c2) return panelTile.getPanelCellSide(cellPos,Side.BACK);
        if (relZ<c1) return panelTile.getPanelCellSide(cellPos,Side.FRONT);
        return panelTile.getPanelCellSide(cellPos,panelTile.getSideFromDirection(rayTraceDirection.getOpposite()));
    }

    @Override
    public boolean hasActivation(PlayerEntity player) {
        return player.getMainHandItem().getItem()== Registration.REDSTONE_WRENCH.get();
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
    public CompoundNBT writeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        if (!connectedSides.isEmpty()) {
            List<Integer> sides = new ArrayList<>();
            for (Side side : connectedSides)
                sides.add(side.ordinal());
            nbt.putIntArray("connectedSides",sides);
        }
        if (!pullSides.isEmpty()) {
            List<Integer> sides = new ArrayList<>();
            for (Side side : pullSides)
                sides.add(side.ordinal());
            nbt.putIntArray("pullSides",sides);
        }
        return nbt;
    }

    @Override
    public void readNBT(CompoundNBT compoundTag) {
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
            if (pullSides.contains(Side.TOP))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(p1, c2, p1),
                        new Vector3d(p2, s3, p2)
                ));
            else if (connectedSides.contains(Side.TOP))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(c1, c2, c1),
                        new Vector3d(c2, s3, c2)
                ));
            if (pullSides.contains(Side.BOTTOM))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(p1, s0, p1),
                        new Vector3d(p2, c1, p2)
                ));
            else if (connectedSides.contains(Side.BOTTOM))
                shapes.add(new PanelCellVoxelShape(
                        new Vector3d(c1, s0, c1),
                        new Vector3d(c2, c1, c2)
                ));
        }

        return shapes.toArray(new PanelCellVoxelShape[0]);
    }
}
