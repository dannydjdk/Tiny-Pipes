package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.blocks.PipeBlock;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.components.full.*;
import com.dannyandson.tinypipes.gui.FluidFilterContainerMenu;
import com.dannyandson.tinypipes.gui.ItemFilterContainerMenu;
import com.dannyandson.tinypipes.items.FullPipeItem;
import com.dannyandson.tinypipes.items.PipeWrenchItem;
import com.dannyandson.tinypipes.items.SpeedUpgradeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Registration {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, TinyPipes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TinyPipes.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, TinyPipes.MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, TinyPipes.MODID);
    private static final DeferredRegister<CreativeModeTab> TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TinyPipes.MODID);

    public static final Supplier<PipeBlock> PIPE_BLOCK = BLOCKS.register("pipe_block", PipeBlock::new);
    public static final Supplier<BlockEntityType<PipeBlockEntity>> PIPE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("pipe_block", ()->BlockEntityType.Builder.of(PipeBlockEntity::new,PIPE_BLOCK.get()).build(null));

    //Not so Tiny Pipe items
    public static final Supplier<Item> ITEM_PIPE_ITEM = Registration.ITEMS.register("full_item_pipe", FullPipeItem::new);
    public static final Supplier<Item> ITEM_FILTER_PIPE_ITEM = Registration.ITEMS.register("full_item_filter_pipe", FullPipeItem::new);
    public static final Supplier<Item> FLUID_PIPE_ITEM = Registration.ITEMS.register("full_fluid_pipe", FullPipeItem::new);
    public static final Supplier<Item> FLUID_FILTER_PIPE_ITEM = Registration.ITEMS.register("full_fluid_filter_pipe", FullPipeItem::new);
    public static final Supplier<Item> ENERGY_PIPE_ITEM = Registration.ITEMS.register("full_energy_pipe", FullPipeItem::new);
    public static final Supplier<Item> REDSTONE_PIPE_ITEM = Registration.ITEMS.register("full_redstone_pipe", FullPipeItem::new);

    public static final Supplier<Item> PIPE_WRENCH_ITEM = Registration.ITEMS.register("pipe_wrench", PipeWrenchItem::new);
    public static final Supplier<Item> SPEED_UPGRADE_ITEM = Registration.ITEMS.register("speed_upgrade", SpeedUpgradeItem::new);

    // Refined Storage cable item. Null unless RS is installed; registered via registerRefinedStorageItems().
    public static Supplier<Item> RS_CABLE_ITEM = null;

    public static final Supplier<MenuType<ItemFilterContainerMenu>> ITEM_FILTER_MENU_TYPE = MENU_TYPES.register("item_filter", () -> new MenuType<>(ItemFilterContainerMenu::createMenu, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<FluidFilterContainerMenu>> FLUID_FILTER_MENU_TYPE = MENU_TYPES.register("fluid_filter", () -> new MenuType<>(FluidFilterContainerMenu::createFluidMenu, FeatureFlags.DEFAULT_FLAGS));

    public static final TagKey<Block> MINEABLE_WITH_WRENCH = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TinyPipes.MODID,"mineable/wrench"));

    public static Supplier<CreativeModeTab> CREATIVE_TAB = TAB.register("tinypipestab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("tinypipes"))
                    .icon(() -> new ItemStack(Registration.REDSTONE_PIPE_ITEM.get()))
                    .displayItems((parameters,output) ->ITEMS.getEntries().forEach(o -> output.accept(o.get())))
                    .build());


    //called from main mod constructor
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        TAB.register(modEventBus);
    }

    public static void registerFullPipeItems(){
        Registry.registerFullPipeItem(ItemPipe.class,ITEM_PIPE_ITEM.get());
        Registry.registerFullPipeItem(ItemFilterPipe.class,ITEM_FILTER_PIPE_ITEM.get());
        Registry.registerFullPipeItem(RedstonePipe.class,REDSTONE_PIPE_ITEM.get());
        Registry.registerFullPipeItem(FluidPipe.class,FLUID_PIPE_ITEM.get());
        Registry.registerFullPipeItem(FluidFilterPipe.class,FLUID_FILTER_PIPE_ITEM.get());
        Registry.registerFullPipeItem(EnergyPipe.class,ENERGY_PIPE_ITEM.get());
        if (RS_CABLE_ITEM != null)
            Registry.registerFullPipeItem(com.dannyandson.tinypipes.components.full.RefinedStorageCablePipe.class, RS_CABLE_ITEM.get());
    }

    /**
     * Registers the Refined Storage cable item. Called from the TinyPipes constructor (before the
     * deferred item register fires) only when Refined Storage is installed.
     */
    public static void registerRefinedStorageItems(){
        RS_CABLE_ITEM = ITEMS.register("full_rs_cable", FullPipeItem::new);
    }
}