package com.deadlyhunter.modkit.content;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.content.ore.OreDefinition;
import com.deadlyhunter.modkit.content.recipe.RecipeDefinition;
import com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition;
import com.deadlyhunter.modkit.content.weapon.WeaponDefinition;
import com.deadlyhunter.modkit.content.tool.ToolDefinition;
import com.deadlyhunter.modkit.content.armor.ArmorSetDefinition;
import com.deadlyhunter.modkit.core.ProjectInfo;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public final class ProjectScanner {

    private static final Gson GSON = new Gson();
    private static final int MAX_DEFS_PER_TYPE = 500;

    private ProjectScanner() {}

    public static List<ModkitProject> scanAll() {
        Map<String, ModkitProject> projects = new LinkedHashMap<>();

        for (IModInfo modInfo : ModList.get().getMods()) {
            String modId = modInfo.getModId();
            if (modId.equals(Modkit.MODID)) continue;

            boolean dependsOnModkit = modInfo.getDependencies().stream()
                    .anyMatch(d -> d.getModId().equals(Modkit.MODID));
            if (!dependsOnModkit) continue;

            ModkitProject project = loadFromJar(modInfo);
            if (project != null) projects.put(project.modId, project);
        }

        for (String wsName : WorkspaceManager.listWorkspaces()) {
            ModkitProject project = loadFromWorkspace(wsName);
            if (project != null) {
                if (projects.containsKey(project.modId)) {
                    Modkit.LOGGER.info("[Modkit] Workspace '{}' overrides exported jar.", wsName);
                }
                projects.put(project.modId, project);
            }
        }

        Modkit.LOGGER.info("[Modkit] Discovered {} project(s).", projects.size());
        return new ArrayList<>(projects.values());
    }

    private static ModkitProject loadFromJar(IModInfo modInfo) {
        String modId = modInfo.getModId();
        try {
            Path modRoot = modInfo.getOwningFile().getFile().findResource("/");
            if (modRoot == null || !Files.exists(modRoot)) {
                Modkit.LOGGER.warn("[Modkit] Cannot access resources of mod '{}'", modId);
                return null;
            }

            String displayName = modInfo.getDisplayName() != null ? modInfo.getDisplayName() : modId;
            ModkitProject project = new ModkitProject(modId, displayName, "");

            loadItems(project, modRoot.resolve("modkit").resolve("items"), "jar:" + modId);
            loadBlocks(project, modRoot.resolve("modkit").resolve("blocks"), "jar:" + modId);
            loadOres(project, modRoot.resolve("modkit").resolve("ores"), "jar:" + modId);
            loadRecipes(project, modRoot.resolve("modkit").resolve("recipes"), "jar:" + modId);
            loadOverrides(project, modRoot.resolve("modkit").resolve("overrides"), "jar:" + modId);
            loadWeapons(project, modRoot.resolve("modkit").resolve("weapons"), "jar:" + modId);
            loadTools(project, modRoot.resolve("modkit").resolve("tools"), "jar:" + modId);
            loadArmor(project, modRoot.resolve("modkit").resolve("armor"), "jar:" + modId);
            return project;
        } catch (Exception e) {
            Modkit.LOGGER.error("[Modkit] Failed to scan jar mod '" + modId + "'", e);
            return null;
        }
    }

    private static ModkitProject loadFromWorkspace(String wsName) {
        ProjectInfo info = WorkspaceManager.loadProject(wsName);
        if (info == null) return null;
        ModkitProject project = new ModkitProject(info.modId, info.displayName, info.author);

        Path wsPath = WorkspaceManager.getWorkspacePath(wsName);
        loadItems(project, wsPath.resolve("modkit").resolve("items"), "ws:" + wsName);
        loadBlocks(project, wsPath.resolve("modkit").resolve("blocks"), "ws:" + wsName);
        loadOres(project, wsPath.resolve("modkit").resolve("ores"), "ws:" + wsName);
        loadRecipes(project, wsPath.resolve("modkit").resolve("recipes"), "ws:" + wsName);
        loadOverrides(project, wsPath.resolve("modkit").resolve("overrides"), "ws:" + wsName);
        loadWeapons(project, wsPath.resolve("modkit").resolve("weapons"), "ws:" + wsName);
        loadTools(project, wsPath.resolve("modkit").resolve("tools"), "ws:" + wsName);
        loadArmor(project, wsPath.resolve("modkit").resolve("armor"), "ws:" + wsName);
        return project;
    }

    private static void loadItems(ModkitProject project, Path itemsDir, String sourceTag) {
        loadDefs(itemsDir, sourceTag, project.modId, "item",
                ItemDefinition.class, def -> def.id, ItemDefinition::validate, project.itemDefinitions::add);
    }

    private static void loadBlocks(ModkitProject project, Path blocksDir, String sourceTag) {
        loadDefs(blocksDir, sourceTag, project.modId, "block",
                BlockDefinition.class, def -> def.id, BlockDefinition::validate, project.blockDefinitions::add);
    }

    private static void loadOres(ModkitProject project, Path oresDir, String sourceTag) {
        loadDefs(oresDir, sourceTag, project.modId, "ore",
                OreDefinition.class, def -> def.id, OreDefinition::validate, project.oreDefinitions::add);
    }

    private static void loadRecipes(ModkitProject project, Path recipesDir, String sourceTag) {
        loadDefs(recipesDir, sourceTag, project.modId, "recipe",
                RecipeDefinition.class, def -> def.id, RecipeDefinition::validate, project.recipeDefinitions::add);
    }

    private static void loadOverrides(ModkitProject project, Path overridesDir, String sourceTag) {
        loadDefs(overridesDir, sourceTag, project.modId, "override",
                RecipeOverrideDefinition.class, def -> def.id, RecipeOverrideDefinition::validate,
                project.recipeOverrideDefinitions::add);
    }

    private static void loadWeapons(ModkitProject project, Path weaponsDir, String sourceTag) {
        loadDefs(weaponsDir, sourceTag, project.modId, "weapon",
                WeaponDefinition.class, def -> def.id, WeaponDefinition::validate, project.weaponDefinitions::add);
    }

    private static void loadTools(ModkitProject project, Path toolsDir, String sourceTag) {
        loadDefs(toolsDir, sourceTag, project.modId, "tool",
                ToolDefinition.class, def -> def.id, ToolDefinition::validate, project.toolDefinitions::add);
    }

    private static void loadArmor(ModkitProject project, Path armorDir, String sourceTag) {
        loadDefs(armorDir, sourceTag, project.modId, "armor",
                ArmorSetDefinition.class, def -> def.id, ArmorSetDefinition::validate, project.armorSetDefinitions::add);
    }

    private static <T> void loadDefs(Path dir, String sourceTag, String projectModId, String typeName,
                                     Class<T> defClass, Function<T, String> idGetter,
                                     Function<T, String> validator, Consumer<T> sink) {
        if (!Files.isDirectory(dir)) return;

        Set<String> seenIds = new HashSet<>();
        int loaded = 0, skipped = 0;

        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();

            for (Path file : files) {
                if (loaded >= MAX_DEFS_PER_TYPE) {
                    Modkit.LOGGER.warn("[Modkit] {} reached {} cap ({}). Remaining skipped.",
                            projectModId, typeName, MAX_DEFS_PER_TYPE);
                    break;
                }
                T def = tryLoad(file, sourceTag, defClass, validator);
                if (def == null) { skipped++; continue; }

                String id = idGetter.apply(def);
                if (!seenIds.add(id)) {
                    Modkit.LOGGER.warn("[Modkit] Duplicate {} id '{}' in {}, skipping {}",
                            typeName, id, projectModId, file.getFileName());
                    skipped++;
                    continue;
                }
                sink.accept(def);
                loaded++;
            }
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list " + typeName + " directory of " + projectModId, e);
        }

        if (loaded > 0 || skipped > 0) {
            Modkit.LOGGER.info("[Modkit] {}: loaded {} {}(s), skipped {}", projectModId, loaded, typeName, skipped);
        }
    }

    private static <T> T tryLoad(Path file, String sourceTag, Class<T> cls, Function<T, String> validator) {
        try {
            String json = Files.readString(file);
            T def = GSON.fromJson(json, cls);
            if (def == null) {
                Modkit.LOGGER.warn("[Modkit] {} -> {} is empty", sourceTag, file.getFileName());
                return null;
            }
            String error = validator.apply(def);
            if (error != null) {
                Modkit.LOGGER.warn("[Modkit] {} -> {} invalid: {}", sourceTag, file.getFileName(), error);
                return null;
            }
            return def;
        } catch (Exception e) {
            Modkit.LOGGER.warn("[Modkit] {} -> {} failed to parse: {}", sourceTag, file.getFileName(), e.getMessage());
            return null;
        }
    }
}
