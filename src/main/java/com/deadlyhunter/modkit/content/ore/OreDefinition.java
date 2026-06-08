package com.deadlyhunter.modkit.content.ore;

import com.google.gson.annotations.SerializedName;

public class OreDefinition {

    @SerializedName("modkit_format_version")
    public int formatVersion = 1;

    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("block_id")
    public String blockId;

    @SerializedName("dimension")
    public String dimension = "overworld"; // "overworld" | "nether" | "end"

    @SerializedName("replaces")
    public String replaces = "stone"; // "stone" | "deepslate" | "both"

    @SerializedName("min_y")
    public int minY = -64;

    @SerializedName("max_y")
    public int maxY = 64;

    @SerializedName("veins_per_chunk")
    public int veinsPerChunk = 8;

    @SerializedName("vein_size")
    public int veinSize = 6;

    public String validate() {
        if (id == null || id.isBlank()) return "Missing 'id'";
        if (!id.matches("^[a-z0-9_]{1,40}$")) {
            return "Invalid 'id': only lowercase a-z, 0-9, underscore, max 40 chars";
        }
        if (displayName == null || displayName.isBlank()) displayName = id;
        if (blockId == null || blockId.isBlank()) return "Missing 'block_id' — which block should spawn?";
        if (!blockId.matches("^[a-z0-9_]{1,40}$")) {
            return "Invalid 'block_id'";
        }

        if (dimension == null) dimension = "overworld";
        switch (dimension) {
            case "overworld": case "nether": case "end": break;
            default: return "dimension must be: overworld, nether, or end";
        }

        if (replaces == null) replaces = "stone";
        switch (replaces) {
            case "stone": case "deepslate": case "both": break;
            default: return "replaces must be: stone, deepslate, or both";
        }

        if (minY < -64 || minY > 320) return "min_y must be between -64 and 320";
        if (maxY < -64 || maxY > 320) return "max_y must be between -64 and 320";
        if (minY > maxY) return "min_y cannot be greater than max_y";

        if (veinsPerChunk < 1 || veinsPerChunk > 64) return "veins_per_chunk must be 1-64";
        if (veinSize < 1 || veinSize > 64) return "vein_size must be 1-64";

        return null;
    }
}
