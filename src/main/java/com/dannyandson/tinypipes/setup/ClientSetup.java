package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.gui.ItemFilterGUI;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TinyPipes.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    public static final ResourceLocation PIPE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/pipe");

    public static void init(final FMLClientSetupEvent event) {
        MenuScreens.register(Registration.ITEM_FILTER_MENU_TYPE.get(), ItemFilterGUI::new);
        MenuScreens.register(Registration.FLUID_FILTER_MENU_TYPE.get(), ItemFilterGUI::new);
    }

}