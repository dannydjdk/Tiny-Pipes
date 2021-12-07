package com.dannyandson.tinypipes;

import com.dannyandson.tinypipes.network.ModNetworkHandler;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinypipes.setup.Registration;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TinyPipes.MODID)
public class TinyPipes
{
    public static final String MODID = "tinypipes";
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public TinyPipes() {

        if(FMLEnvironment.dist.isClient()) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::init);
        }

        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        Registration.register();

    }

    private void setup(final FMLCommonSetupEvent event)
    {
        ModNetworkHandler.registerMessages();
        Registration.registerPanelCells();
    }

    public static final ItemGroup ITEM_GROUP = new ItemGroup(MODID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Registration.REDSTONE_PIPE_ITEM.get());
        }
    };
}
