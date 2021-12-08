package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.components.IFilterPipe;
import com.dannyandson.tinypipes.network.ModNetworkHandler;
import com.dannyandson.tinypipes.network.PushItemFilterFlags;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ItemFilterGUI extends ContainerScreen<ItemFilterContainerMenu> {
    public static final int WIDTH = 184;
    public static final int HEIGHT = 158;
    private static final ResourceLocation GUI = new ResourceLocation(TinyPipes.MODID, "textures/gui/item_filter.png");
    private Button blackListButton = null;
    private PanelCellPos cellPos;
    private IFilterPipe iFilterPipe = null;
    private boolean blacklist = false;

    private final ItemFilterContainerMenu menu;

    public ItemFilterGUI(ItemFilterContainerMenu menu, PlayerInventory playerInventory, ITextComponent title) {
        super(menu, playerInventory,title);
        this.menu=menu;
        this.imageHeight=HEIGHT;
        this.imageWidth=WIDTH;
        this.inventoryLabelY=65;
    }

    @Override
    protected void init() {
        if (minecraft.hitResult != null && minecraft.hitResult.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult blockhitresult = (BlockRayTraceResult) minecraft.hitResult;
            TileEntity blockEntity = minecraft.level.getBlockEntity(blockhitresult.getBlockPos());
            if (blockEntity instanceof PanelTile) {
                PanelTile panelTile = (PanelTile)blockEntity;
                PanelCellPos cellPos = PanelCellPos.fromHitVec(panelTile, panelTile.getBlockState().getValue(BlockStateProperties.FACING), blockhitresult);
                if (cellPos.getIPanelCell() instanceof IFilterPipe) {
                    this.iFilterPipe = (IFilterPipe)cellPos.getIPanelCell();
                    this.blacklist= this.iFilterPipe.getBlackList();
                    this.cellPos = cellPos;
                }
            }
        }

        super.init();
        blackListButton = getNewFilterButton();
        addButton(blackListButton);
    }

    private void toggleBlacklist() {
        if (this.iFilterPipe != null) {
            this.blacklist=!this.blacklist;
            this.buttons.remove(blackListButton);
            blackListButton = getNewFilterButton();
            addButton(blackListButton);
            ModNetworkHandler.sendToServer(new PushItemFilterFlags(cellPos,!iFilterPipe.getBlackList()));
        }
    }

    private Button getNewFilterButton()
    {
        return new Button(leftPos+113, topPos+3, 60, 16, new TranslationTextComponent("tinypipes." + ((this.blacklist)?"blacklist" : "whitelist")), button -> toggleBlacklist());
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
   }

    @Override
    protected void renderBg(MatrixStack poseStack, float p_97788_, int p_97789_, int p_97790_) {
        RenderSystem.blendColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(GUI);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        blit(poseStack, x, y, 0, 0, WIDTH, HEIGHT, 256,256);
    }

}
