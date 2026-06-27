package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.content.block.BlockDefinition;

import java.util.ArrayList;
import java.util.List;

public final class VariantModelGenerator {

    private VariantModelGenerator() {}

    public record Asset(String path, String json) {}

    public static List<Asset> generate(String modId, BlockDefinition def) {
        String tex = modId + ":block/" + (def.textureSource != null ? def.textureSource : def.id);
        String id = def.id;
        return switch (def.variantType) {
            case "slab"   -> slab(modId, id, tex);
            case "stairs" -> stairs(modId, id, tex);
            case "wall"   -> wall(modId, id, tex);
            case "fence"  -> fence(modId, id, tex);
            default       -> new ArrayList<>();
        };
    }

    private static List<Asset> slab(String modId, String id, String tex) {
        List<Asset> out = new ArrayList<>();
        String mb = "assets/" + modId + "/models/block/";
        String bot = modId + ":block/" + id;
        String top = modId + ":block/" + id + "_top";
        String dbl = modId + ":block/" + id + "_double";

        out.add(new Asset(mb + id + ".json", model("minecraft:block/slab", tex)));
        out.add(new Asset(mb + id + "_top.json", model("minecraft:block/slab_top", tex)));
        out.add(new Asset(mb + id + "_double.json", cubeAll(tex)));

        String state = """
                {
                  "variants": {
                    "type=bottom": { "model": "%s" },
                    "type=top": { "model": "%s" },
                    "type=double": { "model": "%s" }
                  }
                }
                """.formatted(bot, top, dbl);

        out.add(new Asset("assets/" + modId + "/blockstates/" + id + ".json", state));
        out.add(new Asset("assets/" + modId + "/models/item/" + id + ".json", parentOnly(bot)));
        return out;
    }

    private static List<Asset> stairs(String modId, String id, String tex) {
        List<Asset> out = new ArrayList<>();
        String mb = "assets/" + modId + "/models/block/";
        String base = modId + ":block/" + id;
        String inner = modId + ":block/" + id + "_inner";
        String outer = modId + ":block/" + id + "_outer";

        out.add(new Asset(mb + id + ".json", stairModel("minecraft:block/stairs", tex)));
        out.add(new Asset(mb + id + "_inner.json", stairModel("minecraft:block/inner_stairs", tex)));
        out.add(new Asset(mb + id + "_outer.json", stairModel("minecraft:block/outer_stairs", tex)));

        out.add(new Asset("assets/" + modId + "/blockstates/" + id + ".json",
                stairsBlockstate(base, inner, outer)));
        out.add(new Asset("assets/" + modId + "/models/item/" + id + ".json", parentOnly(base)));
        return out;
    }

    private static List<Asset> wall(String modId, String id, String tex) {
        List<Asset> out = new ArrayList<>();
        String mb = "assets/" + modId + "/models/block/";
        String post = modId + ":block/" + id + "_post";
        String side = modId + ":block/" + id + "_side";
        String sideTall = modId + ":block/" + id + "_side_tall";

        out.add(new Asset(mb + id + "_post.json", oneTex("minecraft:block/template_wall_post", tex)));
        out.add(new Asset(mb + id + "_side.json", oneTex("minecraft:block/template_wall_side", tex)));
        out.add(new Asset(mb + id + "_side_tall.json", oneTex("minecraft:block/template_wall_side_tall", tex)));
        out.add(new Asset(mb + id + "_inventory.json", oneTex("minecraft:block/wall_inventory", tex)));

        out.add(new Asset("assets/" + modId + "/blockstates/" + id + ".json",
                wallBlockstate(post, side, sideTall)));
        out.add(new Asset("assets/" + modId + "/models/item/" + id + ".json",
                parentOnly(modId + ":block/" + id + "_inventory")));
        return out;
    }

    private static List<Asset> fence(String modId, String id, String tex) {
        List<Asset> out = new ArrayList<>();
        String mb = "assets/" + modId + "/models/block/";
        String post = modId + ":block/" + id + "_post";
        String side = modId + ":block/" + id + "_side";

        out.add(new Asset(mb + id + "_post.json", oneTex("minecraft:block/fence_post", tex)));
        out.add(new Asset(mb + id + "_side.json", oneTex("minecraft:block/fence_side", tex)));
        out.add(new Asset(mb + id + "_inventory.json", oneTex("minecraft:block/fence_inventory", tex)));

        out.add(new Asset("assets/" + modId + "/blockstates/" + id + ".json",
                fenceBlockstate(post, side)));
        out.add(new Asset("assets/" + modId + "/models/item/" + id + ".json",
                parentOnly(modId + ":block/" + id + "_inventory")));
        return out;
    }

