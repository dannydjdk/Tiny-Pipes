package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.blocks.PipeBlock;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.components.full.*;
import com.dannyandson.tinypipes.gui.FluidFilterContainerMenu;
import com.dannyandson.tinypipes.gui.ItemFilterContainerMenu;
import com.dannyandson.tinypipes.items.FullPipeItem;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Registration {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TinyPipes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TinyPipes.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TinyPipes.MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES,TinyPipes.MODID);

    public static final RegistryObject<PipeBlock> PIPE_BLOCK = BLOCKS.register("pipe_block", PipeBlock::new);
    public static final RegistryObject<BlockEntityType<PipeBlockEntity>> PIPE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("pipe_block", ()->BlockEntityType.Builder.of(PipeBlockEntity::new,PIPE_BLOCK.get()).build(null));

    //Not so Tiny Pipe items
    public static final RegistryObject<Item> ITEM_PIPE_ITEM = Registration.ITEMS.register("full_item_pipe", FullPipeItem::new);
    public static final RegistryObject<Item> ITEM_FILTER_PIPE_ITEM = Registration.ITEMS.register("full_item_filter_pipe", FullPipeItem::new);
    public static final RegistryObject<Item> FLUID_PIPE_ITEM = Registration.ITEMS.register("full_fluid_pipe", FullPipeItem::new);
    public static final RegistryObject<Item> FLUID_FILTER_PIPE_ITEM = Registration.ITEMS.register("full_fluid_filter_pipe", FullPipeItem::new);
    public static final RegistryObject<Item> ENERGY_PIPE_ITEM = Registration.ITEMS.register("full_energy_pipe", FullPipeItem::new);
    public static final RegistryObject<Item> REDSTONE_PIPE_ITEM = Registration.ITEMS.register("full_redstone_pipe", FullPipeItem::new);


    public static final RegistryObject<MenuType<ItemFilterContainerMenu>> ITEM_FILTER_MENU_TYPE = MENU_TYPES.register("item_filter", () -> new MenuType<>(ItemFilterContainerMenu::createMenu));
    public static final RegistryObject<MenuType<FluidFilterContainerMenu>> FLUID_FILTER_MENU_TYPE = MENU_TYPES.register("fluid_filter", () -> new MenuType<>(FluidFilterContainerMenu::createFluidMenu));

    //called from main mod constructor
    public static void register() {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BLOCK_ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MENU_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    public static void registerFullPipeItems(){
        Registry.registerFullPipeItem(ItemPipe.class,ITEM_PIPE_ITEM.get());
        Registry.registerFullPipeItem(ItemFilterPipe.class,ITEM_FILTER_PIPE_ITEM.get());
        Registry.registerFullPipeItem(RedstonePipe.class,REDSTONE_PIPE_ITEM.get());
        Registry.registerFullPipeItem(FluidPipe.class,FLUID_PIPE_ITEM.get());
        Registry.registerFullPipeItem(FluidFilterPipe.class,FLUID_FILTER_PIPE_ITEM.get());
        Registry.registerFullPipeItem(EnergyPipe.class,ENERGY_PIPE_ITEM.get());
    }

}
