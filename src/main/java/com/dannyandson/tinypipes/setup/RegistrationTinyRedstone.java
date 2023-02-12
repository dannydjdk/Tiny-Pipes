package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.components.tiny.*;
import com.dannyandson.tinypipes.items.TinyPipeItem;
import com.dannyandson.tinyredstone.TinyRedstone;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class RegistrationTinyRedstone {

    //Tiny Redstone Tiny Pipe items
    public static final RegistryObject<Item> ITEM_PIPE_ITEM = Registration.ITEMS.register("item_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> ITEM_FILTER_PIPE_ITEM = Registration.ITEMS.register("item_filter_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> FLUID_PIPE_ITEM = Registration.ITEMS.register("fluid_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> FLUID_FILTER_PIPE_ITEM = Registration.ITEMS.register("fluid_filter_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> ENERGY_PIPE_ITEM = Registration.ITEMS.register("energy_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> REDSTONE_PIPE_ITEM = Registration.ITEMS.register("redstone_pipe", TinyPipeItem::new);

    public static void register(){}

    //called at FMLCommonSetupEvent in ModSetup
    public static void registerPanelCells() {
        TinyRedstone.registerPanelCell(ItemPipe.class, ITEM_PIPE_ITEM.get());
        TinyRedstone.registerPanelCell(ItemFilterPipe.class, ITEM_FILTER_PIPE_ITEM.get());
        TinyRedstone.registerPanelCell(EnergyPipe.class, ENERGY_PIPE_ITEM.get());
        TinyRedstone.registerPanelCell(RedstonePipe.class, REDSTONE_PIPE_ITEM.get());
        TinyRedstone.registerPanelCell(FluidPipe.class, FLUID_PIPE_ITEM.get());
        TinyRedstone.registerPanelCell(FluidFilterPipe.class, FLUID_FILTER_PIPE_ITEM.get());
    }
}
