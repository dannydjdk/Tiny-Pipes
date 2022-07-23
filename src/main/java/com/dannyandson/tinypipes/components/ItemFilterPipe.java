package com.dannyandson.tinypipes.components;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.gui.ItemFilterContainerMenu;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelCellSegment;
import com.dannyandson.tinyredstone.blocks.Side;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;

public class ItemFilterPipe extends ItemPipe implements IFilterPipe{

    boolean changed = false;

    //saved fields
    private static final int filterSlots = 18;
    private String[] filters = new String[filterSlots];
    boolean blacklist = false;

    public static final ResourceLocation ITEM_FILTER_PIPE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/item_filter_pipe");
    private static TextureAtlasSprite sprite = null;

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(ITEM_FILTER_PIPE_TEXTURE);
        return sprite;
    }


    @Override
    public boolean onPlace(PanelCellPos cellPos, Player player) {
        ItemStack stack = ItemStack.EMPTY;
        if (player.getUsedItemHand()!=null)
            stack = player.getItemInHand(player.getUsedItemHand());
        if (stack == ItemStack.EMPTY)
            stack = player.getMainHandItem();
        if (stack.hasTag()) {
            CompoundTag itemNBT = stack.getTag();
            String filterString = itemNBT.getString("filters");
            filters = Arrays.copyOf(filterString.split("\n",filterSlots),filterSlots);
        }

        return super.onPlace(cellPos, player);
    }

    @Override
    protected void populatePushWrapper(PanelCellPos cellPos, @Nullable Side side, ItemStack itemStack, PushWrapper<IItemHandler> pushWrapper, int distance) {
        ResourceLocation itemReg = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        boolean hasItem = itemReg != null && hasItem(itemReg.toString());
        if ((!blacklist && !hasItem) || (blacklist && hasItem)) {
            return;
        }
        super.populatePushWrapper(cellPos, side, itemStack, pushWrapper, distance);
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
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, Player player) {
        if (player.getMainHandItem().getItem() == Registration.REDSTONE_WRENCH.get())
            return super.onBlockActivated(cellPos, segmentClicked, player);

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,new ItemFilterContainerMenu.Provider(this));
        }
        return false;
    }

    @Override
    public boolean tick(PanelCellPos cellPos) {
        if (changed){
            cellPos.getPanelTile().sync();
            changed=false;
        }
        return super.tick(cellPos);
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
