package com.dannyandson.tinypipes.setup;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import com.dannyandson.tinypipes.components.full.RefinedStorageCablePipe;

import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.refinedmods.refinedstorage.common.Platform;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * All Refined Storage API interaction for the {@link RefinedStorageCablePipe}, isolated here so it is
 * only class-loaded when RS is present (call sites are guarded by {@code TinyPipes.RS_LOADED}). RS finds
 * neighbors via a per-BlockPos NeoForge capability, which we register for the shared pipe BE and answer
 * only when a cable is present — so RS sees exactly one cable per block. Lifecycle mirrors RS's own
 * {@code AbstractNetworkNodeContainerBlockEntity}.
 */
public final class RefinedStorageIntegration {

    /** Energy the cable draws from the network; 0 keeps Tiny Pipes cables non-punishing.*/
    private static final long CABLE_ENERGY_USAGE = 0L;

    private RefinedStorageIntegration() {
    }

    /** Registers the RS container-provider capability for the shared pipe BE; added to the mod bus only when RS is loaded. */
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
                Registration.PIPE_BLOCK_ENTITY.get(),
                (pipeBlockEntity, side) -> {
                    AbstractFullPipe pipe = pipeBlockEntity.getPipe(RefinedStorageCablePipe.SLOT);
                    if (pipe instanceof RefinedStorageCablePipe cable) {
                        return (NetworkNodeContainerProvider) cable.getRsProvider();
                    }
                    return null;
                }
        );
    }

    /** Lazily build the network node + in-world container + provider for a cable, wired as RS's own block entities do. */
    private static NetworkNodeContainerProvider getOrCreateProvider(final PipeBlockEntity be,
                                                                    final RefinedStorageCablePipe cable) {
        Object existing = cable.getRsProvider();
        if (existing != null) {
            return (NetworkNodeContainerProvider) existing;
        }
        SimpleNetworkNode node = new SimpleNetworkNode(CABLE_ENERGY_USAGE);
        NetworkNodeContainerProvider provider = RefinedStorageApi.INSTANCE.createNetworkNodeContainerProvider();
        provider.addContainer(
                RefinedStorageApi.INSTANCE.createNetworkNodeContainer(be, node)
                        .name("cable")
                        .build()
        );
        cable.setRsProvider(provider);
        return provider;
    }

    /** Add the cable to the RS network when it enters the world (chunk load or placement). Server-side only. */
    public static void onConnect(final PipeBlockEntity be, final RefinedStorageCablePipe cable) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        getOrCreateProvider(be, cable).initialize(level, () -> { });
    }

    /** Remove the cable from the RS network when it leaves the world (block removed or wrenched). Server-side only. */
    public static void onDisconnect(final PipeBlockEntity be, final RefinedStorageCablePipe cable) {
        Level level = be.getLevel();
        Object provider = cable.getRsProvider();
        if (level == null || level.isClientSide || provider == null) {
            return;
        }
        ((NetworkNodeContainerProvider) provider).remove(level);
    }

    /**
     * Recompute which sides have an RS connection and store it on the cable for rendering, marking the
     * render cache dirty and syncing to clients when the connected set changes. Server-side only.
     */
    public static void refreshConnections(final PipeBlockEntity be, final RefinedStorageCablePipe cable) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        Map<Direction, Boolean> result = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            // Safely variant only: plain getContainerProvider force-loads unloaded neighbor chunks
            // (IMMEDIATE), which cascades into reentrant chunk loading and hangs world load.
            NetworkNodeContainerProvider neighbor =
                    Platform.INSTANCE.getContainerProviderSafely(level, neighborPos, direction.getOpposite());
            result.put(direction, neighbor != null);
        }
        if (cable.setConnections(result)) {
            be.markRenderDirty();
            be.sync();
        }
    }
}