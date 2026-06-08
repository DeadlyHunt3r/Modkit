package com.deadlyhunter.modkit.content.weapon;

import com.google.gson.annotations.SerializedName;


public class WeaponDefinition {

    @SerializedName("modkit_format_version")
    public int formatVersion = 1;

    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("weapon_type")
    public String weaponType = "sword";

    @SerializedName("tier")
    public String tier = "iron";

    @SerializedName("damage_bonus")
    public float damageBonus = 0f;

    @SerializedName("attack_speed")
    public float attackSpeed = -2.4f;

    @SerializedName("mining_level")
    public int miningLevel = 2;

    @SerializedName("durability")
    public int durability = 250;

    @SerializedName("damage_base")
    public float damageBase = 2f;

    @SerializedName("mining_speed")
    public float miningSpeed = 6f;

    @SerializedName("enchantment_value")
    public int enchantmentValue = 14;

    @SerializedName("repair_source")
    public String repairSource = "other";

    @SerializedName("repair_item")
    public String repairItem = "";

    @SerializedName("tooltip_lines")
    public java.util.List<String> tooltipLines = new java.util.ArrayList<>();

    @SerializedName("rarity")
    public String rarity = "common";

    @SerializedName("glow")
    public boolean glow = false;

    public String validate() {
        if (id == null || id.isBlank()) return "Missing 'id'";
        if (!id.matches("^[a-z0-9_]{1,40}$")) {
            return "Invalid 'id': only lowercase a-z, 0-9, underscore, max 40 chars";
        }
        if (displayName == null || displayName.isBlank()) displayName = id;

        if (weaponType == null) weaponType = "sword";
        if (!weaponType.equals("sword")) {
            return "weapon_type currently must be 'sword' (more types coming)";
        }

        if (tier == null) tier = "iron";
        switch (tier) {
            case "wood": case "stone": case "iron": case "diamond": case "netherite": case "custom": break;
            default: return "tier must be: wood, stone, iron, diamond, netherite, or custom";
        }

        if (attackSpeed < -3.9f || attackSpeed > 4.0f) {
            return "attack_speed must be between -3.9 and 4.0";
        }

        if (damageBonus < -10f || damageBonus > 100f) {
            return "damage_bonus must be between -10 and 100";
        }

        if ("custom".equals(tier)) {

            if ("sword".equals(weaponType)) {
                miningLevel = 0;
                miningSpeed = 1.5f;
            }
            if (miningLevel < 0 || miningLevel > 10) return "mining_level must be 0-10";
            if (durability < 1 || durability > 100000) return "durability must be 1-100000";
            if (damageBase < 0 || damageBase > 100) return "damage_base must be 0-100";
            if (miningSpeed < 0 || miningSpeed > 100) return "mining_speed must be 0-100";
            if (enchantmentValue < 0 || enchantmentValue > 50) return "enchantment_value must be 0-50";
        }

        if (repairSource == null) repairSource = "other";
        if (!"mine".equals(repairSource) && !"other".equals(repairSource)) {
            return "repair_source must be 'mine' or 'other'";
        }
        if (repairItem != null && !repairItem.isBlank()) {
            if ("mine".equals(repairSource)) {
                if (repairItem.contains(":")) return "repair_item for 'mine' must be just the id (no namespace)";
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
