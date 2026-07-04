package com.dannyandson.tinypipes.compat.jade;

import com.dannyandson.tinypipes.blocks.PipeBlock;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade integration entry point. Discovered automatically by Jade through the {@link WailaPlugin}
 * annotation, so it loads only when Jade is present and is never referenced by the main mod — no
 * {@code ModList} guard required. All Jade API use stays inside this package.
 */
@WailaPlugin
public class TinyPipesJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PipeComponentProvider.INSTANCE, PipeBlock.class);
        registration.registerBlockIcon(PipeComponentProvider.INSTANCE, PipeBlock.class);
    }
}