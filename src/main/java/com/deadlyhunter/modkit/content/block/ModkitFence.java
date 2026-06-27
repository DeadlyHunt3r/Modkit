package com.deadlyhunter.modkit.content.block;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.FenceBlock;

public class ModkitFence extends FenceBlock {

    private final String name;

    public ModkitFence(BlockDefinition base) {
        super(ModkitBlock.buildPropertiesFor(base));
        this.name = base.displayName + " Fence";
    }

    @Override
    public MutableComponent getName() {
        return Component.literal(name);
    }
}
