package com.deadlyhunter.modkit.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Forge Block subclass that reads its properties from a BlockDefinition.
 *
 * XP drops are not part of loot tables — they live on the Block class via
 * getExpDrop(). We override it here to return a uniform random value between
 * the definition's xp_min and xp_max.
 */
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

    /**
     * Called by vanilla on block break to determine XP drop.
     * Note: Forge's signature here is the legacy non-deprecated overload
     * for 1.20.1 — we receive a RandomSource and the position.
     */
    @Override
    public int getExpDrop(BlockState state, LevelReader level, RandomSource random,
                          BlockPos pos, int fortuneLevel, int silkTouchLevel) {
        // No XP if Silk Touch was used — matches vanilla ore behavior
        if (silkTouchLevel > 0) return 0;
        if (definition.xpMax <= 0) return 0;

        int min = Math.max(0, definition.xpMin);
        int max = Math.max(min, definition.xpMax);
        if (min == max) return min;
        return min + random.nextInt(max - min + 1);
    }

    /** Shared properties builder — also used by ModkitFacingBlock. */
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
