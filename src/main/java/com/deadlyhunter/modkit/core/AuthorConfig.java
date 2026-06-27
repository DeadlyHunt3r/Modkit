package com.deadlyhunter.modkit.core;

import com.deadlyhunter.modkit.Modkit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public final class AuthorConfig {

    private static final Pattern VALID_AUTHOR = Pattern.compile("^[a-z0-9_]{3,20}$");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configFile;
    private static String author = null;

    private AuthorConfig() {}

    public static void init() {
        Path modkitDir = FMLPaths.GAMEDIR.get().resolve("modkit");
        try {
            Files.createDirectories(modkitDir);
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to create modkit directory", e);
        }
        configFile = modkitDir.resolve("author.json");
        load();
    }

    private static void load() {
        if (!Files.exists(configFile)) {
            return;
        }
        try {
            String json = Files.readString(configFile);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj != null && obj.has("author")) {
                author = obj.get("author").getAsString();
            }
        } catch (Exception e) {
            Modkit.LOGGER.error("[Modkit] Failed to read author.json", e);
        }
    }

    private static void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("author", author);
            Files.writeString(configFile, GSON.toJson(obj));
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write author.json", e);
        }
    }

    public static String getAuthor() {
        return author;
    }

    public static boolean isSet() {
        return author != null && !author.isEmpty();
    }

    public static boolean setAuthor(String newAuthor) {
        if (!isValid(newAuthor)) {
            return false;
        }
        author = newAuthor;
        save();
        return true;
    }

    public static boolean isValid(String candidate) {
        return candidate != null && VALID_AUTHOR.matcher(candidate).matches();
    }

    public static String getValidationHint() {
        return "Author must be 3-20 characters, only lowercase letters, numbers and underscores.";
    }
}
