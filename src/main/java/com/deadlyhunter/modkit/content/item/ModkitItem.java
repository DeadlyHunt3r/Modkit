package com.deadlyhunter.modkit.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModkitItem extends Item {

    private final ItemDefinition definition;

    public ModkitItem(ItemDefinition def) {
        super(buildProperties(def));
        this.definition = def;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    private static Properties buildProperties(ItemDefinition def) {
        Properties props = new Properties()
                .stacksTo(def.maxStackSize)
                .rarity(parseRarity(def.rarity));

        if (def.food != null) {
            net.minecraft.world.food.FoodProperties.Builder fb =
                    new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(def.food.nutrition)
                            .saturationMod(def.food.saturation);
            if (def.food.canAlwaysEat) fb.alwaysEat();
            if (def.food.fastEat) fb.fast();
            props.food(fb.build());
        }
        return props;
    }

    private static Rarity parseRarity(String s) {
        if (s == null) return Rarity.COMMON;
        return switch (s.toLowerCase()) {
            case "uncommon" -> Rarity.UNCOMMON;
            case "rare" -> Rarity.RARE;
            case "epic" -> Rarity.EPIC;
            default -> Rarity.COMMON;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (definition.tooltipLines != null) {
            for (String line : definition.tooltipLines) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return definition.glow || super.isFoil(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.displayName);
    }

    @Override
    public Component getDescription() {
        return Component.literal(definition.displayName);
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable net.minecraft.world.item.crafting.RecipeType<?> recipeType) {
        return definition.fuelBurnTime > 0 ? definition.fuelBurnTime : super.getBurnTime(itemStack, recipeType);
    }
}
