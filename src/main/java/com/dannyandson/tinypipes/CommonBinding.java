package com.dannyandson.tinypipes;

import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.blocks.PipeBlock;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TinyPipes.MODID)
public class CommonBinding {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().isCreative() &&
                event.getEntity().level().getBlockState(event.getPos()).getBlock() instanceof PipeBlock pipeBlock &&
                (
                        event.getEntity().getMainHandItem().is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"))) ||
                                Registry.getFullPipeClassFromItem(event.getEntity().getMainHandItem().getItem()) != null
                )
        ) {
            pipeBlock.attack(event.getEntity().level().getBlockState(event.getPos()), event.getEntity().level(), event.getPos(), event.getEntity());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getEntity().isCrouching() && event.getEntity().getMainHandItem().is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"))))
        {
            if (event.getEntity().level().getBlockEntity(event.getPos()) instanceof PipeBlockEntity pipeBlockEntity)
            {
                pipeBlockEntity.getBlockState().getBlock().playerWillDestroy(pipeBlockEntity.getLevel(),pipeBlockEntity.getBlockPos(),pipeBlockEntity.getBlockState(),event.getEntity());
                pipeBlockEntity.getLevel().removeBlock(pipeBlockEntity.getBlockPos(),false);
                event.setCanceled(true);
            }
        }
    }
}
