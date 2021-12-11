package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.components.AbstractTinyPipe;
import com.dannyandson.tinypipes.network.ModNetworkHandler;
import com.dannyandson.tinypipes.network.PushPipeConnection;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.Side;
import com.dannyandson.tinyredstone.gui.ModWidget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class TinyPipeConfigGUI extends Screen {

    private static final int WIDTH = 244;
    private static final int HEIGHT = 160;

    private final PanelCellPos cellPos;
    private final AbstractTinyPipe tinyPipe;
    private Map<Side, AbstractWidget> sideButtons = new HashMap<>();
    private Map<Side,Integer> xLocations = new HashMap<>();
    private Map<Side,Integer> yLocations = new HashMap<>();

    private final ResourceLocation GUI = new ResourceLocation(TinyPipes.MODID, "textures/gui/transparent.png");

    protected TinyPipeConfigGUI(PanelCellPos cellPos, AbstractTinyPipe tinyPipe) {
        super(new TranslatableComponent("tinypipes:pipeconfiggui"));
        this.cellPos = cellPos;
        this.tinyPipe = tinyPipe;
    }

    @Override
    protected void init() {
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;

        addRenderableWidget(new ModWidget(relX-1, relY-1, WIDTH+2, HEIGHT+2, 0xAA000000));
        addRenderableWidget(new ModWidget(relX, relY, WIDTH, HEIGHT, 0x88EEEEEE));

        addRenderableWidget(new ModWidget(relX,relY+2,WIDTH-2,40,new TranslatableComponent("tinypipes.gui.pipe_config")).setTextHAlignment(ModWidget.HAlignment.CENTER));

        for (Direction direction : Direction.values()) {
            int dRelX = relX + ((direction == Direction.UP) ? 2 : (direction == Direction.DOWN) ? 2 : (direction == Direction.NORTH) ? 122 : (direction == Direction.WEST) ? 62 : (direction == Direction.EAST) ? 182 : 122);
            int dRelY = relY + ((direction == Direction.UP) ? 20 : (direction == Direction.DOWN) ? 60 : (direction == Direction.NORTH) ? 20 : (direction == Direction.WEST) ? 40 : (direction == Direction.EAST) ? 40 : 60);
            Side side = cellPos.getPanelTile().getPanelCellSide(cellPos, cellPos.getPanelTile().getSideFromDirection(direction));

            xLocations.put(side, dRelX);
            yLocations.put(side, dRelY);

            //label
            addRenderableWidget(new ModWidget(dRelX, dRelY, 60, 20, Component.nullToEmpty(direction.name())).setTextHAlignment(ModWidget.HAlignment.CENTER));

            //side toggle button
            Button toggleButton = new Button(dRelX, dRelY + 10, 60, 20, Component.nullToEmpty(tinyPipe.getSideConnection(side).name()), button -> toggleConnection(side));
            sideButtons.put(side, toggleButton);
            addRenderableWidget(toggleButton);
        }


        addRenderableWidget(new ModWidget(relX+2,relY+100,WIDTH-2,40,new TranslatableComponent("tinypipes.gui.pipe_config.msg.enabled")));
        addRenderableWidget(new ModWidget(relX+2,relY+110,WIDTH-2,40,new TranslatableComponent("tinypipes.gui.pipe_config.msg.disabled")));
        addRenderableWidget(new ModWidget(relX+2,relY+120,WIDTH-2,40,new TranslatableComponent("tinypipes.gui.pipe_config.msg.pulling")));

        addRenderableWidget(new Button(relX + 82, relY + 135, 80, 20, new TranslatableComponent("tinyredstone.close"), button -> close()));


    }

    private void close() {
        minecraft.setScreen(null);
    }

    private void toggleConnection(Side side)
    {
        AbstractTinyPipe.PipeConnectionState state = tinyPipe.toggleSideConnection(cellPos,side);
        ModNetworkHandler.sendToServer(new PushPipeConnection(cellPos, side, state));

        Button widget = new Button(xLocations.get(side),yLocations.get(side)+10,60,20, Component.nullToEmpty(state.name()),button->toggleConnection(side));

        this.removeWidget(sideButtons.get(side));
        sideButtons.put(side, widget);
        addRenderableWidget(sideButtons.get(side));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, WIDTH, HEIGHT);

        super.render(matrixStack,mouseX, mouseY, partialTicks);
    }


    public static void open(PanelCellPos cellPos, AbstractTinyPipe tinyPipe) {
        Minecraft.getInstance().setScreen(new TinyPipeConfigGUI(cellPos,tinyPipe));
    }

}
