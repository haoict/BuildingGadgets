package com.direwolf20.buildinggadgets.common.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public abstract class Gadget extends Item {
    public Gadget() {
        super(new Properties().group(ItemGroup.TOOLS));
    }
}