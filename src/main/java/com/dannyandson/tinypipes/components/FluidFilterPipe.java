package com.dannyandson.tinypipes.components;

import com.dannyandson.tinypipes.TinyPipes;
import com.dannyandson.tinypipes.caphandlers.PushWrapper;
import com.dannyandson.tinypipes.gui.FluidFilterContainerMenu;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelCellSegment;
import com.dannyandson.tinyredstone.blocks.Side;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
import net.minecraftforge.fmllegacy.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Arrays;

public class FluidFilterPipe extends FluidPipe implements IFilterPipe {

    boolean changed = false;

    //saved fields
    private static final int filterSlots = 18;
    private String[] filters = new String[filterSlots];
    boolean blacklist = false;

    public static final ResourceLocation FLUID_FILTER_PIPE_TEXTURE = new ResourceLocation(TinyPipes.MODID, "block/fluid_filter_pipe");
    private static TextureAtlasSprite sprite = null;

    @Override
    protected TextureAtlasSprite getSprite() {
        if (sprite == null)
            sprite = com.dannyandson.tinyredstone.blocks.RenderHelper.getSprite(FLUID_FILTER_PIPE_TEXTURE);
        return sprite;
    }

    @Override
    public boolean onPlace(PanelCellPos cellPos, Player player) {
        ItemStack stack = player.getItemInHand(player.getUsedItemHand());
        if (stack == ItemStack.EMPTY)
            stack = player.getMainHandItem();
        if (stack.hasTag()) {
            CompoundTag itemNBT = stack.getTag();
            String filterString = itemNBT.getString("filters");
            filters = Arrays.copyOf(filterString.split("\n",filterSlots),filterSlots);
            blacklist = itemNBT.getBoolean("blacklist");
        }

        return super.onPlace(cellPos, player);
    }

    @Override
    protected void populatePushWrapper(PanelCellPos cellPos, @Nullable Side side, FluidStack fluidStack, PushWrapper<IFluidHandler> pushWrapper, int distance) {
        ResourceLocation fluidReg = fluidStack.getFluid().getBucket().getRegistryName();
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
    public boolean hasActivation(Player player) {
        return true;
    }

    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, Player player) {
        if (player.getMainHandItem().getItem() == Registration.REDSTONE_WRENCH.get())
            return super.onBlockActivated(cellPos, segmentClicked, player);

        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player,new FluidFilterContainerMenu.Provider(this));
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
        if (slot<filters.length && bucketItem.getRegistryName()!=null &&
                !bucketItem.getFluid().equals(Fluids.EMPTY) &&
                !(bucketItem instanceof MobBucketItem)
        ) {
            String itemName = itemStack.getItem().getRegistryName().toString();
            if(hasFluid(itemName))
                return;
            filters[slot] = itemStack.getItem().getRegistryName().toString();
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
