package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.IFilterPipe;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.gui.ItemFilterContainerMenu;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

public class ItemFilterPipe extends ItemPipe implements IFilterPipe {

    boolean changed = false;

    //saved fields
    private static final int filterSlots = 18;
    private String[] filters = new String[filterSlots];
    boolean blacklist = false;

    private static TextureAtlasSprite sprite = null;

    @Override
    public TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(RenderHelper.ITEM_FILTER_PIPE_TEXTURE);
        return sprite;
    }


    @Override
    public boolean onPlace(PipeBlockEntity pipeBlockEntity, ItemStack stack) {
        if (stack != ItemStack.EMPTY && stack.hasTag()) {
            CompoundTag itemNBT = stack.getTag();
            String filterString = itemNBT.getString("filters");
            filters = Arrays.copyOf(filterString.split("\n",filterSlots),filterSlots);
        }

        return super.onPlace(pipeBlockEntity, stack);
    }

    @Override
    protected void populatePushWrapper(PipeBlockEntity pipeBlockEntity, @org.jetbrains.annotations.Nullable Direction side, ItemStack itemStack, PushWrapper<IItemHandler> pushWrapper, int distance) {
        ResourceLocation itemReg = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        boolean hasItem = itemReg != null && hasItem(itemReg.toString());
        if ((!blacklist && !hasItem) || (blacklist && hasItem)) {
            return;
        }
        super.populatePushWrapper(pipeBlockEntity, side, itemStack, pushWrapper, distance);
    }

    public boolean hasItem(String itemRegistryName){
        for (String filter : filters) {
            if(filter!=null && filter.equals(itemRegistryName))
                return true;
        }
        return false;
    }

    public boolean getBlackList(){
        return blacklist;
    }

    public void serverSetBlacklist(boolean blacklist) {
        this.blacklist = blacklist;
        setChanged();
    }

    @Override
    public void openGUI(PipeBlockEntity pipeBlockEntity, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,new ItemFilterContainerMenu.Provider(this));
        }
    }

    @Override
    public boolean tick(PipeBlockEntity pipeBlockEntity) {
        if (changed){
            pipeBlockEntity.sync();
            changed=false;
        }
        return super.tick(pipeBlockEntity);
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();
        String filterString = "";
        for (String filter : filters){
            filterString = ((filterString.length()>0)?filterString+"\n":"") + filter;
        }
        nbt.putString("filters",filterString);
        nbt.putBoolean("blacklist",blacklist);

        return nbt;
    }

    @Override
    public void readNBT(CompoundTag compoundTag) {
        super.readNBT(compoundTag);
        String filterString = compoundTag.getString("filters");
        filters = Arrays.copyOf(filterString.split("\n",filterSlots),filterSlots);
        blacklist = compoundTag.getBoolean("blacklist");
    }

    @Override
    public CompoundTag getItemTag() {
        boolean empty = true;
        CompoundTag nbt = new CompoundTag();
        String filterString = "";
        for (String filter : filters){
            filterString = ((filterString.length()>0)?filterString+"\n":"") + filter;
            if (filter!=null && !filter.equals("null") && filter.length()>0)
                empty=false;
        }
        if (empty)return null;

        nbt.putString("filters",filterString);
        return nbt;
    }

    //Container Implementation

    @Override
    public int getContainerSize() {
        return filterSlots;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot<filters.length && filters[slot]!=null && !filters[slot].equals("null") && !filters[slot].isEmpty()) {
            CompoundTag itemNbt = new CompoundTag();
            itemNbt.putString("id", filters[slot]);
            itemNbt.putInt("Count",1);
            return ItemStack.of(itemNbt);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int p_18943_) {
        return removeItemNoUpdate(slot);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot<filters.length)
            filters[slot]="";
        setChanged();
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        if (slot<filters.length && ForgeRegistries.ITEMS.getKey(itemStack.getItem())!=null) {
            String itemName = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
            if(hasItem(itemName))
                return;
            filters[slot] = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        this.changed=true;
    }

    @Override
    public boolean stillValid(Player p_18946_) {
        return true;
    }

    @Override
    public void clearContent() {
        filters=new String[filterSlots];
        setChanged();
    }

    //End Container Implementation
}
