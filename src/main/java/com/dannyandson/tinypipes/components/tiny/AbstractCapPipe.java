package com.dannyandson.tinypipes.components.tiny;

import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.ICapPipe;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinyredstone.blocks.PanelCellNeighbor;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelCellSegment;
import com.dannyandson.tinyredstone.blocks.PanelCellVoxelShape;
import com.dannyandson.tinyredstone.blocks.Side;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class AbstractCapPipe<CapType> extends AbstractTinyPipe implements ICapPipe<CapType> {
    protected PushWrapper<CapType> pushWrapper = null;
    protected int amountPushed = 0;

    // Gray dye reverts a side to the default channel (no map entry == default).
    public static final int DEFAULT_FREQUENCY = DyeColor.GRAY.getId();

    // Per-side channel ("frequency"). Only non-default channels are stored; absent sides use the
    // default. A pull side only delivers to output sides on the same channel.
    private final Map<Side, Integer> frequencies = new HashMap<>();

    // Sides that reach the panel edge with an adjacent (non-panel) block. Cap pipes only interface
    // with machines there, so the channel color is only drawn on these arms; the rest keep the
    // pipe's type texture. Derived state, refreshed on placement/neighbor change and persisted so
    // the client render has it before the first neighborChanged.
    private final Set<Side> edgeSides = new HashSet<>();

    // One-shot guard: legacy pipes saved before this feature have no "edgeSides" NBT, so they're
    // seeded once from getShapes (which has a cellPos and runs when the panel builds its shape).
    private boolean edgeSidesInitialized = false;

    // Generic (untextured) pipe sprite used to tint the channel color on edge arms.
    private static TextureAtlasSprite channelSprite = null;
    protected TextureAtlasSprite getChannelSprite() {
        if (channelSprite == null)
            channelSprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(ClientSetup.PIPE_TEXTURE);
        return channelSprite;
    }


    public abstract int canAccept(int amount);

    public void didPush(int amount) {
        amountPushed+=amount;
    }

    /** Whether this pipe is currently redstone-disabled; used to gray out the render. */
    protected boolean isDisabled() {
        return false;
    }

    /**
     * The channel ("frequency") assigned to a side; DEFAULT_FREQUENCY when undyed.
     * Contents pulled in on one side only travel to output sides on the same channel.
     */
    public int getFrequency(Side side) {
        return frequencies.getOrDefault(side, DEFAULT_FREQUENCY);
    }

    /** Render color for a side's channel band; the default channel renders as gray. */
    protected int getChannelColor(Side side) {
        if (isDisabled()) return 0xFF888888;
        return DyeColor.byId(getFrequency(side)).getMapColor().col;
    }

    /**
     * Assign a channel to a side from a dye. Gray reverts the side to the default channel.
     * Routing recomputes each tick, so this just updates state and re-syncs the band color.
     */
    public void setColor(PanelCellPos cellPos, Side side, DyeColor color) {
        if (color == DyeColor.GRAY)
            frequencies.remove(side);
        else
            frequencies.put(side, color.getId());
        // push the new band color to clients (server authoritative; client copy already updated above)
        if (cellPos.getPanelTile().getLevel() != null && !cellPos.getPanelTile().getLevel().isClientSide)
            cellPos.getPanelTile().sync();
    }

    /** Recompute which sides face an adjacent (non-panel) block, i.e. a machine-facing edge. */
    protected void updateEdgeSides(PanelCellPos cellPos) {
        edgeSides.clear();
        for (Side side : Side.values()) {
            PanelCellNeighbor neighbor = cellPos.getNeighbor(side);
            if (neighbor != null && neighbor.getBlockPos() != null)
                edgeSides.add(side);
        }
        edgeSidesInitialized = true;
    }

    /**
     * getShapes is the only cellPos-bearing method that runs on the client without a tick, so it
     * doubles as a lazy seed for legacy pipes that predate the edgeSides NBT. Computes once.
     */
    @Override
    public PanelCellVoxelShape[] getShapes(PanelCellPos cellPos) {
        if (!edgeSidesInitialized && cellPos != null)
            updateEdgeSides(cellPos);
        return super.getShapes(cellPos);
    }

    @Override
    public boolean onPlace(PanelCellPos cellPos, Player player) {
        boolean result = super.onPlace(cellPos, player);
        updateEdgeSides(cellPos);
        return result;
    }

    @Override
    public boolean hasActivation(Player player) {
        return super.hasActivation(player) || player.getMainHandItem().getItem() instanceof DyeItem;
    }

    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, Player player) {
        if (player.getMainHandItem().getItem() instanceof DyeItem dyeItem) {
            Side sideClicked = getClickedSide(cellPos, player);
            if (sideClicked != null)
                setColor(cellPos, sideClicked, dyeItem.getDyeColor());
            return false;
        }
        return super.onBlockActivated(cellPos, segmentClicked, player);
    }

    /**
     * Same arm geometry as AbstractTinyPipe, but edge arms (machine-facing) are tinted with their
     * channel color on the generic pipe sprite while every other arm keeps the type texture.
     */
    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, float alpha) {
        TextureAtlasSprite typeSprite = getSprite();
        VertexConsumer builder = buffer.getBuffer((alpha == 1.0) ? RenderType.solid() : RenderType.translucent());
        int color = getColor();

        //center cube keeps the pipe's type texture/color
        com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, typeSprite, c1, c2, c1, c2, c1, c2, combinedLight, color, alpha);

        renderArm(poseStack, builder, Side.FRONT,  p1, p2, p1, p2, c2, s3,  s1, s2, s1, s2, c2, s3, typeSprite, color, combinedLight, alpha);
        renderArm(poseStack, builder, Side.BACK,   p1, p2, p1, p2, s0, c1,  s1, s2, s1, s2, s0, c1, typeSprite, color, combinedLight, alpha);
        renderArm(poseStack, builder, Side.LEFT,   c2, s3, p1, p2, p1, p2,  c2, s3, s1, s2, s1, s2, typeSprite, color, combinedLight, alpha);
        renderArm(poseStack, builder, Side.RIGHT,  s0, c1, p1, p2, p1, p2,  s0, c1, s1, s2, s1, s2, typeSprite, color, combinedLight, alpha);
        renderArm(poseStack, builder, Side.TOP,    p1, p2, c2, s3, p1, p2,  s1, s2, c2, s3, s1, s2, typeSprite, color, combinedLight, alpha);
        renderArm(poseStack, builder, Side.BOTTOM, p1, p2, s0, c1, p1, p2,  s1, s2, s0, c1, s1, s2, typeSprite, color, combinedLight, alpha);
    }

    /**
     * Draws one side arm. The first coordinate set is the (thin) pull shape, the second the
     * connected shape; only one is drawn depending on the side's connection state.
     */
    private void renderArm(PoseStack poseStack, VertexConsumer builder, Side side,
                           float px1, float px2, float py1, float py2, float pz1, float pz2,
                           float cx1, float cx2, float cy1, float cy2, float cz1, float cz2,
                           TextureAtlasSprite typeSprite, int typeColor, int combinedLight, float alpha) {
        boolean edge = edgeSides.contains(side);
        TextureAtlasSprite armSprite = edge ? getChannelSprite() : typeSprite;
        int armColor = edge ? getChannelColor(side) : typeColor;
        if (pullSides.contains(side))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, armSprite, px1, px2, py1, py2, pz1, pz2, combinedLight, armColor, alpha);
        else if (connectedSides.contains(side))
            com.dannyandson.tinypipes.components.RenderHelper.drawCube(poseStack, builder, armSprite, cx1, cx2, cy1, cy2, cz1, cz2, combinedLight, armColor, alpha);
    }

    @Override
    public boolean tick(PanelCellPos cellPos) {
        amountPushed=0;
        return false;
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();
        if (!frequencies.isEmpty()) {
            CompoundTag frequenciesNBT = new CompoundTag();
            for (Map.Entry<Side, Integer> set : frequencies.entrySet())
                if (set.getKey() != null)
                    frequenciesNBT.putInt(set.getKey().name(), set.getValue());
            nbt.put("frequencies", frequenciesNBT);
        }
        if (!edgeSides.isEmpty()) {
            List<Integer> sides = new ArrayList<>();
            for (Side side : edgeSides)
                if (side != null)
                    sides.add(side.ordinal());
            nbt.putIntArray("edgeSides", sides);
        }
        return nbt;
    }

    @Override
    public void readNBT(CompoundTag compoundTag) {
        super.readNBT(compoundTag);
        if (compoundTag.contains("frequencies")) {
            for (String side : compoundTag.getCompound("frequencies").getAllKeys()) {
                frequencies.put(Side.valueOf(side), compoundTag.getCompound("frequencies").getInt(side));
            }
        }
        if (compoundTag.contains("edgeSides")) {
            edgeSides.clear();
            for (int i : compoundTag.getIntArray("edgeSides"))
                edgeSides.add(Side.values()[i]);
            edgeSidesInitialized = true;
        }
    }
}