package com.deadlyhunter.modkit.content.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public class ModkitPickaxe extends PickaxeItem {

    private final ToolDefinition definition;

    public ModkitPickaxe(ToolDefinition def, String modId) {
        super(
                ModkitToolTier.resolve(modId, def),
                1 + Math.round(def.damageBonus),
                def.attackSpeed,
                ModkitToolProperties.build(def)
        );
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
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (definition.tooltipLines != null) {
            for (String line : definition.tooltipLines) {
                if (line != null && !line.isBlank()) {
                    tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}
