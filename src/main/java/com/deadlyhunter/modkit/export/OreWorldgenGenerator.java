package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.content.ore.OreDefinition;

public final class OreWorldgenGenerator {

    private OreWorldgenGenerator() {}

    public static String getConfiguredFeaturePath(String modId, String oreId) {
        return "data/" + modId + "/worldgen/configured_feature/" + oreId + ".json";
    }

    public static String getPlacedFeaturePath(String modId, String oreId) {
        return "data/" + modId + "/worldgen/placed_feature/" + oreId + ".json";
    }

    public static String getBiomeModifierPath(String modId, String oreId) {
        return "data/" + modId + "/neoforge/biome_modifier/" + oreId + ".json";
    }

    public static String generateConfiguredFeature(String modId, OreDefinition def) {
        String blockRef = modId + ":" + def.blockId;
        String targets = buildTargets(blockRef, def.replaces);

        return """
                {
                  "type": "minecraft:ore",
                  "config": {
                    "size": %d,
                    "discard_chance_on_air_exposure": 0.0,
                    "targets": [
                %s
                    ]
                  }
                }
                """.formatted(def.veinSize, targets);
    }

    private static String buildTargets(String blockRef, String replaces) {

        StringBuilder sb = new StringBuilder();

        boolean stone = replaces.equals("stone") || replaces.equals("both");
        boolean deepslate = replaces.equals("deepslate") || replaces.equals("both");

        if (stone) {
            sb.append("      {\n");
            sb.append("        \"target\": {\n");
            sb.append("          \"predicate_type\": \"minecraft:tag_match\",\n");
            sb.append("          \"tag\": \"minecraft:stone_ore_replaceables\"\n");
            sb.append("        },\n");
            sb.append("        \"state\": { \"Name\": \"").append(blockRef).append("\" }\n");
            sb.append("      }");
        }

        if (stone && deepslate) sb.append(",\n");

        if (deepslate) {
            sb.append("      {\n");
            sb.append("        \"target\": {\n");
            sb.append("          \"predicate_type\": \"minecraft:tag_match\",\n");
            sb.append("          \"tag\": \"minecraft:deepslate_ore_replaceables\"\n");
            sb.append("        },\n");
            sb.append("        \"state\": { \"Name\": \"").append(blockRef).append("\" }\n");
            sb.append("      }");
        }

        return sb.toString();
    }

    public static String generatePlacedFeature(String modId, OreDefinition def) {
        String featureRef = modId + ":" + def.id;

        return """
                {
                  "feature": "%s",
                  "placement": [
                    { "type": "minecraft:count", "count": %d },
                    { "type": "minecraft:in_square" },
                    {
                      "type": "minecraft:height_range",
                      "height": {
                        "type": "minecraft:uniform",
                        "min_inclusive": { "absolute": %d },
                        "max_inclusive": { "absolute": %d }
                      }
                    },
                    { "type": "minecraft:biome" }
                  ]
                }
                """.formatted(featureRef, def.veinsPerChunk, def.minY, def.maxY);
    }

    public static String generateBiomeModifier(String modId, OreDefinition def) {
        String biomeTag = switch (def.dimension) {
            case "nether" -> "#minecraft:is_nether";
            case "end"    -> "#minecraft:is_end";
            default       -> "#minecraft:is_overworld";
        };
        String featureRef = modId + ":" + def.id;

        return """
                {
                  "type": "neoforge:add_features",
                  "biomes": "%s",
                  "features": "%s",
                  "step": "underground_ores"
                }
                """.formatted(biomeTag, featureRef);
    }
}
