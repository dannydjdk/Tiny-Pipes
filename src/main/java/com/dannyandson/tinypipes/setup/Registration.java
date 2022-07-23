package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.components.*;
import com.dannyandson.tinypipes.gui.FluidFilterContainerMenu;
import com.dannyandson.tinypipes.gui.ItemFilterContainerMenu;
import com.dannyandson.tinypipes.items.TinyPipeItem;
import com.dannyandson.tinyredstone.TinyRedstone;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Registration {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TinyPipes.MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES,TinyPipes.MODID);

    public static final RegistryObject<Item> ITEM_PIPE_ITEM = ITEMS.register("item_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> ITEM_FILTER_PIPE_ITEM = ITEMS.register("item_filter_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> FLUID_PIPE_ITEM = ITEMS.register("fluid_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> FLUID_FILTER_PIPE_ITEM = ITEMS.register("fluid_filter_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> ENERGY_PIPE_ITEM = ITEMS.register("energy_pipe", TinyPipeItem::new);
    public static final RegistryObject<Item> REDSTONE_PIPE_ITEM = ITEMS.register("redstone_pipe", TinyPipeItem::new);

    public static final RegistryObject<MenuType<ItemFilterContainerMenu>> ITEM_FILTER_MENU_TYPE = MENU_TYPES.register("item_filter", () -> new MenuType<>(ItemFilterContainerMenu::createMenu));
    public static final RegistryObject<MenuType<FluidFilterContainerMenu>> FLUID_FILTER_MENU_TYPE = MENU_TYPES.register("fluid_filter", () -> new MenuType<>(FluidFilterContainerMenu::createFluidMenu));

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
        TinyRedstone.registerPanelCell(FluidFilterPipe.class, FLUID_FILTER_PIPE_ITEM.get());
    }
}
