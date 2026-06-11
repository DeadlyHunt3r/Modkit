package com.deadlyhunter.modkit.content.armor;

import com.google.gson.annotations.SerializedName;

public class ArmorSetDefinition {

    @SerializedName("modkit_format_version")
    public int formatVersion = 1;

    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("tier")
    public String tier = "iron";

    @SerializedName("has_helmet")
    public boolean hasHelmet = true;
    @SerializedName("has_chestplate")
    public boolean hasChestplate = true;
    @SerializedName("has_leggings")
    public boolean hasLeggings = true;
    @SerializedName("has_boots")
    public boolean hasBoots = true;

    @SerializedName("helmet_name")
    public String helmetName = "";
    @SerializedName("chestplate_name")
    public String chestplateName = "";
    @SerializedName("leggings_name")
    public String leggingsName = "";
    @SerializedName("boots_name")
    public String bootsName = "";

    @SerializedName("defense_helmet")
    public int defenseHelmet = 3;
    @SerializedName("defense_chestplate")
    public int defenseChestplate = 8;
    @SerializedName("defense_leggings")
    public int defenseLeggings = 6;
    @SerializedName("defense_boots")
    public int defenseBoots = 3;

    @SerializedName("toughness")
    public float toughness = 2.0f;

    @SerializedName("knockback_resistance")
    public float knockbackResistance = 0.0f;

    @SerializedName("durability_multiplier")
    public int durabilityMultiplier = 33;

    @SerializedName("enchantment_value")
    public int enchantmentValue = 10;

    @SerializedName("repair_source")
    public String repairSource = "other";

    @SerializedName("repair_item")
    public String repairItem = "";

    @SerializedName("rarity")
    public String rarity = "common";

    @SerializedName("glow")
    public boolean glow = false;

    @SerializedName("tooltip_lines")
    public java.util.List<String> tooltipLines = new java.util.ArrayList<>();

    public static final String[] PIECE_TYPES = {"helmet", "chestplate", "leggings", "boots"};

    public boolean hasPiece(String pieceType) {
        return switch (pieceType) {
            case "helmet"     -> hasHelmet;
            case "chestplate" -> hasChestplate;
            case "leggings"   -> hasLeggings;
            case "boots"      -> hasBoots;
            default           -> false;
        };
    }

    public String pieceItemId(String pieceType) {
        return id + "_" + pieceType;
    }

    public String pieceDisplayName(String pieceType) {
        String custom = switch (pieceType) {
            case "helmet"     -> helmetName;
            case "chestplate" -> chestplateName;
            case "leggings"   -> leggingsName;
            case "boots"      -> bootsName;
            default           -> "";
        };
        if (custom != null && !custom.isBlank()) return custom;
        String suffix = Character.toUpperCase(pieceType.charAt(0)) + pieceType.substring(1);
        return (displayName != null && !displayName.isBlank() ? displayName : id) + " " + suffix;
    }

    public static int[] vanillaDefense(String tier) {
        return switch (tier) {
            case "leather"   -> new int[]{1, 3, 2, 1};
            case "chainmail" -> new int[]{2, 5, 4, 1};
            case "iron"      -> new int[]{2, 6, 5, 2};
            case "gold"      -> new int[]{2, 5, 3, 1};
            case "diamond"   -> new int[]{3, 8, 6, 3};
            case "netherite" -> new int[]{3, 8, 6, 3};
            default          -> null;
        };
    }

    public String validate() {
        if (id == null || id.isBlank()) return "Missing 'id'";
        if (!id.matches("^[a-z0-9_]{1,32}$")) {
            return "Invalid 'id': only lowercase a-z, 0-9, underscore, max 32 chars";
        }
        if (displayName == null || displayName.isBlank()) displayName = id;

        if (tier == null) tier = "iron";
        switch (tier) {
            case "leather": case "chainmail": case "iron": case "gold":
            case "diamond": case "netherite": case "custom": break;
            default: return "tier must be: leather, chainmail, iron, gold, diamond, netherite, or custom";
        }

        if (!hasHelmet && !hasChestplate && !hasLeggings && !hasBoots) {
            return "At least one piece must be enabled";
        }

        if ("custom".equals(tier)) {
            if (defenseHelmet < 0 || defenseHelmet > 20) return "defense_helmet must be 0-20";
            if (defenseChestplate < 0 || defenseChestplate > 20) return "defense_chestplate must be 0-20";
            if (defenseLeggings < 0 || defenseLeggings > 20) return "defense_leggings must be 0-20";
            if (defenseBoots < 0 || defenseBoots > 20) return "defense_boots must be 0-20";
            if (toughness < 0 || toughness > 10) return "toughness must be 0-10";
            if (knockbackResistance < 0 || knockbackResistance > 1) return "knockback_resistance must be 0-1";
            if (durabilityMultiplier < 1 || durabilityMultiplier > 200) return "durability_multiplier must be 1-200";
            if (enchantmentValue < 0 || enchantmentValue > 50) return "enchantment_value must be 0-50";
        }

        if (repairSource == null) repairSource = "other";
        if (!"mine".equals(repairSource) && !"other".equals(repairSource)) {
            return "repair_source must be 'mine' or 'other'";
        }
        if (repairItem != null && !repairItem.isBlank()) {
            if ("mine".equals(repairSource)) {
                if (repairItem.contains(":")) return "repair_item for 'mine' must be just the id";
                if (!repairItem.matches("[a-z0-9_]{1,40}")) return "invalid repair_item id";
            } else {
                if (!repairItem.contains(":")) return "repair_item for 'other' must include namespace";
                if (!repairItem.matches("[a-z0-9_]+:[a-z0-9_/]+")) return "invalid repair_item id format";
            }
        }

        if (rarity == null) rarity = "common";
        switch (rarity) {
            case "common": case "uncommon": case "rare": case "epic": break;
            default: return "rarity must be: common, uncommon, rare, or epic";
        }

        if (tooltipLines == null) tooltipLines = new java.util.ArrayList<>();
        if (tooltipLines.size() > 8) return "max 8 tooltip lines";
        for (String line : tooltipLines) {
            if (line != null && line.length() > 80) return "tooltip line too long (max 80 chars)";
        }

        return null;
    }
}
