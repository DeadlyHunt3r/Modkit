package com.deadlyhunter.modkit.content.tool;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

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
        TagKey<Block> incorrect = incorrectForLevel(def.miningLevel);

        return new Tier() {
            @Override public int getUses() { return def.durability; }
            @Override public float getSpeed() { return def.miningSpeed; }
            @Override public float getAttackDamageBonus() { return def.damageBase; }
            @Override public TagKey<Block> getIncorrectBlocksForDrops() { return incorrect; }
            @Override public int getEnchantmentValue() { return def.enchantmentValue; }
            @Override public Ingredient getRepairIngredient() { return repair; }
        };
    }

    static TagKey<Block> incorrectForLevel(int level) {
        return switch (level) {
            case 0  -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            case 1  -> BlockTags.INCORRECT_FOR_STONE_TOOL;
            case 2  -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 3  -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
    }

    private static Ingredient buildRepairIngredient(String modId, ToolDefinition def) {
        if (def.repairItem == null || def.repairItem.isBlank()) return Ingredient.EMPTY;

        String fullId = "mine".equals(def.repairSource) ? modId + ":" + def.repairItem : def.repairItem;
        ResourceLocation loc = ResourceLocation.tryParse(fullId);
        if (loc == null) return Ingredient.EMPTY;

        Item item = BuiltInRegistries.ITEM.get(loc);
        if (item == null) return Ingredient.EMPTY;

        return Ingredient.of(item);
    }
}
