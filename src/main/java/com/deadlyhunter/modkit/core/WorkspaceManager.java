package com.deadlyhunter.modkit.core;

import com.deadlyhunter.modkit.Modkit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class WorkspaceManager {

    private static final Pattern VALID_MOD_NAME = Pattern.compile("^[a-z0-9_]{3,30}$");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<String> RESERVED = List.of(
            "minecraft", "forge", "neoforge", "fabric", "quilt", "modkit", "omnidev"
    );

    private static Path workspacesRoot;

    private WorkspaceManager() {}

    public static void init() {
        workspacesRoot = FMLPaths.GAMEDIR.get().resolve("modkit").resolve("workspaces");
        try {
            Files.createDirectories(workspacesRoot);
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to create workspaces directory", e);
        }
    }

    public static Path getWorkspacesRoot() {
        return workspacesRoot;
    }

    public static Path getWorkspacePath(String modName) {
        return workspacesRoot.resolve(modName);
    }

    public static boolean exists(String modName) {
        return Files.isDirectory(getWorkspacePath(modName));
    }

    public static List<String> listWorkspaces() {
        List<String> result = new ArrayList<>();
        if (!Files.isDirectory(workspacesRoot)) return result;
        try (Stream<Path> stream = Files.list(workspacesRoot)) {
            stream.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .forEach(result::add);
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list workspaces", e);
        }
        return result;
    }

    public static CreateResult create(String author, String modName) {
        if (!isValidModName(modName)) {
            return CreateResult.failure("Mod name must be 3-30 characters, only lowercase letters, numbers and underscores.");
        }
        if (RESERVED.contains(modName)) {
            return CreateResult.failure("'" + modName + "' is a reserved name and cannot be used.");
        }
        if (exists(modName)) {
            return CreateResult.failure("A workspace named '" + modName + "' already exists.");
        }

        Path workspace = getWorkspacePath(modName);
        try {
            Files.createDirectories(workspace.resolve("assets"));
            Files.createDirectories(workspace.resolve("data"));
            Files.createDirectories(workspace.resolve("modkit").resolve("items"));
            Files.createDirectories(workspace.resolve("modkit").resolve("blocks"));
            Files.createDirectories(workspace.resolve("modkit").resolve("ores"));
            Files.createDirectories(workspace.resolve("modkit").resolve("recipes"));
            Files.createDirectories(workspace.resolve("modkit").resolve("weapons"));
            Files.createDirectories(workspace.resolve("modkit").resolve("tools"));
            Files.createDirectories(workspace.resolve("modkit").resolve("armor"));
            Files.createDirectories(workspace.resolve("assets").resolve("textures").resolve("armor"));

            ProjectInfo info = new ProjectInfo(author, modName);
            Files.writeString(workspace.resolve("project_info.json"), GSON.toJson(info));

            return CreateResult.success(info);
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to create workspace " + modName, e);
            try {
                deleteRecursive(workspace);
            } catch (IOException ignored) {}
            return CreateResult.failure("Failed to create workspace: " + e.getMessage());
        }
    }

    public static ProjectInfo loadProject(String modName) {
        Path infoFile = getWorkspacePath(modName).resolve("project_info.json");
        if (!Files.exists(infoFile)) return null;
        try {
            String json = Files.readString(infoFile);
            return GSON.fromJson(json, ProjectInfo.class);
        } catch (Exception e) {
            Modkit.LOGGER.error("[Modkit] Failed to load project_info for " + modName, e);
            return null;
        }
    }

    public static boolean isValidModName(String name) {
        return name != null && VALID_MOD_NAME.matcher(name).matches();
    }

    public static String getValidationHint() {
        return "Mod name must be 3-30 characters, only lowercase letters, numbers and underscores.";
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }
    }

    public static class CreateResult {
        public final boolean success;
        public final String message;
        public final ProjectInfo info;

        private CreateResult(boolean success, String message, ProjectInfo info) {
            this.success = success;
            this.message = message;
            this.info = info;
        }

        public static CreateResult success(ProjectInfo info) {
            return new CreateResult(true, "Workspace created.", info);
        }

        public static CreateResult failure(String message) {
            return new CreateResult(false, message, null);
        }
    }
}
