package com.deadlyhunter.modkit.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class ModkitFacingBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<ModkitFacingBlock> CODEC = MapCodec.unit(
            () -> { throw new UnsupportedOperationException("Modkit blocks are built dynamically, not via codec"); });

    private final BlockDefinition definition;

    public ModkitFacingBlock(BlockDefinition def) {
        super(ModkitBlock.buildPropertiesFor(def));
        this.definition = def;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public BlockDefinition getDefinition() {
        return definition;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public MutableComponent getName() {
        return Component.literal(definition.displayName);
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, stack, dropExperience);
        if (dropExperience) {
            IntProvider xp = ModkitBlock.xpProvider(definition);
            if (xp != null) this.tryDropExperience(level, pos, stack, xp);
        }
    }
}
