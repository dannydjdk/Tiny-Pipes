package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.rendering.PipeBlockEntityRenderer;
import com.dannyandson.tinypipes.gui.ItemFilterGUI;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = TinyPipes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    public static final ResourceLocation PIPE_TEXTURE = ResourceLocation.fromNamespaceAndPath(TinyPipes.MODID, "block/pipe");
    public static final ResourceLocation PIPE_PULL_TEXTURE = ResourceLocation.fromNamespaceAndPath(TinyPipes.MODID, "block/pipe_pull");
    public static final ResourceLocation PIPE_BUNDLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(TinyPipes.MODID, "block/pipe_bundle");

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.ITEM_FILTER_MENU_TYPE.get(), ItemFilterGUI::getItemFilterGUI);
        event.register(Registration.FLUID_FILTER_MENU_TYPE.get(), ItemFilterGUI::getItemFilterGUI);
    }

    @SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.PIPE_BLOCK_ENTITY.get(), PipeBlockEntityRenderer::new);
    }
}
