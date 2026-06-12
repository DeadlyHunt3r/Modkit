package com.deadlyhunter.modkit.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Variant of ModkitBlock with a horizontal FACING property — used for the
 * texture modes "front_other" and "front_top_bottom" where the front face
 * should point at the player on placement (furnace-style).
 *
 * Behavior (XP drops, name, properties) mirrors ModkitBlock; the properties
 * builder is reused from there to avoid duplication.
 */
public class ModkitFacingBlock extends HorizontalDirectionalBlock {

    private final BlockDefinition definition;

    public ModkitFacingBlock(BlockDefinition def) {
        super(ModkitBlock.buildPropertiesFor(def));
        this.definition = def;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public BlockDefinition getDefinition() {
        return definition;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Front faces the player — same as vanilla furnace
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public MutableComponent getName() {
        return Component.literal(definition.displayName);
    }

    @Override
    public int getExpDrop(BlockState state, LevelReader level, RandomSource random,
                          BlockPos pos, int fortuneLevel, int silkTouchLevel) {
        if (silkTouchLevel > 0) return 0;
        if (definition.xpMax <= 0) return 0;

        int min = Math.max(0, definition.xpMin);
        int max = Math.max(min, definition.xpMax);
        if (min == max) return min;
        return min + random.nextInt(max - min + 1);
    }
}
