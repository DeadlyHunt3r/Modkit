package com.deadlyhunter.modkit.content.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ModkitSword extends SwordItem {

    private final WeaponDefinition definition;

    public ModkitSword(WeaponDefinition def, String modId) {
        super(
                ModkitWeaponTier.resolve(modId, def),
                3 + Math.round(def.damageBonus),
                def.attackSpeed,
                buildProperties(def)
        );
        this.definition = def;
    }

    public WeaponDefinition getDefinition() {
        return definition;
    }

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

    private static Properties buildProperties(WeaponDefinition def) {
        Properties p = new Properties();
        p = p.rarity(parseRarity(def.rarity));
        return p;
    }

    private static Rarity parseRarity(String s) {
        if (s == null) return Rarity.COMMON;
        return switch (s) {
            case "uncommon" -> Rarity.UNCOMMON;
            case "rare"     -> Rarity.RARE;
            case "epic"     -> Rarity.EPIC;
            default         -> Rarity.COMMON;
        };
    }
}
