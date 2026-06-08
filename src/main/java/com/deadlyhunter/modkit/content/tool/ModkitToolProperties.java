package com.deadlyhunter.modkit.content.tool;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

final class ModkitToolProperties {

    private ModkitToolProperties() {}

    static Item.Properties build(ToolDefinition def) {
        return new Item.Properties().rarity(parseRarity(def.rarity));
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
