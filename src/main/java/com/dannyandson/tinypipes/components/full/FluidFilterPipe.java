package com.dannyandson.tinypipes.components.full;

import com.dannyandson.tinypipes.blocks.PipeBlockEntity;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.components.IFilterPipe;
import com.dannyandson.tinypipes.components.RenderHelper;
import com.dannyandson.tinypipes.gui.FluidFilterContainerMenu;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;

public class FluidFilterPipe extends FluidPipe implements IFilterPipe {

    boolean changed = false;

    //saved fields
    private static final int filterSlots = 18;
    private String[] filters = new String[filterSlots];
    boolean blacklist = false;

    private static TextureAtlasSprite sprite = null;

    @Override
    public TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = RenderHelper.getSprite(RenderHelper.FLUID_FILTER_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public boolean onPlace(PipeBlockEntity pipeBlockEntity, ItemStack stack) {
        if (stack != ItemStack.EMPTY && stack.hasTag()) {
            CompoundTag itemNBT = stack.getTag();
            String filterString = itemNBT.getString("filters");
            filters = Arrays.copyOf(filterString.split("\n",filterSlots),filterSlots);
            blacklist = itemNBT.getBoolean("blacklist");
        }

        return super.onPlace(pipeBlockEntity, stack);
    }

    @Override
    protected void populatePushWrapper(PipeBlockEntity pipeBlockEntity, @Nullable Direction side, FluidStack fluidStack, PushWrapper<IFluidHandler> pushWrapper, int distance) {
        ResourceLocation fluidReg = ForgeRegistries.ITEMS.getKey(fluidStack.getFluid().getBucket());
        boolean hasFluid = fluidReg != null && hasFluid(fluidReg.toString());
        if ((!blacklist && !hasFluid) || (blacklist && hasFluid)) {
            return;
        }

        super.populatePushWrapper(pipeBlockEntity, side, fluidStack, pushWrapper, distance);
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
    public void openGUI(PipeBlockEntity pipeBlockEntity, Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player,new FluidFilterContainerMenu.Provider(this));
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
        BucketItem bucketItem;
        if (!(itemStack.getItem() instanceof BucketItem))
            return;
        bucketItem=(BucketItem) itemStack.getItem();
        if (slot<filters.length && ForgeRegistries.ITEMS.getKey(bucketItem)!=null &&
                !bucketItem.getFluid().equals(Fluids.EMPTY) &&
                !(bucketItem instanceof MobBucketItem)
        ) {
            String itemName = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
            if(hasFluid(itemName))
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
