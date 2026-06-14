package com.deadlyhunter.modkit.content.block;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.WallBlock;

public class ModkitWall extends WallBlock {

    private final String name;

    public ModkitWall(BlockDefinition base) {
        super(ModkitBlock.buildPropertiesFor(base));
        this.name = base.displayName + " Wall";
    }

    @Override
    public MutableComponent getName() {
        return Component.literal(name);
    }
}
