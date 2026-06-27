package com.deadlyhunter.modkit.content.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ModkitArmorItem extends ArmorItem {

    private final ArmorSetDefinition setDef;
    private final String pieceType;

    public ModkitArmorItem(ArmorSetDefinition setDef, Holder<ArmorMaterial> material, int durabilityMultiplier, String pieceType) {
        super(material, typeFor(pieceType), buildProperties(setDef, typeFor(pieceType), durabilityMultiplier));
        this.setDef = setDef;
        this.pieceType = pieceType;
    }

    public ArmorSetDefinition getSetDefinition() { return setDef; }
    public String getPieceType() { return pieceType; }

    private static Type typeFor(String pieceType) {
        return switch (pieceType) {
            case "chestplate" -> Type.CHESTPLATE;
            case "leggings"   -> Type.LEGGINGS;
            case "boots"      -> Type.BOOTS;
            default           -> Type.HELMET;
        };
    }

    private static Properties buildProperties(ArmorSetDefinition def, Type type, int durabilityMultiplier) {
        return new Properties()
                .rarity(parseRarity(def.rarity))
                .durability(type.getDurability(durabilityMultiplier));
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

    @Override
    public MutableComponent getName(ItemStack stack) {
        return Component.literal(setDef.pieceDisplayName(pieceType));
    }

    @Override
    public Component getDescription() {
        return Component.literal(setDef.pieceDisplayName(pieceType));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return setDef.glow || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (setDef.tooltipLines != null) {
            for (String line : setDef.tooltipLines) {
                if (line != null && !line.isBlank()) {
                    tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}
