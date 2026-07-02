package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.components.full.RefinedStorageCablePipe;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Refined Storage API interaction for the {@link RefinedStorageCablePipe}, isolated here so the rest
 * of the mod never references RS types directly (call sites are guarded by {@code TinyPipes.RS_LOADED}).
 *
 * <p>[RS-DISABLED 26.2] Refined Storage has no 26.2 release yet, so the RS bodies are stubbed out and
 * the {@code com.refinedmods.*} imports removed, letting the mod build without the RS dependency.
 * {@code TinyPipes.RS_LOADED} is false while RS is absent, so these methods are never invoked.
 *
 * <p>To re-enable: add the RS dependency back in {@code build.gradle}, then restore the original
 * method bodies and {@code com.refinedmods.*} imports from the 26.1 source (this is the only file that
 * touched the RS API — the cable pipe itself has always been RS-import-free and needs no change).
 */
public final class RefinedStorageIntegration {

    private RefinedStorageIntegration() {
    }

    /** Registers the RS container-provider capability for the shared pipe BE. */
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        // [RS-DISABLED 26.2] no-op until Refined Storage is available for 26.2.
    }

    /** Add the cable to the RS network when it enters the world. Server-side only. */
    public static void onConnect(final PipeBlockEntity be, final RefinedStorageCablePipe cable) {
        // [RS-DISABLED 26.2] no-op until Refined Storage is available for 26.2.
    }

    /** Remove the cable from the RS network when it leaves the world. Server-side only. */
    public static void onDisconnect(final PipeBlockEntity be, final RefinedStorageCablePipe cable) {
        // [RS-DISABLED 26.2] no-op until Refined Storage is available for 26.2.
    }

    /** Recompute which sides have an RS connection and store it on the cable for rendering. Server-side only. */
    public static void refreshConnections(final PipeBlockEntity be, final RefinedStorageCablePipe cable) {
        // [RS-DISABLED 26.2] no-op until Refined Storage is available for 26.2.
    }
}