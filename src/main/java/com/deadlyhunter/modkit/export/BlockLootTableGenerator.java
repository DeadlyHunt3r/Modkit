package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.content.block.BlockDefinition;


public final class BlockLootTableGenerator {

    private BlockLootTableGenerator() {}

    public static String getLootTablePath(String modId, String blockId) {
        return "data/" + modId + "/loot_table/blocks/" + blockId + ".json";
    }


    public static String generate(String modId, BlockDefinition def) {
        if (def.isVariant() && "slab".equals(def.variantType)) {
            return generateSlabDrop(modId, def);
        }
        return switch (def.dropMode) {
            case "self"       -> generateSelfDrop(modId, def);
            case "item_mine"  -> generateItemDrop(modId, def, modId + ":" + def.dropItem);
            case "item_other" -> generateItemDrop(modId, def, def.dropItem);
            case "nothing"    -> null;
            default           -> generateSelfDrop(modId, def);
        };
    }

    private static String generateSlabDrop(String modId, BlockDefinition def) {
        return """
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "rolls": 1.0,
                      "bonus_rolls": 0.0,
                      "entries": [
                        {
                          "type": "minecraft:item",
                          "name": "%s:%s",
                          "functions": [
                            {
                              "function": "minecraft:set_count",
                              "count": 2.0,
                              "conditions": [
                                {
                                  "condition": "minecraft:block_state_property",
                                  "block": "%s:%s",
                                  "properties": { "type": "double" }
                                }
                              ],
                              "add": false
                            },
                            { "function": "minecraft:explosion_decay" }
                          ]
                        }
                      ],
                      "conditions": [
                        { "condition": "minecraft:survives_explosion" }
                      ]
                    }
                  ]
                }
                """.formatted(modId, def.id, modId, def.id);
    }

    public static String generateSelfDrop(String modId, BlockDefinition def) {
        return """
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "rolls": 1.0,
                      "bonus_rolls": 0.0,
                      "entries": [
                        {
                          "type": "minecraft:item",
                          "name": "%s:%s"
                        }
                      ],
                      "conditions": [
                        { "condition": "minecraft:survives_explosion" }
                      ]
                    }
                  ]
                }
                """.formatted(modId, def.id);
    }


    public static String generateItemDrop(String modId, BlockDefinition def, String fullDropId) {
        StringBuilder functions = new StringBuilder();

        if (def.dropMin != 1 || def.dropMax != 1) {
            functions.append("""
                            {
                              "function": "minecraft:set_count",
                              "count": {
                                "type": "minecraft:uniform",
                                "min": %d.0,
                                "max": %d.0
                              },
                              "add": false
                            }
                    """.formatted(def.dropMin, def.dropMax));
        }

        if (def.dropFortune) {
            if (functions.length() > 0) functions.append(",\n");
            functions.append("""
                            {
                              "function": "minecraft:apply_bonus",
                              "enchantment": "minecraft:fortune",
                              "formula": "minecraft:ore_drops"
                            }
                    """);
        }

        if (functions.length() > 0) functions.append(",\n");
        functions.append("""
                        {
                          "function": "minecraft:explosion_decay"
                        }
                """);

        return """
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "rolls": 1.0,
                      "bonus_rolls": 0.0,
                      "entries": [
                        {
                          "type": "minecraft:item",
                          "name": "%s",
                          "functions": [
                %s
                          ]
                        }
                      ]
                    }
                  ]
                }
                """.formatted(fullDropId, functions.toString());
    }
}
