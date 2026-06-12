package com.deadlyhunter.modkit.export;

import java.util.Set;


public final class BlockModelGenerator {

    private BlockModelGenerator() {}


    public static String generateBlockstate(String modId, String blockId, String textureMode) {
        boolean facing = "front_other".equals(textureMode) || "front_top_bottom".equals(textureMode);
        if (!facing) {
            return """
                    {
                      "variants": {
                        "": { "model": "%s:block/%s" }
                      }
                    }
                    """.formatted(modId, blockId);
        }
        String model = modId + ":block/" + blockId;
        return """
                {
                  "variants": {
                    "facing=north": { "model": "%s" },
                    "facing=east":  { "model": "%s", "y": 90 },
                    "facing=south": { "model": "%s", "y": 180 },
                    "facing=west":  { "model": "%s", "y": 270 }
                  }
                }
                """.formatted(model, model, model, model);
    }


    public static String generateBlockstate(String modId, String blockId) {
        return generateBlockstate(modId, blockId, "all");
    }


    public static String generateBlockModel(String modId, String blockId,
                                             String textureMode, Set<String> presentSuffixes) {
        switch (textureMode == null ? "all" : textureMode) {
            case "front_other" -> {
                String front = ref(modId, blockId, "front", presentSuffixes);
                String rest  = ref(modId, blockId, "", presentSuffixes);
                return """
                        {
                          "parent": "minecraft:block/orientable_with_bottom",
                          "textures": {
                            "front": "%s",
                            "side": "%s",
                            "top": "%s",
                            "bottom": "%s"
                          }
                        }
                        """.formatted(front, rest, rest, rest);
            }
            case "front_top_bottom" -> {
                String front  = ref(modId, blockId, "front", presentSuffixes);
                String top    = ref(modId, blockId, "top", presentSuffixes);
                String bottom = ref(modId, blockId, "bottom", presentSuffixes);
                String side   = ref(modId, blockId, "", presentSuffixes);
                return """
                        {
                          "parent": "minecraft:block/orientable_with_bottom",
                          "textures": {
                            "front": "%s",
                            "top": "%s",
                            "bottom": "%s",
                            "side": "%s"
                          }
                        }
                        """.formatted(front, top, bottom, side);
            }
            case "all_unique" -> {
                String n = ref(modId, blockId, "north", presentSuffixes);
                String s = ref(modId, blockId, "south", presentSuffixes);
                String e = ref(modId, blockId, "east", presentSuffixes);
                String w = ref(modId, blockId, "west", presentSuffixes);
                String u = ref(modId, blockId, "up", presentSuffixes);
                String d = ref(modId, blockId, "down", presentSuffixes);
                return """
                        {
                          "parent": "minecraft:block/cube",
                          "textures": {
                            "north": "%s",
                            "south": "%s",
                            "east": "%s",
                            "west": "%s",
                            "up": "%s",
                            "down": "%s",
                            "particle": "%s"
                          }
                        }
                        """.formatted(n, s, e, w, u, d, n);
            }
            default -> {
                String all = ref(modId, blockId, "", presentSuffixes);
                return """
                        {
                          "parent": "minecraft:block/cube_all",
                          "textures": {
                            "all": "%s"
                          }
                        }
                        """.formatted(all);
            }
        }
    }


    public static String generateBlockModel(String modId, String blockId, boolean hasTexture) {
        return generateBlockModel(modId, blockId, "all", hasTexture ? Set.of("") : Set.of());
    }

    private static String ref(String modId, String blockId, String suffix, Set<String> present) {
        if (present.contains(suffix)) {
            return modId + ":block/" + blockId + (suffix.isEmpty() ? "" : "_" + suffix);
        }
        return "modkit:block/missing";
    }


    public static String generateItemModel(String modId, String blockId) {
        return """
                {
                  "parent": "%s:block/%s"
                }
                """.formatted(modId, blockId);
    }
}
