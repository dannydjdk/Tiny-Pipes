package com.dannyandson.tinypipes;

import com.dannyandson.tinypipes.setup.ClientSetup;
import com.dannyandson.tinypipes.setup.Registration;
import com.dannyandson.tinypipes.setup.RegistrationTinyRedstone;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TinyPipes.MODID)
public class TinyPipes
{
    public static final String MODID = "tinypipes";
    public static final Logger LOGGER = LogManager.getLogger();

    public TinyPipes(IEventBus modEventBus, ModContainer modContainer) {

        if(FMLEnvironment.dist.isClient()) {
            // Menu screen registration handled via @SubscribeEvent in ClientSetup
        }

        modEventBus.addListener(this::setup);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);
        Registration.register(modEventBus);
        if (ModList.get().isLoaded("tinyredstone"))
            RegistrationTinyRedstone.register();
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        if (ModList.get().isLoaded("tinyredstone"))
            RegistrationTinyRedstone.registerPanelCells();
        Registration.registerFullPipeItems();
    }
}
