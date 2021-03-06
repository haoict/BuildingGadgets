package com.direwolf20.buildinggadgets.common.network.packets;

import com.direwolf20.buildinggadgets.client.events.EventTooltip;
import com.direwolf20.buildinggadgets.client.renders.BaseRenderer;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryHelper;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryLinker;
import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.objects.UniqueItem;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class PacketSetRemoteInventoryCache {

    private final boolean isCopyPaste;
    private Multiset<UniqueItem> cache;
    private Pair<BlockPos, RegistryKey<World>> loc;

    public PacketSetRemoteInventoryCache(Multiset<UniqueItem> cache, boolean isCopyPaste) {
        this.cache = cache;
        this.isCopyPaste = isCopyPaste;
    }

    public PacketSetRemoteInventoryCache(Pair<BlockPos, RegistryKey<World>> loc, boolean isCopyPaste) {
        this.loc = loc;
        this.isCopyPaste = isCopyPaste;
    }

    public static PacketSetRemoteInventoryCache decode(PacketBuffer buf) {
        boolean isCopyPaste = buf.readBoolean();
        if (buf.readBoolean()) {
            Pair<BlockPos, RegistryKey<World>> loc = new ImmutablePair<>(
                    buf.readBlockPos(),
                    RegistryKey.getOrCreateKey(Registry.WORLD_KEY, buf.readResourceLocation())
            );
            return new PacketSetRemoteInventoryCache(loc, isCopyPaste);
        }
        int len = buf.readInt();
        ImmutableMultiset.Builder<UniqueItem> builder = ImmutableMultiset.builder();
        for (int i = 0; i < len; i++)
            builder.addCopies(new UniqueItem(Item.getItemById(buf.readInt())), buf.readInt());

        Multiset<UniqueItem> cache = builder.build();
        return new PacketSetRemoteInventoryCache(cache, isCopyPaste);
    }

    public static void encode(PacketSetRemoteInventoryCache msg, PacketBuffer buf) {
        buf.writeBoolean(msg.isCopyPaste());
        boolean isRequest = msg.getCache() == null;
        buf.writeBoolean(isRequest);
        if (isRequest) {
            buf.writeLong(msg.getLoc().getLeft().toLong());
            buf.writeResourceLocation(msg.getLoc().getRight().getLocation());
            return;
        }
        Set<Entry<UniqueItem>> items = msg.getCache().entrySet();
        buf.writeInt(items.size());
        for (Entry<UniqueItem> entry : items) {
            UniqueItem uniqueItem = entry.getElement();
            buf.writeInt(Item.getIdFromItem(uniqueItem.createStack().getItem()));
            buf.writeInt(entry.getCount());
        }
    }

    public boolean isCopyPaste() {
        return isCopyPaste;
    }

    public Multiset<UniqueItem> getCache() {
        return cache;
    }

    public Pair<BlockPos, RegistryKey<World>> getLoc() {
        return loc;
    }

    public static class Handler {
        public static void handle(final PacketSetRemoteInventoryCache msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayerEntity player = ctx.get().getSender();
                if (player != null) {
                    Set<UniqueItem> itemTypes = new HashSet<>();
                    ImmutableMultiset.Builder<UniqueItem> builder = ImmutableMultiset.builder();
                    InventoryLinker.getLinkedInventory(player.world, msg.loc.getKey(), msg.loc.getValue(), null).ifPresent(inventory -> {
                        for (int i = 0; i < inventory.getSlots(); i++) {
                            ItemStack stack = inventory.getStackInSlot(i);
                            if (!stack.isEmpty()) {
                                Item item = stack.getItem();
                                UniqueItem uniqueItem = new UniqueItem(item);
                                if (!itemTypes.contains(uniqueItem)) {
                                    itemTypes.add(uniqueItem);
                                    builder.addCopies(uniqueItem, InventoryHelper.countInContainer(inventory, item));
                                }
                            }
                        }
                    });

                    PacketHandler.sendTo(new PacketSetRemoteInventoryCache(builder.build(), msg.isCopyPaste()), player);
                    return;
                }
                if (msg.isCopyPaste())
                    EventTooltip.setCache(msg.getCache());
                else
                    BaseRenderer.setInventoryCache(msg.getCache());
            });

            ctx.get().setPacketHandled(true);
        }
    }
}