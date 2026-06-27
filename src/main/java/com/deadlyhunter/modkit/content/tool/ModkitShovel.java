package com.deadlyhunter.modkit.content.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ModkitShovel extends ShovelItem {

    private final ToolDefinition definition;

    public ModkitShovel(ToolDefinition def, String modId) {
        this(def, ModkitToolTier.resolve(modId, def));
    }

    private ModkitShovel(ToolDefinition def, Tier tier) {
        super(tier, ModkitToolProperties.build(def)
                .attributes(ShovelItem.createAttributes(tier, 1.5f + def.damageBonus, def.attackSpeed)));
        this.definition = def;
    }

    public ToolDefinition getDefinition() { return definition; }

    @Override
    public MutableComponent getName(ItemStack stack) {
        return Component.literal(definition.displayName);
    }

    @Override
    public Component getDescription() {
        return Component.literal(definition.displayName);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return definition.glow || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (definition.tooltipLines != null) {
            for (String line : definition.tooltipLines) {
                if (line != null && !line.isBlank()) {
                    tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}
