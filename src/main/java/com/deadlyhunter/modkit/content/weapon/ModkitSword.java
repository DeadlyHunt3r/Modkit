package com.deadlyhunter.modkit.content.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ModkitSword extends SwordItem {

    private final WeaponDefinition definition;

    public ModkitSword(WeaponDefinition def, String modId) {
        this(def, ModkitWeaponTier.resolve(modId, def));
    }

    private ModkitSword(WeaponDefinition def, Tier tier) {
        super(tier, new Item.Properties()
                .rarity(parseRarity(def.rarity))
                .attributes(SwordItem.createAttributes(tier, 3 + Math.round(def.damageBonus), def.attackSpeed)));
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
