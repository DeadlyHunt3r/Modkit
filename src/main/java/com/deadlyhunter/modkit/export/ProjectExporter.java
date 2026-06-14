package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.core.ProjectInfo;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public final class ProjectExporter {

    private static final Gson GSON = new Gson();

    private ProjectExporter() {}

    public static ExportResult export(String modName) {
        ProjectInfo info = WorkspaceManager.loadProject(modName);
        if (info == null) {
            return ExportResult.failure("Workspace '" + modName + "' not found or project_info.json is corrupt.");
        }

        Path workspace = WorkspaceManager.getWorkspacePath(modName);
        Path exportsDir = getExportsDir();
        Path finalJar = exportsDir.resolve(modName + ".jar");
        Path tmpJar = exportsDir.resolve(modName + ".jar.tmp");

        List<ItemDefinition> items = loadItems(workspace);
        List<BlockDefinition> blocks = loadBlocks(workspace);
        List<com.deadlyhunter.modkit.content.ore.OreDefinition> ores = loadOres(workspace);
        List<com.deadlyhunter.modkit.content.recipe.RecipeDefinition> recipes = loadRecipes(workspace);
        List<com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition> overrides = loadOverrides(workspace);
        List<com.deadlyhunter.modkit.content.weapon.WeaponDefinition> weapons = loadWeapons(workspace);
        List<com.deadlyhunter.modkit.content.tool.ToolDefinition> tools = loadTools(workspace);
        List<com.deadlyhunter.modkit.content.armor.ArmorSetDefinition> armorSets = loadArmorSets(workspace);
        List<String> warnings = new ArrayList<>();

        try {
            Files.createDirectories(exportsDir);
            Files.deleteIfExists(tmpJar);

            try (ZipOutputStream zip = new ZipOutputStream(
                    Files.newOutputStream(tmpJar, StandardOpenOption.CREATE_NEW))) {

                writeEntry(zip, "META-INF/MANIFEST.MF", ManifestGenerator.generate(info));
                java.util.Set<String> loadAfterMods = new java.util.LinkedHashSet<>();
                for (com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition ov : overrides) {
                    if (ov.targetNamespace != null && !ov.isVanillaTarget()) {
                        loadAfterMods.add(ov.targetNamespace);
                    }
                }
                writeEntry(zip, "META-INF/mods.toml", ModsTomlGenerator.generate(info, loadAfterMods));
                writeEntry(zip, "pack.mcmeta", PackMcmetaGenerator.generate(info));


                writeEntry(zip,
                        "assets/" + info.modId + "/lang/en_us.json",
                        LangGenerator.generate(info, items, blocks, weapons, tools, armorSets));


                Set<String> writtenAssetPaths = new HashSet<>();


                Path workspaceItemTexDir = workspace.resolve("assets").resolve("textures").resolve("item");
                for (ItemDefinition def : items) {
                    Path userTex = workspaceItemTexDir.resolve(def.id + ".png");
                    String modelPath = "assets/" + info.modId + "/models/item/" + def.id + ".json";

                    if (Files.isRegularFile(userTex)) {
                        String texPath = "assets/" + info.modId + "/textures/item/" + def.id + ".png";
                        writeBinary(zip, texPath, Files.readAllBytes(userTex));
                        writeEntry(zip, modelPath, ItemModelGenerator.generateWithTexture(info.modId, def.id));
                    } else {
                        writeEntry(zip, modelPath, ItemModelGenerator.generateFallback(def.id));
                        warnings.add("Item '" + def.id + "' has no texture (expected: assets/textures/item/"
                                + def.id + ".png) — using Modkit fallback icon.");
                    }
                    writtenAssetPaths.add("textures/item/" + def.id + ".png");
                    writtenAssetPaths.add("models/item/" + def.id + ".json");
                }


                for (com.deadlyhunter.modkit.content.weapon.WeaponDefinition def : weapons) {
                    Path userTex = workspaceItemTexDir.resolve(def.id + ".png");
                    String modelPath = "assets/" + info.modId + "/models/item/" + def.id + ".json";

                    if (Files.isRegularFile(userTex)) {
                        String texPath = "assets/" + info.modId + "/textures/item/" + def.id + ".png";
                        writeBinary(zip, texPath, Files.readAllBytes(userTex));
                        writeEntry(zip, modelPath,
                                ItemModelGenerator.generateWeaponWithTexture(info.modId, def.id));
                    } else {
                        writeEntry(zip, modelPath, ItemModelGenerator.generateWeaponFallback());
                        warnings.add("Weapon '" + def.id + "' has no texture (expected: assets/textures/item/"
                                + def.id + ".png) — using Modkit fallback icon.");
                    }
                    writtenAssetPaths.add("textures/item/" + def.id + ".png");
                    writtenAssetPaths.add("models/item/" + def.id + ".json");
                }


                for (com.deadlyhunter.modkit.content.tool.ToolDefinition def : tools) {
                    Path userTex = workspaceItemTexDir.resolve(def.id + ".png");
                    String modelPath = "assets/" + info.modId + "/models/item/" + def.id + ".json";

                    if (Files.isRegularFile(userTex)) {
                        String texPath = "assets/" + info.modId + "/textures/item/" + def.id + ".png";
                        writeBinary(zip, texPath, Files.readAllBytes(userTex));
                        writeEntry(zip, modelPath,
                                ItemModelGenerator.generateWeaponWithTexture(info.modId, def.id));
                    } else {
                        writeEntry(zip, modelPath, ItemModelGenerator.generateWeaponFallback());
                        warnings.add("Tool '" + def.id + "' has no texture (expected: assets/textures/item/"
                                + def.id + ".png) — using Modkit fallback icon.");
                    }
                    writtenAssetPaths.add("textures/item/" + def.id + ".png");
                    writtenAssetPaths.add("models/item/" + def.id + ".json");
                }


                Path workspaceArmorTexDir = workspace.resolve("assets").resolve("textures").resolve("armor");
                for (com.deadlyhunter.modkit.content.armor.ArmorSetDefinition def : armorSets) {

                    for (String pieceType : com.deadlyhunter.modkit.content.armor.ArmorSetDefinition.PIECE_TYPES) {
                        if (!def.hasPiece(pieceType)) continue;
                        String pieceId = def.pieceItemId(pieceType);
                        Path userTex = workspaceItemTexDir.resolve(pieceId + ".png");
                        String modelPath = "assets/" + info.modId + "/models/item/" + pieceId + ".json";

                        if (Files.isRegularFile(userTex)) {
                            String texPath = "assets/" + info.modId + "/textures/item/" + pieceId + ".png";
                            writeBinary(zip, texPath, Files.readAllBytes(userTex));
                            writeEntry(zip, modelPath,
                                    ItemModelGenerator.generateWithTexture(info.modId, pieceId));
                        } else {
                            writeEntry(zip, modelPath, ItemModelGenerator.generateFallback(pieceId));
                            warnings.add("Armor piece '" + pieceId + "' has no icon texture — using fallback.");
                        }
                        writtenAssetPaths.add("textures/item/" + pieceId + ".png");
                        writtenAssetPaths.add("models/item/" + pieceId + ".json");
                    }


                    for (int layer = 1; layer <= 2; layer++) {
                        String layerFile = def.id + "_layer_" + layer + ".png";
                        Path userLayer = workspaceArmorTexDir.resolve(layerFile);
                        if (Files.isRegularFile(userLayer)) {
                            writeBinary(zip,
                                    "assets/" + info.modId + "/textures/models/armor/" + layerFile,
                                    Files.readAllBytes(userLayer));
                        } else {

                            boolean needed = (layer == 1 && (def.hasHelmet || def.hasChestplate || def.hasBoots))
                                    || (layer == 2 && def.hasLeggings);
                            if (needed) {
                                warnings.add("Armor set '" + def.id + "' is missing " + layerFile
                                        + " — pieces will render invisible when worn.");
                            }
                        }
                    }
                }


                Path workspaceBlockTexDir = workspace.resolve("assets").resolve("textures").resolve("block");
                for (BlockDefinition def : blocks) {
                    if (def.isVariant()) {
                        for (VariantModelGenerator.Asset a : VariantModelGenerator.generate(info.modId, def)) {
                            writeEntry(zip, a.path(), a.json());
                            writtenAssetPaths.add(a.path().replace("assets/" + info.modId + "/", ""));
                        }
                        continue;
                    }


                    java.util.Set<String> presentSuffixes = new java.util.HashSet<>();
                    for (String suffix : BlockDefinition.textureSuffixes(def.textureMode)) {
                        String fileName = suffix.isEmpty() ? def.id + ".png" : def.id + "_" + suffix + ".png";
                        Path userTex = workspaceBlockTexDir.resolve(fileName);
                        if (Files.isRegularFile(userTex)) {
                            presentSuffixes.add(suffix);
                            writeBinary(zip,
                                    "assets/" + info.modId + "/textures/block/" + fileName,
                                    Files.readAllBytes(userTex));
                            writtenAssetPaths.add("textures/block/" + fileName);
                        } else {
                            String label = suffix.isEmpty() ? "base" : suffix;
                            warnings.add("Block '" + def.id + "' is missing the '" + label
                                    + "' texture (expected: assets/textures/block/" + fileName
                                    + ") — using Modkit fallback for that face.");
                        }
                    }

                    String blockstatePath  = "assets/" + info.modId + "/blockstates/"  + def.id + ".json";
                    String blockModelPath  = "assets/" + info.modId + "/models/block/" + def.id + ".json";
                    String itemModelPath   = "assets/" + info.modId + "/models/item/"  + def.id + ".json";

                    writeEntry(zip, blockstatePath,
                            BlockModelGenerator.generateBlockstate(info.modId, def.id, def.textureMode));
                    writeEntry(zip, blockModelPath,
                            BlockModelGenerator.generateBlockModel(info.modId, def.id, def.textureMode, presentSuffixes));
                    writeEntry(zip, itemModelPath,  BlockModelGenerator.generateItemModel(info.modId, def.id));

                    writtenAssetPaths.add("blockstates/" + def.id + ".json");
                    writtenAssetPaths.add("models/block/" + def.id + ".json");
                    writtenAssetPaths.add("models/item/"  + def.id + ".json");
                }


                copyAssetsExcept(zip, workspace.resolve("assets"), info.modId, writtenAssetPaths);
                copyDirIntoZip(zip, workspace.resolve("data"), "data/");
                copyDirIntoZip(zip, workspace.resolve("modkit"), "modkit/");

                java.util.Map<String, java.util.List<String>> mergedTags = new java.util.LinkedHashMap<>();

                java.util.Map<String, String> autoBlockTags =
                        BlockTagsGenerator.generate(info.modId, blocks);
                for (java.util.Map.Entry<String, String> e : autoBlockTags.entrySet()) {
                    for (String v : extractTagValues(e.getValue())) {
                        mergedTags.computeIfAbsent(e.getKey(), k -> new java.util.ArrayList<>()).add(v);
                    }
                }


                for (BlockDefinition def : blocks) {
                    String lootJson = BlockLootTableGenerator.generate(info.modId, def);
                    if (lootJson == null) continue;
                    String path = BlockLootTableGenerator.getLootTablePath(info.modId, def.id);
                    writeEntry(zip, path, lootJson);
                }


                java.util.Set<String> blockIds = new java.util.HashSet<>();
                for (BlockDefinition b : blocks) blockIds.add(b.id);

                for (com.deadlyhunter.modkit.content.ore.OreDefinition ore : ores) {
                    if (!blockIds.contains(ore.blockId)) {
                        warnings.add("Ore '" + ore.id + "' references unknown block '"
                                + ore.blockId + "' — skipped.");
                        continue;
                    }
                    writeEntry(zip,
                            OreWorldgenGenerator.getConfiguredFeaturePath(info.modId, ore.id),
                            OreWorldgenGenerator.generateConfiguredFeature(info.modId, ore));
                    writeEntry(zip,
                            OreWorldgenGenerator.getPlacedFeaturePath(info.modId, ore.id),
                            OreWorldgenGenerator.generatePlacedFeature(info.modId, ore));
                    writeEntry(zip,
                            OreWorldgenGenerator.getBiomeModifierPath(info.modId, ore.id),
                            OreWorldgenGenerator.generateBiomeModifier(info.modId, ore));
                }


                for (com.deadlyhunter.modkit.content.recipe.RecipeDefinition recipe : recipes) {
                    try {
                        String json = RecipeJsonGenerator.generate(info.modId, recipe);
                        writeEntry(zip,
                                RecipeJsonGenerator.getRecipePath(info.modId, recipe.id),
                                json);
                    } catch (Exception e) {
                        warnings.add("Recipe '" + recipe.id + "' could not be exported: " + e.getMessage());
                    }
                }


                java.util.Set<String> usedOverridePaths = new java.util.HashSet<>();
                for (com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition ov : overrides) {
                    try {
                        String path = RecipeOverrideGenerator.getOverridePath(ov);
                        if (!usedOverridePaths.add(path)) {
                            warnings.add("Override '" + ov.id + "' targets a recipe already overridden by another entry ("
                                    + path + ") — skipped.");
                            continue;
                        }
                        String json = RecipeOverrideGenerator.generate(info.modId, ov);
                        writeEntry(zip, path, json);
                    } catch (Exception e) {
                        warnings.add("Override '" + ov.id + "' could not be exported: " + e.getMessage());
                    }
                }


                java.util.List<TagJsonGenerator.TagFile> userTagFiles = new java.util.ArrayList<>();
                userTagFiles.addAll(TagJsonGenerator.generateItemTags(info.modId, items));
                userTagFiles.addAll(TagJsonGenerator.generateBlockTags(info.modId, blocks));
                for (TagJsonGenerator.TagFile tf : userTagFiles) {
                    for (String v : extractTagValues(tf.json())) {
                        mergedTags.computeIfAbsent(tf.path(), k -> new java.util.ArrayList<>()).add(v);
                    }
                }


                for (java.util.Map.Entry<String, java.util.List<String>> e : mergedTags.entrySet()) {
                    java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>(e.getValue());
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\n  \"replace\": false,\n  \"values\": [\n");
                    int i = 0;
                    for (String v : unique) {
                        sb.append("    \"").append(v).append("\"");
                        if (++i < unique.size()) sb.append(",");
                        sb.append("\n");
                    }
                    sb.append("  ]\n}\n");
                    try {
                        writeEntry(zip, e.getKey(), sb.toString());
                    } catch (Exception ex) {
                        warnings.add("Tag file '" + e.getKey() + "' could not be written: " + ex.getMessage());
                    }
                }
            }

            Files.move(tmpJar, finalJar,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            return ExportResult.success(finalJar, items.size(), blocks.size(), ores.size(),
                    recipes.size(), weapons.size(), tools.size(), armorSets.size(), warnings);

        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Export failed for " + modName, e);
            try { Files.deleteIfExists(tmpJar); } catch (IOException ignored) {}
            return ExportResult.failure("Export failed: " + e.getMessage());
        }
    }

    public static Path getExportsDir() {
        return FMLPaths.GAMEDIR.get().resolve("modkit").resolve("exports");
    }

    private static List<ItemDefinition> loadItems(Path workspace) {
        return loadDefs(workspace.resolve("modkit").resolve("items"), ItemDefinition.class, ItemDefinition::validate);
    }

    private static List<BlockDefinition> loadBlocks(Path workspace) {
        return loadDefs(workspace.resolve("modkit").resolve("blocks"), BlockDefinition.class, BlockDefinition::validate);
    }

    private static List<com.deadlyhunter.modkit.content.ore.OreDefinition> loadOres(Path workspace) {
        return loadDefs(workspace.resolve("modkit").resolve("ores"),
                com.deadlyhunter.modkit.content.ore.OreDefinition.class,
                com.deadlyhunter.modkit.content.ore.OreDefinition::validate);
    }

    private static List<com.deadlyhunter.modkit.content.recipe.RecipeDefinition> loadRecipes(Path workspace) {
        return loadDefs(workspace.resolve("modkit").resolve("recipes"),
                com.deadlyhunter.modkit.content.recipe.RecipeDefinition.class,
                com.deadlyhunter.modkit.content.recipe.RecipeDefinition::validate);
    }

    private static List<com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition> loadOverrides(Path workspace) {
        return loadDefs(workspace.resolve("modkit").resolve("overrides"),
                com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition.class,
                com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition::validate);
    }

    private static List<com.deadlyhunter.modkit.content.weapon.WeaponDefinition> loadWeapons(Path workspace) {
        return loadDefs(workspace.resolve("modkit").resolve("weapons"),
                com.deadlyhunter.modkit.content.weapon.WeaponDefinition.class,
                com.deadlyhunter.modkit.content.weapon.WeaponDefinition::validate);
    }

    private static List<com.deadlyhunter.modkit.content.tool.ToolDefinition> loadTools(Path workspace) {
        return loadDefs(workspace.resolve("modkit").resolve("tools"),
                com.deadlyhunter.modkit.content.tool.ToolDefinition.class,
                com.deadlyhunter.modkit.content.tool.ToolDefinition::validate);
    }

    private static List<com.deadlyhunter.modkit.content.armor.ArmorSetDefinition> loadArmorSets(Path workspace) {
        return loadDefs(workspace.resolve("modkit").resolve("armor"),
                com.deadlyhunter.modkit.content.armor.ArmorSetDefinition.class,
                com.deadlyhunter.modkit.content.armor.ArmorSetDefinition::validate);
    }

    private static <T> List<T> loadDefs(Path dir, Class<T> cls,
                                        java.util.function.Function<T, String> validator) {
        List<T> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) return result;
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path file : (Iterable<Path>) stream.sorted()::iterator) {
                if (!file.getFileName().toString().endsWith(".json")) continue;
                try {
                    T def = GSON.fromJson(Files.readString(file), cls);
                    if (def != null && validator.apply(def) == null) result.add(def);
                    else Modkit.LOGGER.warn("[Modkit] Skipping invalid def during export: {}", file.getFileName());
                } catch (Exception e) {
                    Modkit.LOGGER.warn("[Modkit] Could not parse {} during export: {}",
                            file.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list def dir during export", e);
        }
        return result;
    }


    private static java.util.List<String> extractTagValues(String tagJson) {
        java.util.List<String> values = new java.util.ArrayList<>();
        if (tagJson == null) return values;
        try {
            com.google.gson.JsonObject obj =
                    com.google.gson.JsonParser.parseString(tagJson).getAsJsonObject();
            if (obj.has("values") && obj.get("values").isJsonArray()) {
                for (com.google.gson.JsonElement el : obj.getAsJsonArray("values")) {
                    if (el.isJsonPrimitive()) {
                        values.add(el.getAsString());
                    } else if (el.isJsonObject() && el.getAsJsonObject().has("id")) {
                        values.add(el.getAsJsonObject().get("id").getAsString());
                    }
                }
            }
        } catch (Exception ignored) {}
        return values;
    }

    private static void writeEntry(ZipOutputStream zip, String path, String content) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry);
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void writeBinary(ZipOutputStream zip, String path, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }


    private static void copyAssetsExcept(ZipOutputStream zip, Path assetsDir,
                                          String modId, Set<String> skip) throws IOException {
        if (!Files.isDirectory(assetsDir)) return;

        try (Stream<Path> walk = Files.walk(assetsDir)) {
            for (Path path : (Iterable<Path>) walk.sorted()::iterator) {
                if (path.equals(assetsDir)) continue;

                String relative = assetsDir.relativize(path).toString().replace('\\', '/');
                if (skip.contains(relative)) continue;
                if (relative.equals("lang/en_us.json")) continue;

                String entryName = "assets/" + modId + "/" + relative;
                if (Files.isDirectory(path)) {
                    zip.putNextEntry(new ZipEntry(entryName + "/"));
                    zip.closeEntry();
                } else {
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
        }
    }

    private static void copyDirIntoZip(ZipOutputStream zip, Path sourceDir, String zipPrefix) throws IOException {
        if (!Files.isDirectory(sourceDir)) return;

        try (Stream<Path> walk = Files.walk(sourceDir)) {
            for (Path path : (Iterable<Path>) walk.sorted()::iterator) {
                if (path.equals(sourceDir)) continue;

                String relative = sourceDir.relativize(path).toString().replace('\\', '/');
                String entryName = zipPrefix + relative;

                if (Files.isDirectory(path)) {
                    zip.putNextEntry(new ZipEntry(entryName + "/"));
                    zip.closeEntry();
                } else {
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
        }
    }


    public static class ExportResult {
        public final boolean success;
        public final String message;
        public final Path jarPath;
        public final int itemCount;
        public final int blockCount;
        public final int oreCount;
        public final int recipeCount;
        public final int weaponCount;
        public final int toolCount;
        public final int armorSetCount;
        public final List<String> warnings;

        private ExportResult(boolean success, String message, Path jarPath,
                             int itemCount, int blockCount, int oreCount, int recipeCount,
                             int weaponCount, int toolCount, int armorSetCount,
                             List<String> warnings) {
            this.success = success;
            this.message = message;
            this.jarPath = jarPath;
            this.itemCount = itemCount;
            this.blockCount = blockCount;
            this.oreCount = oreCount;
            this.recipeCount = recipeCount;
            this.weaponCount = weaponCount;
            this.toolCount = toolCount;
            this.armorSetCount = armorSetCount;
            this.warnings = warnings == null ? List.of() : warnings;
        }

        public static ExportResult success(Path jar, int itemCount, int blockCount, int oreCount,
                                            int recipeCount, int weaponCount, int toolCount,
                                            int armorSetCount, List<String> warnings) {
            return new ExportResult(true, "Export successful.", jar,
                    itemCount, blockCount, oreCount, recipeCount, weaponCount, toolCount,
                    armorSetCount, warnings);
        }

        public static ExportResult failure(String message) {
            return new ExportResult(false, message, null, 0, 0, 0, 0, 0, 0, 0, List.of());
        }
    }
}
