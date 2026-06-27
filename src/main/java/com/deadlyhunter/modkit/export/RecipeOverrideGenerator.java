package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition;

public final class RecipeOverrideGenerator {

    private RecipeOverrideGenerator() {}

    public static String getOverridePath(RecipeOverrideDefinition def) {
        String ns = (def.targetNamespace == null || def.targetNamespace.isBlank())
                ? "minecraft" : def.targetNamespace;
        return "data/" + ns + "/recipe/" + def.targetRecipe + ".json";
    }

    public static String generate(String modId, RecipeOverrideDefinition def) {
        if (def.isDisable()) {
            return """
                    {
                      "type": "minecraft:crafting_shaped",
                      "conditions": [
                        { "type": "forge:false" }
                      ],
                      "pattern": [ "#" ],
                      "key": { "#": { "item": "minecraft:barrier" } },
                      "result": { "item": "minecraft:barrier" }
                    }
                    """;
        }
        return RecipeJsonGenerator.generate(modId, def.replacement);
    }
}
