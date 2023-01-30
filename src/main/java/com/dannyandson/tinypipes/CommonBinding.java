package com.dannyandson.tinypipes;

import com.dannyandson.tinypipes.api.Registry;
import com.dannyandson.tinypipes.blocks.PipeBlock;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TinyPipes.MODID)
public class CommonBinding {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        //allow creative players to remove cells by left clicking with wrench or cell item
        if (event.getEntity().isCreative() &&
                event.getEntity().level.getBlockState(event.getPos()).getBlock() instanceof PipeBlock pipeBlock &&
                (
                        event.getEntity().getMainHandItem().is(ItemTags.create(new ResourceLocation("forge", "tools/wrench"))) ||
                                Registry.getFullPipeClassFromItem(event.getEntity().getMainHandItem().getItem()) != null
                )
        ) {
            pipeBlock.attack(event.getEntity().level.getBlockState(event.getPos()), event.getEntity().level, event.getPos(), event.getEntity());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getEntity().isCrouching() && event.getEntity().getMainHandItem().is(ItemTags.create(new ResourceLocation("forge", "tools/wrench"))))
        {
            if (event.getEntity().level.getBlockEntity(event.getPos()) instanceof PipeBlockEntity pipeBlockEntity)
            {
                pipeBlockEntity.getBlockState().getBlock().playerWillDestroy(pipeBlockEntity.getLevel(),pipeBlockEntity.getBlockPos(),pipeBlockEntity.getBlockState(),event.getEntity());
                pipeBlockEntity.getLevel().removeBlock(pipeBlockEntity.getBlockPos(),false);
                event.setCanceled(true);
            }
        }
    }


}
