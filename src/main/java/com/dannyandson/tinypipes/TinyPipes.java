package com.dannyandson.tinypipes;

import com.dannyandson.tinypipes.setup.ModRegistration;
import com.dannyandson.tinypipes.setup.RegistrationTinyRedstone;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

@Mod(TinyPipes.MODID)
public class TinyPipes
{
    public static final String MODID = "tinypipes";
    public static final Logger LOGGER = LogManager.getLogger();

    // The default frequency for the pipes, used for redstone pipes
    public static final int defaultFrequency = 0x810E0C;
    // list of frequencies that can be used in the pipe : [0x810E0C,0,1,2,3,4,5,6,7,8,9,10,11,12,13,15]
    public static final List<Integer> possibleFrequencies = Arrays.stream(DyeColor.values()).map(dyeColor -> {
        if (dyeColor == DyeColor.RED) return defaultFrequency; // red is the default frequency
        return dyeColor.getId();
    }).toList();

    public TinyPipes(IEventBus modEventBus, ModContainer modContainer) {

        if(FMLEnvironment.getDist().isClient()) {
            // Menu screen registration handled via @SubscribeEvent in ClientSetup
        }

        modEventBus.addListener(this::setup);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);
        ModRegistration.register(modEventBus);
        if (ModList.get().isLoaded("tinyredstone"))
            RegistrationTinyRedstone.register();
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        if (ModList.get().isLoaded("tinyredstone"))
            RegistrationTinyRedstone.registerPanelCells();
        ModRegistration.registerFullPipeItems();
    }
}
