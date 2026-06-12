package com.deadlyhunter.modkit.content.recipe;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecipeDefinition {

    @SerializedName("modkit_format_version")
    public int formatVersion = 1;

    @SerializedName("type")
    public String type = "shaped";

    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("result_source")
    public String resultSource = "mine";

    @SerializedName("result_item")
    public String resultItem = "";
    @SerializedName("result_count")
    public int resultCount = 1;

    @SerializedName("pattern")
    public List<String> pattern = new ArrayList<>(List.of("   ", "   ", "   "));

    @SerializedName("ingredients")
    public Map<String, Ingredient> ingredients = new LinkedHashMap<>();



    @SerializedName("ingredient_list")
    public List<Ingredient> ingredientList = new ArrayList<>();


    @SerializedName("input")
    public Ingredient input;

    @SerializedName("experience")
    public float experience = 0.1f;

    @SerializedName("cooking_time")
    public int cookingTime = 200;

    @SerializedName("smithing_template")
    public Ingredient smithingTemplate;

    @SerializedName("smithing_base")
    public Ingredient smithingBase;

    @SerializedName("smithing_addition")
    public Ingredient smithingAddition;


    public static class Ingredient {
        @SerializedName("source")
        public String source = "other";

        @SerializedName("id")
        public String id = "";

        public Ingredient() {}
        public Ingredient(String source, String id) {
            this.source = source;
            this.id = id;
        }


        public boolean isEmpty() {
            return id == null || id.isBlank();
        }

        public String validate() {
            if (source == null) source = "other";
            if (!"mine".equals(source) && !"other".equals(source)) {
                return "ingredient source must be 'mine' or 'other'";
            }
            if (id == null || id.isBlank()) return null;
            if ("mine".equals(source)) {
                if (id.contains(":")) return "'mine' ingredient must not include namespace";
                if (!id.matches("[a-z0-9_]{1,40}")) return "invalid mine ingredient id";
            } else {
                if (!id.contains(":")) return "'other' ingredient must include namespace (e.g. minecraft:diamond)";
                if (!id.matches("[a-z0-9_]+:[a-z0-9_/]+")) return "invalid other ingredient id format";
            }
            return null;
        }
    }



    public String validate() {
        if (id == null || id.isBlank()) return "Missing 'id'";
        if (!id.matches("^[a-z0-9_]{1,40}$")) {
            return "Invalid 'id': only lowercase a-z, 0-9, underscore, max 40 chars";
        }
        if (displayName == null || displayName.isBlank()) displayName = id;

        if (type == null) type = "shaped";
        switch (type) {
            case "shaped":
            case "shapeless":
            case "smelting":
            case "blasting":
            case "smoking":
            case "stonecutting":
            case "smithing":
                break;
            default:
                return "type must be: shaped, shapeless, smelting, blasting, smoking, stonecutting, or smithing";
        }


        if (resultSource == null) resultSource = "mine";
        if (!"mine".equals(resultSource) && !"other".equals(resultSource)) {
            return "result_source must be 'mine' or 'other'";
        }
        if (resultItem == null || resultItem.isBlank()) return "result_item is required";
        if ("mine".equals(resultSource)) {
            if (resultItem.contains(":")) return "result_item for 'mine' must not include namespace";
            if (!resultItem.matches("[a-z0-9_]{1,40}")) return "invalid result_item id";
        } else {
            if (!resultItem.contains(":")) return "result_item for 'other' must include namespace";
            if (!resultItem.matches("[a-z0-9_]+:[a-z0-9_/]+")) return "invalid result_item id format";
        }
        if (resultCount < 1 || resultCount > 64) return "result_count must be 1-64";


        switch (type) {
            case "shaped" -> {
                if (pattern == null || pattern.isEmpty()) return "shaped recipe needs a pattern";
                if (pattern.size() > 3) return "pattern can have at most 3 rows";
                int width = -1;
                boolean anyNonSpace = false;
                for (String row : pattern) {
                    if (row == null) return "pattern row cannot be null";
                    if (row.length() > 3) return "pattern row cannot be longer than 3 chars";
                    if (width == -1) width = row.length();
                    else if (row.length() != width) return "all pattern rows must be same length";
                    for (char c : row.toCharArray()) {
                        if (c != ' ') {
                            anyNonSpace = true;
                            if (!ingredients.containsKey(String.valueOf(c))) {
                                return "pattern uses key '" + c + "' but ingredients[] has no entry for it";
                            }
                        }
                    }
                }
                if (!anyNonSpace) return "pattern must contain at least one ingredient";

                for (Map.Entry<String, Ingredient> e : ingredients.entrySet()) {
                    Ingredient ing = e.getValue();
                    if (ing == null || ing.isEmpty()) {
                        return "ingredient '" + e.getKey() + "' is empty";
                    }
                    String err = ing.validate();
                    if (err != null) return "ingredient '" + e.getKey() + "': " + err;
                }
            }
            case "shapeless" -> {
                if (ingredientList == null || ingredientList.isEmpty()) {
                    return "shapeless recipe needs at least 1 ingredient";
                }
                int filled = 0;
                for (Ingredient ing : ingredientList) {
                    if (ing == null || ing.isEmpty()) continue;
                    String err = ing.validate();
                    if (err != null) return "ingredient: " + err;
                    filled++;
                }
                if (filled == 0) return "shapeless recipe needs at least 1 ingredient";
                if (filled > 9) return "shapeless recipe can have at most 9 ingredients";
            }
            case "smelting", "blasting", "smoking" -> {
                if (input == null || input.isEmpty()) return "this recipe needs an input";
                String err = input.validate();
                if (err != null) return "input: " + err;
                if (experience < 0) return "experience cannot be negative";
                if (cookingTime < 1 || cookingTime > 32000) return "cooking_time must be 1-32000";
            }
            case "stonecutting" -> {
                if (input == null || input.isEmpty()) return "stonecutting recipe needs an input";
                String err = input.validate();
                if (err != null) return "input: " + err;
            }
            case "smithing" -> {
                if (smithingTemplate == null || smithingTemplate.isEmpty()) {
                    return "smithing recipe needs a template";
                }
                String terr = smithingTemplate.validate();
                if (terr != null) return "template: " + terr;
                if (smithingBase == null || smithingBase.isEmpty()) {
                    return "smithing recipe needs a base item";
                }
                String berr = smithingBase.validate();
                if (berr != null) return "base: " + berr;
                if (smithingAddition == null || smithingAddition.isEmpty()) {
                    return "smithing recipe needs an addition item";
                }
                String aerr = smithingAddition.validate();
                if (aerr != null) return "addition: " + aerr;
            }
        }
        return null;
    }
}
