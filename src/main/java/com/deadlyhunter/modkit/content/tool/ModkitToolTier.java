package com.deadlyhunter.modkit.content.tool;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;

public final class ModkitToolTier {

    private ModkitToolTier() {}

    public static Tier resolve(String modId, ToolDefinition def) {
        return switch (def.tier) {
            case "wood"      -> Tiers.WOOD;
            case "stone"     -> Tiers.STONE;
            case "iron"      -> Tiers.IRON;
            case "diamond"   -> Tiers.DIAMOND;
            case "netherite" -> Tiers.NETHERITE;
            default          -> buildCustom(modId, def);
        };
    }

    private static Tier buildCustom(String modId, ToolDefinition def) {
        Ingredient repair = buildRepairIngredient(modId, def);
        return new Tier() {
            @Override public int getUses() { return def.durability; }
            @Override public float getSpeed() { return def.miningSpeed; }
            @Override public float getAttackDamageBonus() { return def.damageBase; }
            @Override public int getLevel() { return def.miningLevel; }
            @Override public int getEnchantmentValue() { return def.enchantmentValue; }
            @Override public Ingredient getRepairIngredient() { return repair; }
        };
    }

    private static Ingredient buildRepairIngredient(String modId, ToolDefinition def) {
        if (def.repairItem == null || def.repairItem.isBlank()) return Ingredient.EMPTY;

        String fullId = "mine".equals(def.repairSource)
                ? modId + ":" + def.repairItem
                : def.repairItem;

        ResourceLocation loc = ResourceLocation.tryParse(fullId);
        if (loc == null) return Ingredient.EMPTY;

        Item item = BuiltInRegistries.ITEM.get(loc);
        if (item == null) return Ingredient.EMPTY;

        return Ingredient.of(item);
    }
}
