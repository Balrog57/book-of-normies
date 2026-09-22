package com.normies.book.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.normies.book.BookOfNormiesMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("book_of_normies.json");
    private static ModConfig INSTANCE;

    public int permission_level = 2;
    public List<String> allowed_roles = new ArrayList<>(List.of("admin", "moderator"));
    public List<String> priority_mods = new ArrayList<>(List.of("minecraft", "worldedit", "luckperms"));
    public int commands_per_page = 7;

    private ModConfig() {}

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new ModConfig();
                }
                normalize(INSTANCE);
            } catch (Exception e) {
                BookOfNormiesMod.LOGGER.warn("Failed to read config, using defaults: {}", e.toString());
                INSTANCE = new ModConfig();
                save();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(get(), writer);
            }
        } catch (IOException e) {
            BookOfNormiesMod.LOGGER.warn("Failed to write config: {}", e.toString());
        }
    }

    private static void normalize(ModConfig config) {
        if (config.allowed_roles == null) {
            config.allowed_roles = new ArrayList<>();
        }
        if (config.priority_mods == null) {
            config.priority_mods = new ArrayList<>(List.of("minecraft"));
        }
        if (config.commands_per_page < 3) {
            config.commands_per_page = 3;
        }
        if (config.commands_per_page > 12) {
            config.commands_per_page = 12;
        }
    }
}
