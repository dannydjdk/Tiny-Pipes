package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntityRenderer;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.gui.ItemFilterGUI;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TinyPipes.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    public static final ResourceLocation PIPE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/pipe");
    public static final ResourceLocation PIPE_PULL_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/pipe_pull");
    public static final ResourceLocation PIPE_BUNDLE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/pipe_bundle");

    public static void init(final FMLClientSetupEvent event) {
        MenuScreens.register(Registration.ITEM_FILTER_MENU_TYPE.get(), ItemFilterGUI::new);
        MenuScreens.register(Registration.FLUID_FILTER_MENU_TYPE.get(), ItemFilterGUI::new);
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        if (!event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            return;
        }
        event.addSprite(PIPE_TEXTURE);
        event.addSprite(PIPE_PULL_TEXTURE);
        event.addSprite(PIPE_BUNDLE_TEXTURE);
        event.addSprite(RenderHelper.ENERGY_PIPE_TEXTURE);
        event.addSprite(RenderHelper.ITEM_PIPE_TEXTURE);
        event.addSprite(RenderHelper.ITEM_FILTER_PIPE_TEXTURE);
        event.addSprite(RenderHelper.REDSTONE_PIPE_TEXTURE);
    }

    @SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.PIPE_BLOCK_ENTITY.get(), PipeBlockEntityRenderer::new);
    }

    }