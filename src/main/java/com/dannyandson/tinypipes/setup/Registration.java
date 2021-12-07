package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.components.*;
import com.dannyandson.tinypipes.gui.ItemFilterContainerMenu;
import com.dannyandson.tinypipes.items.TinyPipeItem;
import com.dannyandson.tinyredstone.TinyRedstone;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class Registration {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TinyPipes.MODID);
    private static final DeferredRegister<ContainerType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.CONTAINERS,TinyPipes.MODID);

    public static final RegistryObject<Item> ITEM_PIPE_ITEM = ITEMS.register("item_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> ITEM_FILTER_PIPE_ITEM = ITEMS.register("item_filter_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> FLUID_PIPE_ITEM = ITEMS.register("fluid_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> ENERGY_PIPE_ITEM = ITEMS.register("energy_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> REDSTONE_PIPE_ITEM = ITEMS.register("redstone_pipe", TinyPipeItem::new);

    public static final RegistryObject<ContainerType<ItemFilterContainerMenu>> ITEM_FILTER_MENU_TYPE = MENU_TYPES.register("item_filter", () -> new ContainerType<>(ItemFilterContainerMenu::createMenu));

    //called from main mod constructor
    public static void register() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MENU_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    //called at FMLCommonSetupEvent in ModSetup
    public static void registerPanelCells() {
        TinyRedstone.registerPanelCell(ItemPipe.class, ITEM_PIPE_ITEM.get());
        TinyRedstone.registerPanelCell(ItemFilterPipe.class, ITEM_FILTER_PIPE_ITEM.get());
        TinyRedstone.registerPanelCell(EnergyPipe.class, ENERGY_PIPE_ITEM.get());
        TinyRedstone.registerPanelCell(RedstonePipe.class, REDSTONE_PIPE_ITEM.get());
        TinyRedstone.registerPanelCell(FluidPipe.class, FLUID_PIPE_ITEM.get());
    }
}
