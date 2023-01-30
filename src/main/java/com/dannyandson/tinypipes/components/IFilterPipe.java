package com.dannyandson.tinypipes.components;

import net.minecraft.world.Container;

public interface IFilterPipe extends Container {
    boolean getBlackList();
    void serverSetBlacklist(boolean blacklist);
}
