package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.components.EnergyPipe;
import com.dannyandson.tinypipes.components.ItemPipe;
import com.dannyandson.tinypipes.components.RedstonePipe;
import com.dannyandson.tinypipes.gui.ItemFilterGUI;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TinyPipes.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    public static final ResourceLocation PIPE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/pipe");

    public static void init(final FMLClientSetupEvent event) {
        MenuScreens.register(Registration.ITEM_FILTER_MENU_TYPE.get(), ItemFilterGUI::new);
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        if (!event.getMap().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            return;
        }
        event.addSprite(PIPE_TEXTURE);
        event.addSprite(EnergyPipe.ENERGY_PIPE_TEXTURE);
        event.addSprite(ItemPipe.ITEM_PIPE_TEXTURE);
        event.addSprite(RedstonePipe.REDSTONE_PIPE_TEXTURE);
    }

}