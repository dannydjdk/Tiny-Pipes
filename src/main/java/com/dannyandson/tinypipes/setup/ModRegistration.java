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
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRegistration {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TinyPipes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TinyPipes.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TinyPipes.MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, TinyPipes.MODID);
    private static final DeferredRegister<CreativeModeTab> TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TinyPipes.MODID);

    public static final DeferredBlock<PipeBlock> PIPE_BLOCK = BLOCKS.registerBlock("pipe_block",
            PipeBlock::new,
            BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.0f).dynamicShape());

    public static final Supplier<BlockEntityType<PipeBlockEntity>> PIPE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("pipe_block", () -> new BlockEntityType<>(PipeBlockEntity::new, PIPE_BLOCK.get()));

    //Not so Tiny Pipe items
    public static final DeferredItem<Item> ITEM_PIPE_ITEM = ITEMS.registerItem("full_item_pipe", FullPipeItem::new);
    public static final DeferredItem<Item> ITEM_FILTER_PIPE_ITEM = ITEMS.registerItem("full_item_filter_pipe", FullPipeItem::new);
    public static final DeferredItem<Item> FLUID_PIPE_ITEM = ITEMS.registerItem("full_fluid_pipe", FullPipeItem::new);
    public static final DeferredItem<Item> FLUID_FILTER_PIPE_ITEM = ITEMS.registerItem("full_fluid_filter_pipe", FullPipeItem::new);
    public static final DeferredItem<Item> ENERGY_PIPE_ITEM = ITEMS.registerItem("full_energy_pipe", FullPipeItem::new);
    public static final DeferredItem<Item> REDSTONE_PIPE_ITEM = ITEMS.registerItem("full_redstone_pipe", FullPipeItem::new);

    public static final DeferredItem<Item> PIPE_WRENCH_ITEM = ITEMS.registerItem("pipe_wrench", PipeWrenchItem::new);
    public static final DeferredItem<Item> SPEED_UPGRADE_ITEM = ITEMS.registerItem("speed_upgrade", SpeedUpgradeItem::new);

    public static final Supplier<MenuType<ItemFilterContainerMenu>> ITEM_FILTER_MENU_TYPE = MENU_TYPES.register("item_filter", () -> new MenuType<>(ItemFilterContainerMenu::createMenu, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<FluidFilterContainerMenu>> FLUID_FILTER_MENU_TYPE = MENU_TYPES.register("fluid_filter", () -> new MenuType<>(FluidFilterContainerMenu::createFluidMenu, FeatureFlags.DEFAULT_FLAGS));

    public static final TagKey<Block> MINEABLE_WITH_WRENCH = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(TinyPipes.MODID, "mineable/wrench"));

    public static Supplier<CreativeModeTab> CREATIVE_TAB = TAB.register("tinypipestab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("tinypipes"))
                    .icon(() -> new ItemStack(ModRegistration.REDSTONE_PIPE_ITEM.get()))
                    .displayItems((parameters, output) -> ITEMS.getEntries().forEach(o -> output.accept(o.get())))
                    .build());


    //called from main mod constructor
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        TAB.register(modEventBus);
    }

    public static void registerFullPipeItems() {
        Registry.registerFullPipeItem(ItemPipe.class, ITEM_PIPE_ITEM.get());
        Registry.registerFullPipeItem(ItemFilterPipe.class, ITEM_FILTER_PIPE_ITEM.get());
        Registry.registerFullPipeItem(RedstonePipe.class, REDSTONE_PIPE_ITEM.get());
        Registry.registerFullPipeItem(FluidPipe.class, FLUID_PIPE_ITEM.get());
        Registry.registerFullPipeItem(FluidFilterPipe.class, FLUID_FILTER_PIPE_ITEM.get());
        Registry.registerFullPipeItem(EnergyPipe.class, ENERGY_PIPE_ITEM.get());
    }
}
