package com.dannyandson.tinypipes;

import com.dannyandson.tinypipes.network.ModNetworkHandler;
import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinypipes.setup.Registration;
import com.dannyandson.tinypipes.setup.RegistrationTinyRedstone;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TinyPipes.MODID)
public class TinyPipes
{
    public static final String MODID = "tinypipes";
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    // The default frequency for the pipes, used for redstone pipes
    public static final int defaultFrequency = 0x810E0C;
    // list of frequencies that can be used in the pipe : [0x810E0C,0,1,2,3,4,5,6,7,8,9,10,11,12,13,15]
    public static final List<Integer> possibleFrequencies = Arrays.stream(DyeColor.values()).map(dyeColor -> {
        if (dyeColor == DyeColor.RED) return defaultFrequency; // red is the default frequency
        return dyeColor.getId();
    }).toList();

    public TinyPipes() {

        if(FMLEnvironment.dist.isClient()) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::init);
        }

        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);
        Registration.register();
        if (ModList.get().isLoaded("tinyredstone"))
            RegistrationTinyRedstone.register();
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        ModNetworkHandler.registerMessages();
        if (ModList.get().isLoaded("tinyredstone"))
            RegistrationTinyRedstone.registerPanelCells();
        Registration.registerFullPipeItems();
    }
}
