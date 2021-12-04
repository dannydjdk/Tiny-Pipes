package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.components.ItemFilterPipe;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemFilterGUI extends AbstractContainerScreen<ItemFilterContainerMenu> implements MenuAccess<ItemFilterContainerMenu> {
    public static final int WIDTH = 184;
    public static final int HEIGHT = 158;
    private static final ResourceLocation GUI = new ResourceLocation(TinyPipes.MODID, "textures/gui/item_filter.png");
    private int relX = (this.width - WIDTH) / 2;
    private int relY = (this.height - HEIGHT) / 2;

    private ItemFilterContainerMenu menu;
    private ItemFilterPipe itemFilterPipe;
    private int scrollIndex = 0;
    private List<ModWidget> listWidgets = new ArrayList<>();

    public ItemFilterGUI(ItemFilterContainerMenu menu, Inventory playerInventory,Component title) {
        super(menu, playerInventory,title);
        this.menu=menu;
        this.itemFilterPipe=menu.getItemFilterPipe();
        this.imageHeight=HEIGHT;
        this.imageWidth=WIDTH;
        this.inventoryLabelY=65;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
   }

    @Override
    protected void renderBg(PoseStack poseStack, float p_97788_, int p_97789_, int p_97790_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        blit(poseStack, x, y, 0, 0, WIDTH, HEIGHT, 256,256);
    }

}
