package com.deadlyhunter.modkit.content.item;

import com.google.gson.annotations.SerializedName;


public class ItemDefinition {

    @SerializedName("modkit_format_version")
    public int formatVersion = 1;

    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("max_stack_size")
    public int maxStackSize = 64;


    @SerializedName("tooltip_lines")
    public java.util.List<String> tooltipLines = java.util.Collections.emptyList();
    @SerializedName("rarity")
    public String rarity = "common";


    @SerializedName("glow")
    public boolean glow = false;


    @SerializedName("fuel_burn_time")
    public int fuelBurnTime = 0;


    @SerializedName("food")
    public FoodData food;


    public String validate() {
        if (id == null || id.isBlank()) return "Missing 'id'";
        if (!id.matches("^[a-z0-9_]{1,40}$")) {
            return "Invalid 'id': only lowercase a-z, 0-9, underscore, max 40 chars";
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = id;
        }
        if (maxStackSize < 1 || maxStackSize > 64) {
            return "max_stack_size must be 1-64";
        }
        if (fuelBurnTime < 0) {
            return "fuel_burn_time cannot be negative";
        }
        if (rarity == null) rarity = "common";
        if (tooltipLines == null) tooltipLines = java.util.Collections.emptyList();
        if (food != null) {
            String foodError = food.validate();
            if (foodError != null) return "food: " + foodError;
        }
        return null;
    }

    public static class FoodData {
        @SerializedName("nutrition")
        public int nutrition = 1;

        @SerializedName("saturation")
        public float saturation = 0.1f;

        @SerializedName("can_always_eat")
        public boolean canAlwaysEat = false;

        @SerializedName("fast_eat")
        public boolean fastEat = false;

        @SerializedName("effects")
        public java.util.List<FoodEffect> effects = new java.util.ArrayList<>();

        public String validate() {
            if (nutrition < 0 || nutrition > 20) return "nutrition must be 0-20";
            if (saturation < 0 || saturation > 20) return "saturation must be 0-20";
            if (effects != null) {
                if (effects.size() > 6) return "max 6 food effects";
                for (FoodEffect e : effects) {
                    String err = e.validate();
                    if (err != null) return err;
                }
            }
            return null;
        }
    }

    public static class FoodEffect {
        @SerializedName("effect")
        public String effect = "minecraft:regeneration";

        @SerializedName("duration")
        public int duration = 100;

        @SerializedName("amplifier")
        public int amplifier = 0;

        @SerializedName("chance")
        public float chance = 1.0f;

        public String validate() {
            if (effect == null || effect.isBlank()) return "effect id missing";
            if (!effect.matches("[a-z0-9_]+:[a-z0-9_/]+")) return "invalid effect id: " + effect;
            if (duration < 1 || duration > 1000000) return "duration must be 1-1000000 ticks";
            if (amplifier < 0 || amplifier > 255) return "amplifier must be 0-255";
            if (chance < 0f || chance > 1f) return "chance must be 0.0-1.0";
            return null;
        }
    }
}
