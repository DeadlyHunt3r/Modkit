package com.deadlyhunter.modkit.content.block;

import com.google.gson.annotations.SerializedName;

public class BlockDefinition {

    @SerializedName("modkit_format_version")
    public int formatVersion = 1;

    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("hardness")
    public float hardness = 1.5f;

    @SerializedName("resistance")
    public float resistance = 6.0f;

    @SerializedName("tool")
    public String tool = "any";

    @SerializedName("tool_tier")
    public String toolTier = "wood";

    @SerializedName("requires_correct_tool")
    public boolean requiresCorrectTool = false;

    @SerializedName("light_emission")
    public int lightEmission = 0;

    @SerializedName("sound_group")
    public String soundGroup = "stone";

    @SerializedName("friction")
    public float friction = 0.6f;


    @SerializedName("texture_mode")
    public String textureMode = "all";


    public static String[] textureSuffixes(String mode) {
        return switch (mode == null ? "all" : mode) {
            case "front_other"      -> new String[]{"", "front"};
            case "front_top_bottom" -> new String[]{"", "front", "top", "bottom"};
            case "all_unique"       -> new String[]{"north", "south", "east", "west", "up", "down"};
            default                 -> new String[]{""};
        };
    }


    public boolean usesFacing() {
        return "front_other".equals(textureMode) || "front_top_bottom".equals(textureMode);
    }


    @SerializedName("drop_self")
    public boolean dropSelf = true;


    @SerializedName("drop_mode")
    public String dropMode = "self";


    @SerializedName("drop_item")
    public String dropItem = "";


    @SerializedName("drop_min")
    public int dropMin = 1;


    @SerializedName("drop_max")
    public int dropMax = 1;


    @SerializedName("drop_fortune")
    public boolean dropFortune = false;


    @SerializedName("xp_min")
    public int xpMin = 0;


    @SerializedName("xp_max")
    public int xpMax = 0;

    @SerializedName("tags")
    public java.util.List<String> tags = new java.util.ArrayList<>();

    public String validate() {
        if (id == null || id.isBlank()) return "Missing 'id'";
        if (!id.matches("^[a-z0-9_]{1,40}$")) {
            return "Invalid 'id': only lowercase a-z, 0-9, underscore, max 40 chars";
        }
        if (displayName == null || displayName.isBlank()) displayName = id;
        if (hardness < -1f) return "hardness cannot be less than -1 (-1 = unbreakable)";
        if (resistance < 0f) return "resistance cannot be negative";
        if (lightEmission < 0 || lightEmission > 15) return "light_emission must be 0-15";
        if (friction < 0.1f || friction > 1.0f) return "friction must be between 0.1 and 1.0";

        if (textureMode == null) textureMode = "all";
        switch (textureMode) {
            case "all": case "front_other": case "front_top_bottom": case "all_unique": break;
            default: return "texture_mode must be: all, front_other, front_top_bottom, or all_unique";
        }

        if (tool == null) tool = "any";
        switch (tool) {
            case "any": case "pickaxe": case "axe": case "shovel": case "hoe": break;
            default: return "tool must be: any, pickaxe, axe, shovel, or hoe";
        }

        if (toolTier == null) toolTier = "wood";
        if ("netherite".equals(toolTier)) toolTier = "diamond";
        switch (toolTier) {
            case "wood": case "stone": case "iron": case "diamond": break;
            default: return "tool_tier must be: wood, stone, iron, or diamond";
        }

        if (soundGroup == null) soundGroup = "stone";
        switch (soundGroup) {
            case "stone": case "wood": case "gravel": case "grass": case "metal":
            case "glass": case "wool": case "sand": case "snow": break;
            default: return "Unknown sound_group: " + soundGroup;
        }


        if (dropMode == null || dropMode.isBlank()) {
            dropMode = dropSelf ? "self" : "nothing";
        }

        if ("item".equals(dropMode)) {
            if (dropItem != null && dropItem.contains(":")) {
                dropMode = "item_other";
            } else {
                dropMode = "item_mine";
            }
        }
        switch (dropMode) {
            case "self":
            case "nothing":
                break;
            case "item_mine":
                if (dropItem == null || dropItem.isBlank()) {
                    return "drop_mode = item_mine requires 'drop_item' (your item's id, e.g. fire_essence)";
                }
                if (dropItem.contains(":")) {
                    return "drop_item for item_mine must be just the id (no namespace) — use item_other for cross-mod refs";
                }
                if (!dropItem.matches("[a-z0-9_]{1,40}")) {
                    return "Invalid drop_item id";
                }
                if (dropMin < 0) return "drop_min cannot be negative";
                if (dropMax < dropMin) return "drop_max cannot be less than drop_min";
                if (dropMax > 64) return "drop_max cannot exceed 64";
                break;
            case "item_other":
                if (dropItem == null || dropItem.isBlank()) {
                    return "drop_mode = item_other requires 'drop_item' (full id, e.g. minecraft:diamond)";
                }
                if (!dropItem.contains(":")) {
                    return "drop_item for item_other must include namespace (e.g. minecraft:diamond)";
                }
                if (!dropItem.matches("[a-z0-9_]+:[a-z0-9_/]+")) {
                    return "Invalid drop_item id format";
                }
                if (dropMin < 0) return "drop_min cannot be negative";
                if (dropMax < dropMin) return "drop_max cannot be less than drop_min";
                if (dropMax > 64) return "drop_max cannot exceed 64";
                break;
            default:
                return "drop_mode must be: self, nothing, item_mine, or item_other";
        }

        if (xpMin < 0) return "xp_min cannot be negative";
        if (xpMax < xpMin) return "xp_max cannot be less than xp_min";
        if (xpMax > 100) return "xp_max cannot exceed 100";


        dropSelf = "self".equals(dropMode);

        if (tags == null) tags = new java.util.ArrayList<>();
        String tagErr = com.deadlyhunter.modkit.content.tag.TagUtil.validateTagList(tags);
        if (tagErr != null) return tagErr;

        return null;
    }
}
