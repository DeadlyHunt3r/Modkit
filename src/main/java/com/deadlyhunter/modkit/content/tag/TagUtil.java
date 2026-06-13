package com.deadlyhunter.modkit.content.tag;

import java.util.List;

public final class TagUtil {

    private TagUtil() {}

    public static String validateTagId(String tag) {
        if (tag == null || tag.isBlank()) return "empty tag id";
        if (!tag.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            return "invalid tag id: " + tag + " (use namespace:path, e.g. forge:gems)";
        }
        return null;
    }

    public static String validateTagList(List<String> tags) {
        if (tags == null) return null;
        if (tags.size() > 20) return "max 20 tags";
        for (String t : tags) {
            String err = validateTagId(t);
            if (err != null) return err;
        }
        return null;
    }

    public static final List<String> COMMON_ITEM_TAGS = List.of(
            "forge:ingots",
            "forge:gems",
            "forge:nuggets",
            "forge:dusts",
            "forge:ores",
            "forge:raw_materials",
            "forge:rods",
            "forge:gears",
            "forge:plates",
            "minecraft:planks",
            "minecraft:logs",
            "minecraft:wool",
            "minecraft:flowers",
            "minecraft:saplings",
            "minecraft:coals",
            "minecraft:swords",
            "minecraft:pickaxes",
            "minecraft:axes",
            "minecraft:shovels",
            "minecraft:hoes",
            "minecraft:music_discs",
            "forge:tools",
            "forge:armors",
            "minecraft:piglin_loved"
    );

    public static final List<String> COMMON_BLOCK_TAGS = List.of(
            "minecraft:mineable/pickaxe",
            "minecraft:mineable/axe",
            "minecraft:mineable/shovel",
            "minecraft:mineable/hoe",
            "minecraft:needs_stone_tool",
            "minecraft:needs_iron_tool",
            "minecraft:needs_diamond_tool",
            "forge:ores",
            "forge:storage_blocks",
            "minecraft:logs",
            "minecraft:planks",
            "minecraft:wool",
            "minecraft:walls",
            "minecraft:fences",
            "minecraft:slabs",
            "minecraft:stairs",
            "minecraft:beacon_base_blocks",
            "minecraft:dragon_immune",
            "minecraft:wither_immune",
            "minecraft:climbable"
    );
}
