package com.deadlyhunter.modkit.export;


public final class BlockModelGenerator {

    private BlockModelGenerator() {}

    public static String generateBlockstate(String modId, String blockId) {
        return """
                {
                  "variants": {
                    "": { "model": "%s:block/%s" }
                  }
                }
                """.formatted(modId, blockId);
    }


    public static String generateBlockModel(String modId, String blockId, boolean hasTexture) {
        String textureRef = hasTexture
                ? modId + ":block/" + blockId
                : "modkit:block/missing";

        return """
                {
                  "parent": "minecraft:block/cube_all",
                  "textures": {
                    "all": "%s"
                  }
                }
                """.formatted(textureRef);
    }


    public static String generateItemModel(String modId, String blockId) {
        return """
                {
                  "parent": "%s:block/%s"
                }
                """.formatted(modId, blockId);
    }
}
