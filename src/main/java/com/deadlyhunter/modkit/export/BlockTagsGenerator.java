package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.content.block.BlockDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockTagsGenerator {

    private BlockTagsGenerator() {}


    public static Map<String, String> generate(String modId, List<BlockDefinition> blocks) {

        Map<String, List<String>> entries = new LinkedHashMap<>();

        for (BlockDefinition def : blocks) {
            String resourceId = modId + ":" + def.id;

            if (def.isVariant()) {
                switch (def.variantType) {
                    case "fence" -> add(entries, "minecraft:fences", resourceId);
                    case "wall"  -> add(entries, "minecraft:walls", resourceId);
                }
            }


            String tool = def.tool != null ? def.tool : "any";
            switch (tool) {
                case "pickaxe" -> add(entries, "minecraft:mineable/pickaxe", resourceId);
                case "axe"     -> add(entries, "minecraft:mineable/axe",     resourceId);
                case "shovel"  -> add(entries, "minecraft:mineable/shovel",  resourceId);
                case "hoe"     -> add(entries, "minecraft:mineable/hoe",     resourceId);
            }


            if (def.requiresCorrectTool && !"any".equals(tool)) {
                String tier = def.toolTier != null ? def.toolTier : "wood";
                switch (tier) {
                    case "stone"     -> add(entries, "minecraft:needs_stone_tool",   resourceId);
                    case "iron"      -> add(entries, "minecraft:needs_iron_tool",    resourceId);
                    case "diamond"   -> add(entries, "minecraft:needs_diamond_tool", resourceId);
                    case "netherite" -> add(entries, "minecraft:needs_diamond_tool", resourceId);

                }
            }
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : entries.entrySet()) {
            String tagPath = e.getKey();
            String filePath = tagPathToFilePath(tagPath);
            result.put(filePath, buildTagJson(e.getValue()));
        }
        return result;
    }

    private static void add(Map<String, List<String>> map, String key, String value) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }


    private static String tagPathToFilePath(String tagPath) {
        int colon = tagPath.indexOf(':');
        String namespace = tagPath.substring(0, colon);
        String path = tagPath.substring(colon + 1);
        return "data/" + namespace + "/tags/blocks/" + path + ".json";
    }

    private static String buildTagJson(List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"replace\": false,\n");
        sb.append("  \"values\": [\n");
        for (int i = 0; i < values.size(); i++) {
            sb.append("    \"").append(values.get(i)).append("\"");
            if (i < values.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }
}
