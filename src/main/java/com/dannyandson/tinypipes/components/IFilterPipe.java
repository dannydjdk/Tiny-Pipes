package com.dannyandson.tinypipes.components;

import net.minecraft.inventory.IInventory;

public interface IFilterPipe extends IInventory {
    boolean getBlackList();
    void serverSetBlacklist(boolean blacklist);
}
