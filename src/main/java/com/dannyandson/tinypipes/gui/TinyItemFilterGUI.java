package com.dannyandson.tinypipes.gui;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.components.tiny.AbstractTinyPipe;
import com.dannyandson.tinypipes.components.IFilterPipe;
import com.dannyandson.tinypipes.network.ModNetworkHandler;
import com.dannyandson.tinypipes.network.PushItemFilterFlags;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class TinyItemFilterGUI extends AbstractContainerScreen<ItemFilterContainerMenu> implements MenuAccess<ItemFilterContainerMenu> {
    public static final int WIDTH = 184;
    public static final int HEIGHT = 158;
    private static final Identifier GUI = Identifier.fromNamespaceAndPath(TinyPipes.MODID, "textures/gui/item_filter.png");
    private Button blackListButton = null;
    private PanelCellPos cellPos;
    private IFilterPipe iFilterPipe = null;
    private boolean blacklist = false;

    private final ItemFilterContainerMenu menu;

    public TinyItemFilterGUI(ItemFilterContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WIDTH, HEIGHT);
        this.menu=menu;
        this.inventoryLabelY=65;
    }

    @Override
    protected void init() {
        if (minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockhitresult = (BlockHitResult) minecraft.hitResult;
            BlockEntity blockEntity = minecraft.level.getBlockEntity(blockhitresult.getBlockPos());
            if (blockEntity instanceof PanelTile panelTile) {
                PanelCellPos cellPos = PanelCellPos.fromHitVec(panelTile, panelTile.getBlockState().getValue(BlockStateProperties.FACING), blockhitresult);
                if (cellPos.getIPanelCell() instanceof IFilterPipe iFilterPipe) {
                    this.cellPos = cellPos;
                    this.iFilterPipe =iFilterPipe;
                    this.blacklist=iFilterPipe.getBlackList();
                }
            }
        }

        super.init();
        blackListButton = getNewFilterButton();
        addRenderableWidget(blackListButton);

        addRenderableWidget(ModWidget.buildButton(leftPos+73, topPos+3, 50, 16, Component.translatable("tinypipes.pipe_config_button"), button -> {
            TinyPipeConfigGUI.open(cellPos,(AbstractTinyPipe) iFilterPipe);
        }));

    }

    private void toggleBlacklist() {
        if (this.iFilterPipe != null) {
            this.blacklist=!this.blacklist;
            removeWidget(blackListButton);
            blackListButton = getNewFilterButton();
            addRenderableWidget(blackListButton);
            ModNetworkHandler.sendToServer(new PushItemFilterFlags(cellPos.getPanelTile().getBlockPos(),cellPos.getIndex(),!iFilterPipe.getBlackList()));
        }
    }

    private Button getNewFilterButton()
    {
        return ModWidget.buildButton(leftPos+123, topPos+3, 50, 16, Component.translatable("tinypipes." + ((this.blacklist)?"blacklist" : "whitelist")), button -> toggleBlacklist());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - WIDTH) / 2;
        int y = (this.height - HEIGHT) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, x, y, 0, 0, WIDTH, HEIGHT, 256, 256);
    }
}
