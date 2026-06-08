package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.content.recipe.RecipeDefinition;
import com.deadlyhunter.modkit.content.recipe.RecipeDefinition.Ingredient;

import java.util.LinkedHashSet;
import java.util.Set;

public final class RecipeJsonGenerator {

    private RecipeJsonGenerator() {}

    public static String getRecipePath(String modId, String recipeId) {
        return "data/" + modId + "/recipes/" + recipeId + ".json";
    }

    public static String generate(String modId, RecipeDefinition def) {
        return switch (def.type) {
            case "shaped"       -> generateShaped(modId, def);
            case "shapeless"    -> generateShapeless(modId, def);
            case "smelting"     -> generateCookingRecipe(modId, def, "minecraft:smelting");
            case "blasting"     -> generateCookingRecipe(modId, def, "minecraft:blasting");
            case "smoking"      -> generateCookingRecipe(modId, def, "minecraft:smoking");
            case "stonecutting" -> generateStonecutting(modId, def);
            default             -> throw new IllegalArgumentException("Unknown recipe type: " + def.type);
        };
    }

    private static String generateShaped(String modId, RecipeDefinition def) {
        StringBuilder patternJson = new StringBuilder();
        patternJson.append("[");
        for (int i = 0; i < def.pattern.size(); i++) {
            patternJson.append('"').append(escape(def.pattern.get(i))).append('"');
            if (i < def.pattern.size() - 1) patternJson.append(", ");
        }
        patternJson.append("]");

        Set<Character> usedKeys = new LinkedHashSet<>();
        for (String row : def.pattern) {
            for (char c : row.toCharArray()) {
                if (c != ' ') usedKeys.add(c);
            }
        }

        StringBuilder keyJson = new StringBuilder();
        keyJson.append("{\n");
        int i = 0;
        for (Character key : usedKeys) {
            Ingredient ing = def.ingredients.get(String.valueOf(key));
            keyJson.append("    \"").append(key).append("\": ").append(ingredientJson(modId, ing));
            if (i < usedKeys.size() - 1) keyJson.append(",");
            keyJson.append("\n");
            i++;
        }
        keyJson.append("  }");

        return """
                {
                  "type": "minecraft:crafting_shaped",
                  "pattern": %s,
                  "key": %s,
                  "result": %s
                }
                """.formatted(
                patternJson.toString(),
                keyJson.toString(),
                resultJson(modId, def));
    }

    private static String generateShapeless(String modId, RecipeDefinition def) {
        StringBuilder ingredientsJson = new StringBuilder();
        ingredientsJson.append("[");
        int count = 0;
        for (Ingredient ing : def.ingredientList) {
            if (ing == null || ing.isEmpty()) continue;
            if (count > 0) ingredientsJson.append(", ");
            ingredientsJson.append(ingredientJson(modId, ing));
            count++;
        }
        ingredientsJson.append("]");

        return """
                {
                  "type": "minecraft:crafting_shapeless",
                  "ingredients": %s,
                  "result": %s
                }
                """.formatted(ingredientsJson.toString(), resultJson(modId, def));
    }

    private static String generateCookingRecipe(String modId, RecipeDefinition def, String recipeType) {
        String resultId = resolveItemId(modId, def.resultSource, def.resultItem);
        return """
                {
                  "type": "%s",
                  "ingredient": %s,
                  "result": "%s",
                  "experience": %s,
                  "cookingtime": %d
                }
                """.formatted(
                recipeType,
                ingredientJson(modId, def.input),
                resultId,
                Float.toString(def.experience),
                def.cookingTime);
    }

    private static String generateStonecutting(String modId, RecipeDefinition def) {
        String resultId = resolveItemId(modId, def.resultSource, def.resultItem);
        return """
                {
                  "type": "minecraft:stonecutting",
                  "ingredient": %s,
                  "result": "%s",
                  "count": %d
                }
                """.formatted(
                ingredientJson(modId, def.input),
                resultId,
                Math.max(1, def.resultCount));
    }

    private static String ingredientJson(String modId, Ingredient ing) {
        String id = resolveItemId(modId, ing.source, ing.id);
        return "{ \"item\": \"" + id + "\" }";
    }

    private static String resultJson(String modId, RecipeDefinition def) {
        String id = resolveItemId(modId, def.resultSource, def.resultItem);
        if (def.resultCount <= 1) {
            return "{ \"item\": \"" + id + "\" }";
        }
        return "{ \"item\": \"" + id + "\", \"count\": " + def.resultCount + " }";
    }

    private static String resolveItemId(String modId, String source, String id) {
        if ("mine".equals(source)) return modId + ":" + id;
        return id;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
