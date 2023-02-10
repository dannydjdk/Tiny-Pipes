package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.blocks.PipeConnectionState;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.network.ModNetworkHandler;
import com.dannyandson.tinypipes.network.PushPipeConnection;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class PipeConfigGUI extends Screen {

    private static final int WIDTH = 244;
    private static final int HEIGHT = 160;

    private final PipeBlockEntity pipeBlockEntity;
    private final AbstractFullPipe pipe;
    private Map<Direction, AbstractWidget> sideButtons = new HashMap<>();
    private Map<Direction,Integer> xLocations = new HashMap<>();
    private Map<Direction,Integer> yLocations = new HashMap<>();

    private final ResourceLocation GUI = new ResourceLocation(TinyPipes.MODID, "textures/gui/transparent.png");

    protected PipeConfigGUI(PipeBlockEntity pipeBlockEntity, AbstractFullPipe tinyPipe) {
        super(Component.translatable("tinypipes:pipeconfiggui"));
        this.pipeBlockEntity = pipeBlockEntity;
        this.pipe = tinyPipe;
    }

    @Override
    protected void init() {
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;

        addRenderableWidget(new ModWidget(relX-1, relY-1, WIDTH+2, HEIGHT+2, 0xAA000000));
        addRenderableWidget(new ModWidget(relX, relY, WIDTH, HEIGHT, 0x88EEEEEE));

        addRenderableWidget(new ModWidget(relX,relY+2,WIDTH-2,40,Component.translatable("tinypipes.gui.pipe_config")).setTextHAlignment(ModWidget.HAlignment.CENTER));

        for (Direction side : Direction.values()) {
            int dRelX = relX + ((side == Direction.UP) ? 2 : (side == Direction.DOWN) ? 2 : (side == Direction.NORTH) ? 122 : (side == Direction.WEST) ? 62 : (side == Direction.EAST) ? 182 : 122);
            int dRelY = relY + ((side == Direction.UP) ? 20 : (side == Direction.DOWN) ? 60 : (side == Direction.NORTH) ? 20 : (side == Direction.WEST) ? 40 : (side == Direction.EAST) ? 40 : 60);

            xLocations.put(side, dRelX);
            yLocations.put(side, dRelY);

            //label
            addRenderableWidget(new ModWidget(dRelX, dRelY, 60, 20, Component.nullToEmpty(side.name())).setTextHAlignment(ModWidget.HAlignment.CENTER));

            //side toggle button
            Button toggleButton = ModWidget.buildButton(dRelX, dRelY + 10, 60, 20, Component.nullToEmpty(pipe.getPipeSideStatus(side).name()), button -> toggleConnection(side));
            sideButtons.put(side, toggleButton);
            addRenderableWidget(toggleButton);
        }


        addRenderableWidget(new ModWidget(relX+2,relY+100,WIDTH-2,40,Component.translatable("tinypipes.gui.pipe_config.msg.enabled")));
        addRenderableWidget(new ModWidget(relX+2,relY+110,WIDTH-2,40,Component.translatable("tinypipes.gui.pipe_config.msg.disabled")));
        addRenderableWidget(new ModWidget(relX+2,relY+120,WIDTH-2,40,Component.translatable("tinypipes.gui.pipe_config.msg.pulling")));

        addRenderableWidget(ModWidget.buildButton(relX + 82, relY + 135, 80, 20, Component.translatable("tinyredstone.close"), button -> close()));


    }

    private void close() {
        minecraft.setScreen(null);
    }

    private void toggleConnection(Direction side) {
        PipeConnectionState state = pipe.togglePipeSide(side);

        ModNetworkHandler.sendToServer(new PushPipeConnection(pipeBlockEntity.getBlockPos(), pipe.slotPos(), side, state));

        Button widget = ModWidget.buildButton(xLocations.get(side), yLocations.get(side) + 10, 60, 20, Component.nullToEmpty(state.name()), button -> toggleConnection(side));

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


    public static void open(PipeBlockEntity pipeBlockEntity, AbstractFullPipe tinyPipe) {
        if(pipeBlockEntity!=null && tinyPipe!=null)
            Minecraft.getInstance().setScreen(new PipeConfigGUI(pipeBlockEntity, tinyPipe));
    }

}
