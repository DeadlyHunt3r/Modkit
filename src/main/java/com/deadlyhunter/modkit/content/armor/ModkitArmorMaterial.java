package com.deadlyhunter.modkit.content.armor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

public class ModkitArmorMaterial implements ArmorMaterial {

    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};

    private final String modId;
    private final ArmorSetDefinition def;

    private final int[] defense;
    private final int durabilityMultiplier;
    private final int enchantmentValue;
    private final float toughness;
    private final float knockbackResistance;

    public ModkitArmorMaterial(String modId, ArmorSetDefinition def) {
        this.modId = modId;
        this.def = def;

        if ("custom".equals(def.tier)) {
            this.defense = new int[]{def.defenseHelmet, def.defenseChestplate, def.defenseLeggings, def.defenseBoots};
            this.durabilityMultiplier = def.durabilityMultiplier;
            this.enchantmentValue = def.enchantmentValue;
            this.toughness = def.toughness;
            this.knockbackResistance = def.knockbackResistance;
        } else {
            int[] vanillaDef = ArmorSetDefinition.vanillaDefense(def.tier);
            this.defense = vanillaDef != null ? vanillaDef : new int[]{2, 6, 5, 2};
            this.durabilityMultiplier = vanillaDurabilityMultiplier(def.tier);
            this.enchantmentValue = vanillaEnchantValue(def.tier);
            this.toughness = switch (def.tier) {
                case "diamond"   -> 2.0f;
                case "netherite" -> 3.0f;
                default          -> 0.0f;
            };
            this.knockbackResistance = "netherite".equals(def.tier) ? 0.1f : 0.0f;
        }
    }

    private static int vanillaDurabilityMultiplier(String tier) {
        return switch (tier) {
            case "leather"   -> 5;
            case "chainmail" -> 15;
            case "iron"      -> 15;
            case "gold"      -> 7;
            case "diamond"   -> 33;
            case "netherite" -> 37;
            default          -> 15;
        };
    }

    private static int vanillaEnchantValue(String tier) {
        return switch (tier) {
            case "leather"   -> 15;
            case "chainmail" -> 12;
            case "iron"      -> 9;
            case "gold"      -> 25;
            case "diamond"   -> 10;
            case "netherite" -> 15;
            default          -> 9;
        };
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        int base = switch (type) {
            case HELMET     -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS   -> 15;
            case BOOTS      -> 13;
        };
        return base * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET     -> defense[0];
            case CHESTPLATE -> defense[1];
            case LEGGINGS   -> defense[2];
            case BOOTS      -> defense[3];
        };
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return switch (def.tier) {
            case "leather"   -> SoundEvents.ARMOR_EQUIP_LEATHER;
            case "chainmail" -> SoundEvents.ARMOR_EQUIP_CHAIN;
            case "gold"      -> SoundEvents.ARMOR_EQUIP_GOLD;
            case "diamond"   -> SoundEvents.ARMOR_EQUIP_DIAMOND;
            case "netherite" -> SoundEvents.ARMOR_EQUIP_NETHERITE;
            default          -> SoundEvents.ARMOR_EQUIP_IRON;
        };
    }

    @Override
    public Ingredient getRepairIngredient() {
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

    @Override
    public String getName() {
        return modId + ":" + def.id;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
