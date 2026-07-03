package com.dannyandson.tinypipes.components.tiny;

import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.IFilterPipe;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.gui.FluidFilterContainerMenu;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelCellSegment;
import com.dannyandson.tinyredstone.blocks.Side;
import com.dannyandson.tinyredstone.setup.ModRegistration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.minecraft.core.registries.BuiltInRegistries;

import org.jspecify.annotations.Nullable;
import java.util.Arrays;

public class FluidFilterPipe extends FluidPipe implements IFilterPipe {

    boolean changed = false;

    //saved fields
    private static final int filterSlots = 18;
    private String[] filters = new String[filterSlots];
    boolean blacklist = false;

    private static TextureAtlasSprite sprite = null;

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(RenderHelper.FLUID_FILTER_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public boolean onPlace(PanelCellPos cellPos, Player player) {
        ItemStack stack = ItemStack.EMPTY;
        if (player.getUsedItemHand()!=null)
            stack = player.getItemInHand(player.getUsedItemHand());
        if (stack == ItemStack.EMPTY)
            stack = player.getMainHandItem();
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            CompoundTag itemNBT = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            String filterString = itemNBT.getStringOr("filters", "");
            filters = Arrays.copyOf(filterString.split("\n",filterSlots),filterSlots);
            blacklist = itemNBT.getBooleanOr("blacklist", false);
        }

        return super.onPlace(cellPos, player);
    }

    @Override
    protected void populatePushWrapper(PanelCellPos cellPos, @Nullable Side side, FluidStack fluidStack, PushWrapper<ResourceHandler<FluidResource>> pushWrapper, int distance) {
        Identifier fluidReg = BuiltInRegistries.ITEM.getKey(fluidStack.getFluid().getBucket());
        boolean hasFluid = fluidReg != null && hasFluid(fluidReg.toString());
        if ((!blacklist && !hasFluid) || (blacklist && hasFluid)) {
            return;
        }

        super.populatePushWrapper(cellPos, side, fluidStack, pushWrapper, distance);
    }

    public boolean hasFluid(String itemRegistryName){
        for (String filter : filters) {
            if(filter!=null && filter.equals(itemRegistryName))
                return true;
        }
        return false;
    }

    //IFilterPipe interface implementation
    @Override
    public boolean getBlackList(){
        return blacklist;
    }

    public void serverSetBlacklist(boolean blacklist) {
        this.blacklist = blacklist;
        setChanged();
    }
    //end IFilterPipe

    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, Player player) {
        if (player.getMainHandItem().getItem() == ModRegistration.REDSTONE_WRENCH.get()
                || player.getMainHandItem().has(DataComponents.DYE))
            return super.onBlockActivated(cellPos, segmentClicked, player);

        if (player instanceof ServerPlayer) {
            ((ServerPlayer) player).openMenu(new FluidFilterContainerMenu.Provider(this));
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
        String filterString = compoundTag.getStringOr("filters", "");
        filters = Arrays.copyOf(filterString.split("\n",filterSlots),filterSlots);
        blacklist = compoundTag.getBooleanOr("blacklist", false);
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
        nbt.putBoolean("blacklist",blacklist);
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
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot<filters.length && filters[slot]!=null && !filters[slot].equals("null") && !filters[slot].isEmpty()) {
            net.minecraft.resources.Identifier rl = net.minecraft.resources.Identifier.tryParse(filters[slot]);
            if (rl != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(rl))
                return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(rl));
            return ItemStack.EMPTY;
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
        BucketItem bucketItem;
        if (!(itemStack.getItem() instanceof BucketItem))
            return;
        bucketItem=(BucketItem) itemStack.getItem();
        if (slot<filters.length && BuiltInRegistries.ITEM.getKey(bucketItem)!=null &&
                !bucketItem.content.equals(Fluids.EMPTY) &&
                !(bucketItem instanceof MobBucketItem)
        ) {
            String itemName = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
            if(hasFluid(itemName))
                return;
            filters[slot] = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
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