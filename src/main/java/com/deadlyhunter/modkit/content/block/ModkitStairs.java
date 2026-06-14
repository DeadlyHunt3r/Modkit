package com.deadlyhunter.modkit.content.block;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;

public class ModkitStairs extends StairBlock {

    private final String name;

    public ModkitStairs(BlockDefinition base) {
        super(() -> Blocks.STONE.defaultBlockState(), ModkitBlock.buildPropertiesFor(base));
        this.name = base.displayName + " Stairs";
    }

    @Override
    public MutableComponent getName() {
        return Component.literal(name);
    }
}
