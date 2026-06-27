package com.deadlyhunter.modkit.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ModkitBlock extends Block {

    private final BlockDefinition definition;

    public ModkitBlock(BlockDefinition def) {
        super(buildProperties(def));
        this.definition = def;
    }

    public BlockDefinition getDefinition() {
        return definition;
    }

    @Override
    public MutableComponent getName() {
        return Component.literal(definition.displayName);
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, stack, dropExperience);
        if (dropExperience) {
            IntProvider xp = xpProvider(definition);
            if (xp != null) this.tryDropExperience(level, pos, stack, xp);
        }
    }

    static IntProvider xpProvider(BlockDefinition def) {
        if (def.xpMax <= 0) return null;
        int min = Math.max(0, def.xpMin);
        int max = Math.max(min, def.xpMax);
        return UniformInt.of(min, max);
    }

    static BlockBehaviour.Properties buildPropertiesFor(BlockDefinition def) {
        return buildProperties(def);
    }

    private static BlockBehaviour.Properties buildProperties(BlockDefinition def) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of();

        if (def.hardness < 0) {
            props.strength(-1.0f, def.resistance);
        } else {
            props.strength(def.hardness, def.resistance);
        }

        if (def.requiresCorrectTool && def.tool != null && !"any".equals(def.tool)) {
            props.requiresCorrectToolForDrops();
        }

        if (def.lightEmission > 0) {
            final int level = def.lightEmission;
            props.lightLevel(state -> level);
        }

        props.sound(parseSoundGroup(def.soundGroup));
        props.friction(def.friction);

        return props;
    }

    private static SoundType parseSoundGroup(String s) {
        if (s == null) return SoundType.STONE;
        return switch (s) {
            case "wood" -> SoundType.WOOD;
            case "gravel" -> SoundType.GRAVEL;
            case "grass" -> SoundType.GRASS;
            case "metal" -> SoundType.METAL;
            case "glass" -> SoundType.GLASS;
            case "wool" -> SoundType.WOOL;
            case "sand" -> SoundType.SAND;
            case "snow" -> SoundType.SNOW;
            default -> SoundType.STONE;
        };
    }
}
