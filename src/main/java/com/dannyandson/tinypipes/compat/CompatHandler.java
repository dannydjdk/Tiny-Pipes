package com.dannyandson.tinypipes.compat;

import com.dannyandson.tinypipes.compat.theoneprobe.PipeProvider;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;

/**
 * Wires up optional overlay-mod integrations. The One Probe is registered via IMC and only when it is
 * loaded, so its classes stay unloaded otherwise. Jade discovers its own {@code @WailaPlugin}, so it
 * needs nothing here.
 */
public class CompatHandler {

    public static void register() {
        if (ModList.get().isLoaded("theoneprobe")) {
            InterModComms.sendTo("theoneprobe", "getTheOneProbe", PipeProvider::new);
        }
    }
}