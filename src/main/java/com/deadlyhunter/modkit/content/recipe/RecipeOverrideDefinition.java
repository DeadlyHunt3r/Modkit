package com.deadlyhunter.modkit.content.recipe;

import com.google.gson.annotations.SerializedName;

public class RecipeOverrideDefinition {

    @SerializedName("modkit_format_version")
    public int formatVersion = 1;

    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("mode")
    public String mode = "disable";

    @SerializedName("target_recipe")
    public String targetRecipe = "";

    @SerializedName("target_namespace")
    public String targetNamespace = "minecraft";

    @SerializedName("replacement")
    public RecipeDefinition replacement;

    public boolean isDisable() {
        return "disable".equals(mode);
    }

    public boolean isVanillaTarget() {
        return targetNamespace == null || targetNamespace.isBlank() || "minecraft".equals(targetNamespace);
    }

    public String validate() {
        if (id == null || id.isBlank()) return "Missing 'id'";
        if (!id.matches("^[a-z0-9_]{1,48}$")) {
            return "Invalid 'id': only lowercase a-z, 0-9, underscore, max 48 chars";
        }
        if (displayName == null || displayName.isBlank()) displayName = id;

        if (mode == null) mode = "disable";
        if (!"disable".equals(mode) && !"replace".equals(mode)) {
            return "mode must be 'disable' or 'replace'";
        }

        if (targetRecipe == null || targetRecipe.isBlank()) return "target_recipe is required";
        if (!targetRecipe.matches("[a-z0-9_/.-]+")) return "invalid target_recipe path";

        if (targetNamespace == null || targetNamespace.isBlank()) targetNamespace = "minecraft";
        if (!targetNamespace.matches("[a-z0-9_.-]+")) return "invalid target_namespace";

        if ("replace".equals(mode)) {
            if (replacement == null) return "replace mode requires a replacement recipe";
            String err = replacement.validate();
            if (err != null) return "replacement: " + err;
        }

        return null;
    }
}
