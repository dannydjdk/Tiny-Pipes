package com.dannyandson.tinypipes.components.tiny;

import net.minecraft.world.Container;

public interface IFilterPipe extends Container {
    boolean getBlackList();
    void serverSetBlacklist(boolean blacklist);
}
