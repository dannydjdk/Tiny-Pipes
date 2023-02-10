package com.dannyandson.tinypipes.api;

import com.dannyandson.tinypipes.components.full.AbstractFullPipe;
import net.minecraft.world.item.Item;

import javax.annotation.CheckForNull;
import java.util.HashMap;
import java.util.Map;

public class Registry {

    private static final Map<Item, Class<? extends AbstractFullPipe>> itemFullPipeMap = new HashMap<>();
    private static final Map<Class<? extends AbstractFullPipe>, Item> fullPipeItemMap = new HashMap<>();

    public static void registerFullPipeItem(Class<? extends AbstractFullPipe> fullPipeClass, Item item){
        itemFullPipeMap.put(item,fullPipeClass);
        fullPipeItemMap.put(fullPipeClass,item);
    }

    @CheckForNull
    public static Item getFullPipeItemFromClass(Class<? extends AbstractFullPipe> fullPipeClass){
        return fullPipeItemMap.get(fullPipeClass);
    }

    @CheckForNull
    public static AbstractFullPipe getFullPipeFromItem(Item item){
        try {
            return itemFullPipeMap.get(item).getConstructor().newInstance();
        }catch (Exception exception){
            return null;
        }
    }

    @CheckForNull
    public static Class<? extends AbstractFullPipe> getFullPipeClassFromItem(Item item){
            return itemFullPipeMap.get(item);
    }

}
