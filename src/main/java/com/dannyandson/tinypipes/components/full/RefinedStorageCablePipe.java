package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.setup.ClientSetup;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A Refined Storage network cable that lives as a sub-pipe inside the shared {@link PipeBlockEntity},
 * using the sentinel slot {@link #SLOT} (not a 2x2 quadrant) and rendered through the block's center
 * core; at most one per block. Holds <b>no RS types</b> — the provider is an opaque {@link Object} and
 * all RS calls live in {@code RefinedStorageIntegration}, loaded only when {@link TinyPipes#RS_LOADED}.
 */
public class RefinedStorageCablePipe extends AbstractFullPipe {

    /** Sentinel slot id, distinct from the four quadrant slots (0-3). */
    public static final int SLOT = 4;

    /** Tint used for the center-core cable geometry (steel blue-gray). */
    public static final int CABLE_COLOR = 0xFF9DB2C4;

    /**
     * RS {@code NetworkNodeContainerProvider}, kept opaque so this class references no RS types.
     * Created/consumed only via RefinedStorageIntegration; server-side only, never set on the client.
     */
    @Nullable
    private Object rsProvider = null;

    /**
     * Which sides currently have an RS connection. Computed server-side by RefinedStorageIntegration,
     * persisted to NBT, read by the renderer for connection nubs — render state only, not the RS graph.
     */
    private final Map<Direction, Boolean> connections = new EnumMap<>(Direction.class);

    /**
     * Post-load countdown during which connections are periodically re-evaluated: neighbors in adjacent
     * chunks may not exist when the cable first loads, so one check can miss them. Server-side; not persisted.
     */
    private int loadSettleTicks = 0;

    /** Begin the post-load re-evaluation window (~2 seconds). */
    public void beginLoadSettle() {
        this.loadSettleTicks = 40;
    }

    @Override
    public int slotPos() {
        return SLOT;
    }

    @Override
    public TextureAtlasSprite getSprite() {
        // Center bundle uses this when the cable is the only pipe; colored geometry is drawn by the renderer.
        return RenderHelper.getSprite(ClientSetup.PIPE_TEXTURE);
    }

    @Override
    public int getColor() {
        return CABLE_COLOR;
    }

    /** Opaque accessor used by RefinedStorageIntegration only. */
    @Nullable
    public Object getRsProvider() {
        return rsProvider;
    }

    /** Opaque setter used by RefinedStorageIntegration only. */
    public void setRsProvider(@Nullable Object provider) {
        this.rsProvider = provider;
    }

    public boolean isConnected(Direction direction) {
        return Boolean.TRUE.equals(connections.get(direction));
    }

    @Override
    public void appendOverlayInfo(List<Component> lines, PipeBlockEntity pipeBlockEntity, Direction side) {
        // RS cables track their own per-side connection set rather than the push/pull side state,
        // so the generic state line doesn't apply; the type name in the header is enough here.
    }

    /**
     * Replace the connection state. Returns true if anything changed (so callers can decide whether
     * to mark the render cache dirty / sync).
     */
    public boolean setConnections(Map<Direction, Boolean> newConnections) {
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            boolean now = Boolean.TRUE.equals(newConnections.get(direction));
            if (isConnected(direction) != now) {
                changed = true;
            }
            connections.put(direction, now);
        }
        return changed;
    }

    @Override
    public void autoConnectOnPlace(PipeBlockEntity pipeBlockEntity) {
        // Refined Storage owns connection logic for cables; the Tiny Pipes per-side auto-connect
        // does not apply here. Render connections are refreshed via the BlockEntity lifecycle.
    }

    @Override
    public boolean onPlace(PipeBlockEntity pipeBlockEntity, ItemStack itemStack) {
        // Join the RS network and refresh render connections when the cable is placed into a block.
        if (TinyPipes.RS_LOADED
                && pipeBlockEntity.getLevel() != null
                && !pipeBlockEntity.getLevel().isClientSide) {
            com.dannyandson.tinypipes.setup.RefinedStorageIntegration.onConnect(pipeBlockEntity, this);
            com.dannyandson.tinypipes.setup.RefinedStorageIntegration.refreshConnections(pipeBlockEntity, this);
            pipeBlockEntity.getLevel().invalidateCapabilities(pipeBlockEntity.getBlockPos());
            // Trigger a neighbor *shape* update now that our capability is live. Refined Storage's
            // CableBlock recomputes its visual connections in updateShape() (not neighborChanged), so a
            // shape update is what makes an already-placed RS cable re-scan and draw the connection.
            // Minecraft's own placement shape-update fires before onConnect, and adding the cable to an
            // existing pipe block changes no block state at all, so this explicit re-poke is required.
            pipeBlockEntity.getBlockState().updateNeighbourShapes(pipeBlockEntity.getLevel(),
                    pipeBlockEntity.getBlockPos(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
        return super.onPlace(pipeBlockEntity, itemStack);
    }

    @Override
    public void onRemove(PipeBlockEntity pipeBlockEntity) {
        // Leave the RS network while the provider is still valid (before any block removal).
        if (TinyPipes.RS_LOADED
                && pipeBlockEntity.getLevel() != null
                && !pipeBlockEntity.getLevel().isClientSide) {
            com.dannyandson.tinypipes.setup.RefinedStorageIntegration.onDisconnect(pipeBlockEntity, this);
            pipeBlockEntity.getLevel().invalidateCapabilities(pipeBlockEntity.getBlockPos());
        }
    }

    @Override
    public void onLoad(PipeBlockEntity pipeBlockEntity) {
        // Join the RS network on load. Do NOT scan neighbors here: clearRemoved runs in chunk post-load,
        // where a neighbor lookup blocks on a not-yet-FULL chunk and hangs world load. The settle window
        // refreshes render connections once chunks are FULL.
        if (TinyPipes.RS_LOADED
                && pipeBlockEntity.getLevel() != null
                && !pipeBlockEntity.getLevel().isClientSide) {
            com.dannyandson.tinypipes.setup.RefinedStorageIntegration.onConnect(pipeBlockEntity, this);
            beginLoadSettle();
        }
    }

    @Override
    public void onUnload(PipeBlockEntity pipeBlockEntity) {
        // Leave the RS network when the block entity is removed or its chunk unloads.
        if (TinyPipes.RS_LOADED
                && pipeBlockEntity.getLevel() != null
                && !pipeBlockEntity.getLevel().isClientSide) {
            com.dannyandson.tinypipes.setup.RefinedStorageIntegration.onDisconnect(pipeBlockEntity, this);
        }
    }

    @Override
    public boolean neighborChanged(PipeBlockEntity pipeBlockEntity, @Nullable Direction updateDirection) {
        boolean change = super.neighborChanged(pipeBlockEntity, updateDirection);
        // Recompute which sides have an RS connection so the renderer can update. Guarded so the RS
        // integration class is never loaded when Refined Storage is absent.
        if (TinyPipes.RS_LOADED
                && pipeBlockEntity.getLevel() != null
                && !pipeBlockEntity.getLevel().isClientSide) {
            com.dannyandson.tinypipes.setup.RefinedStorageIntegration.refreshConnections(pipeBlockEntity, this);
        }
        return change;
    }

    @Override
    public boolean tick(PipeBlockEntity pipeBlockEntity) {
        boolean change = super.tick(pipeBlockEntity);
        if (loadSettleTicks > 0) {
            loadSettleTicks--;
            // Re-evaluate render connections across the settle window; refreshConnections only syncs on
            // actual change, so repeats are cheap. Guarded so the RS class stays unloaded when RS is absent.
            if (TinyPipes.RS_LOADED
                    && loadSettleTicks % 10 == 0
                    && pipeBlockEntity.getLevel() != null
                    && !pipeBlockEntity.getLevel().isClientSide) {
                com.dannyandson.tinypipes.setup.RefinedStorageIntegration.refreshConnections(pipeBlockEntity, this);
            }
        }
        return change;
    }

    @Override
    public void rotate(Rotation rotation) {
        super.rotate(rotation);
        rotateMap(connections, rotation);
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();
        if (!connections.isEmpty()) {
            CompoundTag conn = new CompoundTag();
            for (Map.Entry<Direction, Boolean> entry : connections.entrySet()) {
                if (entry.getKey() != null) {
                    conn.putBoolean(entry.getKey().name(), Boolean.TRUE.equals(entry.getValue()));
                }
            }
            nbt.put("rsConnections", conn);
        }
        return nbt;
    }

    @Override
    public void readNBT(CompoundTag compoundTag) {
        super.readNBT(compoundTag);
        connections.clear();
        if (compoundTag.contains("rsConnections")) {
            CompoundTag conn = compoundTag.getCompound("rsConnections");
            for (String key : conn.getAllKeys()) {
                try {
                    connections.put(Direction.valueOf(key), conn.getBoolean(key));
                } catch (IllegalArgumentException ignored) {
                    // unknown direction key; skip
                }
            }
        }
    }
}