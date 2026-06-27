package com.deadlyhunter.modkit.content.armor;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ModkitArmorMaterial {

    private ModkitArmorMaterial() {}

    public static Holder<ArmorMaterial> create(String modId, ArmorSetDefinition def) {
        int[] defense = resolveDefense(def);

        Map<ArmorItem.Type, Integer> defenseMap = new EnumMap<>(ArmorItem.Type.class);
        defenseMap.put(ArmorItem.Type.HELMET, defense[0]);
        defenseMap.put(ArmorItem.Type.CHESTPLATE, defense[1]);
        defenseMap.put(ArmorItem.Type.LEGGINGS, defense[2]);
        defenseMap.put(ArmorItem.Type.BOOTS, defense[3]);
        if (hasBodyType()) {
            defenseMap.put(ArmorItem.Type.BODY, defense[1]);
        }

        float toughness;
        float knockback;
        int enchantValue;
        if ("custom".equals(def.tier)) {
            toughness = def.toughness;
            knockback = def.knockbackResistance;
            enchantValue = def.enchantmentValue;
        } else {
            toughness = switch (def.tier) {
                case "diamond"   -> 2.0f;
                case "netherite" -> 3.0f;
                default          -> 0.0f;
            };
            knockback = "netherite".equals(def.tier) ? 0.1f : 0.0f;
            enchantValue = vanillaEnchantValue(def.tier);
        }

        Holder<SoundEvent> equipSound = switch (def.tier) {
            case "leather"   -> SoundEvents.ARMOR_EQUIP_LEATHER;
            case "chainmail" -> SoundEvents.ARMOR_EQUIP_CHAIN;
            case "gold"      -> SoundEvents.ARMOR_EQUIP_GOLD;
            case "diamond"   -> SoundEvents.ARMOR_EQUIP_DIAMOND;
            case "netherite" -> SoundEvents.ARMOR_EQUIP_NETHERITE;
            default          -> SoundEvents.ARMOR_EQUIP_IRON;
        };

        Supplier<Ingredient> repair = () -> repairIngredient(modId, def);

        List<ArmorMaterial.Layer> layers = List.of(
                new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(modId, def.id))
        );

        ArmorMaterial material = new ArmorMaterial(defenseMap, enchantValue, equipSound, repair, layers, toughness, knockback);
        return Holder.direct(material);
    }

    public static int durabilityMultiplier(ArmorSetDefinition def) {
        if ("custom".equals(def.tier)) return def.durabilityMultiplier;
        return switch (def.tier) {
            case "leather"   -> 5;
            case "chainmail" -> 15;
            case "iron"      -> 15;
            case "gold"      -> 7;
            case "diamond"   -> 33;
            case "netherite" -> 37;
            default          -> 15;
        };
    }

    private static int[] resolveDefense(ArmorSetDefinition def) {
        if ("custom".equals(def.tier)) {
            return new int[]{def.defenseHelmet, def.defenseChestplate, def.defenseLeggings, def.defenseBoots};
        }
        int[] vanilla = ArmorSetDefinition.vanillaDefense(def.tier);
        return vanilla != null ? vanilla : new int[]{2, 6, 5, 2};
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

    private static boolean hasBodyType() {
        for (ArmorItem.Type t : ArmorItem.Type.values()) {
            if (t.name().equals("BODY")) return true;
        }
        return false;
    }

    private static Ingredient repairIngredient(String modId, ArmorSetDefinition def) {
        if (def.repairItem == null || def.repairItem.isBlank()) return Ingredient.EMPTY;

        String fullId = "mine".equals(def.repairSource) ? modId + ":" + def.repairItem : def.repairItem;
        ResourceLocation loc = ResourceLocation.tryParse(fullId);
        if (loc == null) return Ingredient.EMPTY;

        Item item = BuiltInRegistries.ITEM.get(loc);
        if (item == null) return Ingredient.EMPTY;

        return Ingredient.of(item);
    }
}
