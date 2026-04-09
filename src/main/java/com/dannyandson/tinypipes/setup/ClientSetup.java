package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.rendering.PipeBlockEntityRenderer;
import com.dannyandson.tinypipes.gui.ItemFilterGUI;
import com.dannyandson.tinypipes.gui.PipeConfigPipRenderState;
import com.dannyandson.tinypipes.gui.PipeConfigPipRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

@EventBusSubscriber(modid = TinyPipes.MODID, value = Dist.CLIENT)
public class ClientSetup {

    public static final Identifier PIPE_TEXTURE = Identifier.fromNamespaceAndPath(TinyPipes.MODID, "block/pipe");
    public static final Identifier PIPE_PULL_TEXTURE = Identifier.fromNamespaceAndPath(TinyPipes.MODID, "block/pipe_pull");
    public static final Identifier PIPE_BUNDLE_TEXTURE = Identifier.fromNamespaceAndPath(TinyPipes.MODID, "block/pipe_bundle");

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistration.ITEM_FILTER_MENU_TYPE.get(), ItemFilterGUI::getItemFilterGUI);
        event.register(ModRegistration.FLUID_FILTER_MENU_TYPE.get(), ItemFilterGUI::getItemFilterGUI);
    }

    @SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistration.PIPE_BLOCK_ENTITY.get(), PipeBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterPipRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(PipeConfigPipRenderState.class, PipeConfigPipRenderer::new);
    }
}