    private static String model(String parent, String tex) {
        return """
                {
                  "parent": "%s",
                  "textures": {
                    "bottom": "%s",
                    "top": "%s",
                    "side": "%s"
                  }
                }
                """.formatted(parent, tex, tex, tex);
    }

    private static String stairModel(String parent, String tex) {
        return """
                {
                  "parent": "%s",
                  "textures": {
                    "bottom": "%s",
                    "top": "%s",
                    "side": "%s"
                  }
                }
                """.formatted(parent, tex, tex, tex);
    }

    private static String oneTex(String parent, String tex) {
        return """
                {
                  "parent": "%s",
                  "textures": {
                    "wall": "%s",
                    "texture": "%s"
                  }
                }
                """.formatted(parent, tex, tex);
    }

    private static String cubeAll(String tex) {
        return """
                {
                  "parent": "minecraft:block/cube_all",
                  "textures": { "all": "%s" }
                }
                """.formatted(tex);
    }

    private static String parentOnly(String parent) {
        return "{ \"parent\": \"" + parent + "\" }\n";
    }

    private static String stairsBlockstate(String base, String inner, String outer) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"variants\": {\n");
        List<String> lines = new ArrayList<>();

        String[] facings = { "north", "east", "south", "west" };

        for (String half : new String[]{ "bottom", "top" }) {
            boolean top = half.equals("top");
            for (String facing : facings) {
                for (String shape : new String[]{ "straight", "inner_left", "inner_right", "outer_left", "outer_right" }) {
                    String model = switch (shape) {
                        case "straight" -> base;
                        case "inner_left", "inner_right" -> inner;
                        default -> outer;
                    };
                    int y = stairsRotation(facing, shape, top);
                    StringBuilder v = new StringBuilder();
                    v.append("    \"facing=").append(facing)
                     .append(",half=").append(half)
                     .append(",shape=").append(shape).append("\": { \"model\": \"").append(model).append("\"");
                    if (top) v.append(", \"x\": 180");
                    if (y != 0) v.append(", \"y\": ").append(y);
                    if (top || y != 0) v.append(", \"uvlock\": true");
                    v.append(" }");
                    lines.add(v.toString());
                }
            }
        }
        sb.append(String.join(",\n", lines));
        sb.append("\n  }\n}\n");
        return sb.toString();
    }

    private static int stairsRotation(String facing, String shape, boolean top) {
        int f = switch (facing) {
            case "north" -> 0;
            case "east" -> 1;
            case "south" -> 2;
            default -> 3;
        };
        int y;
        if (shape.equals("inner_left") || shape.equals("outer_left")) {
            y = (f + 3) % 4;
        } else {
            y = f;
        }
        y = (y * 90 + 270) % 360;
        if (top) {
            if (shape.equals("inner_left")) y = (y + 90) % 360;
            else if (shape.equals("inner_right")) y = (y + 90) % 360;
            else if (shape.equals("outer_left")) y = (y + 90) % 360;
            else if (shape.equals("outer_right")) y = (y + 90) % 360;
        }
        return y;
    }

    private static String wallBlockstate(String post, String side, String sideTall) {
        return """
                {
                  "multipart": [
                    { "when": { "up": "true" }, "apply": { "model": "%s" } },
                    { "when": { "north": "low" }, "apply": { "model": "%s", "uvlock": true } },
                    { "when": { "east": "low" }, "apply": { "model": "%s", "y": 90, "uvlock": true } },
                    { "when": { "south": "low" }, "apply": { "model": "%s", "y": 180, "uvlock": true } },
                    { "when": { "west": "low" }, "apply": { "model": "%s", "y": 270, "uvlock": true } },
                    { "when": { "north": "tall" }, "apply": { "model": "%s", "uvlock": true } },
                    { "when": { "east": "tall" }, "apply": { "model": "%s", "y": 90, "uvlock": true } },
                    { "when": { "south": "tall" }, "apply": { "model": "%s", "y": 180, "uvlock": true } },
                    { "when": { "west": "tall" }, "apply": { "model": "%s", "y": 270, "uvlock": true } }
                  ]
                }
                """.formatted(post, side, side, side, side, sideTall, sideTall, sideTall, sideTall);
    }

    private static String fenceBlockstate(String post, String side) {
        return """
                {
                  "multipart": [
                    { "apply": { "model": "%s" } },
                    { "when": { "north": "true" }, "apply": { "model": "%s", "uvlock": true } },
                    { "when": { "east": "true" }, "apply": { "model": "%s", "y": 90, "uvlock": true } },
                    { "when": { "south": "true" }, "apply": { "model": "%s", "y": 180, "uvlock": true } },
                    { "when": { "west": "true" }, "apply": { "model": "%s", "y": 270, "uvlock": true } }
                  ]
                }
                """.formatted(post, side, side, side, side);
    }
}
