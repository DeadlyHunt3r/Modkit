package com.deadlyhunter.modkit.content.block;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.SlabBlock;

public class ModkitSlab extends SlabBlock {

    private final String displayName;

    public ModkitSlab(BlockDefinition base) {
        super(ModkitBlock.buildPropertiesFor(base));
        this.displayName = base.displayName + " Slab";
    }

    @Override
    public MutableComponent getName() {
        return Component.literal(displayName);
    }
}
