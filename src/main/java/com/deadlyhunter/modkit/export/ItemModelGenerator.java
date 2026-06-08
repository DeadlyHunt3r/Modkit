package com.deadlyhunter.modkit.export;

public final class ItemModelGenerator {

    private static final String GENERATED_TEMPLATE = """
            {
              "parent": "minecraft:item/generated",
              "textures": {
                "layer0": "%s"
              }
            }
            """;

    private static final String HANDHELD_TEMPLATE = """
            {
              "parent": "minecraft:item/handheld",
              "textures": {
                "layer0": "%s"
              }
            }
            """;

    private ItemModelGenerator() {}

    public static String generateWithTexture(String modId, String itemId) {
        return GENERATED_TEMPLATE.formatted(modId + ":item/" + itemId);
    }

    public static String generateFallback(String itemId) {
        return GENERATED_TEMPLATE.formatted("modkit:item/missing");
    }

    public static String generateWeaponWithTexture(String modId, String weaponId) {
        return HANDHELD_TEMPLATE.formatted(modId + ":item/" + weaponId);
    }

    public static String generateWeaponFallback() {
        return HANDHELD_TEMPLATE.formatted("modkit:item/missing");
    }
}
