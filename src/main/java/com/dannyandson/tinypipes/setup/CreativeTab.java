package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.TinyPipes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TinyPipes.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)

public class CreativeTab {

    public static CreativeModeTab TAB;

    @SubscribeEvent
    public static void register(CreativeModeTabEvent.Register event) {
        TAB = event.registerCreativeModeTab(
                new ResourceLocation("tinypipes", TinyPipes.MODID), builder -> builder
                        .icon(() -> new ItemStack(Registration.REDSTONE_PIPE_ITEM.get()))
                        .displayItems((featureFlags, output, hasOp) -> Registration.ITEMS.getEntries().forEach(o -> output.accept(o.get())))
                        .title(Component.translatable("tinypipes"))
        );
    }

}
