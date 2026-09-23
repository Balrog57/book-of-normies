package com.normies.book.service;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.normies.book.BookOfNormiesMod;
import com.normies.book.config.ModConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Scans Brigadier roots and attributes each command to the mod that owns its executor class.
 * Prefers jar/resource ownership over fragile package-name heuristics.
 */
public final class CommandScanner {
    private CommandScanner() {}

    public static Map<String, List<CommandNode<CommandSourceStack>>> scan(MinecraftServer server) {
        Map<String, List<CommandNode<CommandSourceStack>>> raw = new HashMap<>();

        for (CommandNode<CommandSourceStack> node : server.getCommands().getDispatcher().getRoot().getChildren()) {
            String name = node.getName();
            if (BookOfNormiesMod.MOD_ID.equals(name) || "bookn".equalsIgnoreCase(name)) {
                continue;
            }
            String modId = resolveModId(node, name);
            raw.computeIfAbsent(modId, k -> new ArrayList<>()).add(node);
        }

        BookOfNormiesMod.LOGGER.info("Book of Normies catalog: {} command groups, {} commands total",
                raw.size(),
                raw.values().stream().mapToInt(List::size).sum());
        return sort(raw);
    }

    private static String resolveModId(CommandNode<CommandSourceStack> node, String name) {
        if (name.contains(":")) {
            String ns = name.substring(0, name.indexOf(':')).toLowerCase(Locale.ROOT);
            if (ModList.get().isLoaded(ns) || "minecraft".equals(ns)) {
                return ns;
            }
        }

        Class<?> owner = findOwningClass(node, Collections.newSetFromMap(new IdentityHashMap<>()));
        if (owner != null) {
            Optional<String> fromJar = modIdFromClassResource(owner);
            if (fromJar.isPresent()) {
                return fromJar.get();
            }
            Optional<String> fromPackage = modIdFromPackage(owner.getName());
            if (fromPackage.isPresent()) {
                return fromPackage.get();
            }
        }

        String lower = name.toLowerCase(Locale.ROOT);
        if (ModList.get().isLoaded(lower)) {
            return lower;
        }
        for (IModInfo mod : ModList.get().getMods()) {
            String id = mod.getModId().toLowerCase(Locale.ROOT);
            if (lower.startsWith(id + "_") || lower.startsWith(id + "-")) {
                return id;
            }
        }

        return "unknown";
    }

    private static Class<?> findOwningClass(CommandNode<?> node, Set<CommandNode<?>> visited) {
        if (node == null || !visited.add(node)) {
            return null;
        }

        if (node.getCommand() != null) {
            Class<?> clazz = unwrap(node.getCommand().getClass());
            if (!isInfrastructure(clazz)) {
                return clazz;
            }
        }

        if (node instanceof ArgumentCommandNode<?, ?> arg && arg.getCustomSuggestions() != null) {
            Class<?> clazz = unwrap(arg.getCustomSuggestions().getClass());
            if (!isInfrastructure(clazz)) {
                return clazz;
            }
        }

        if (node.getRedirect() != null) {
            Class<?> redirected = findOwningClass(node.getRedirect(), visited);
            if (redirected != null) {
                return redirected;
            }
        }

        for (CommandNode<?> child : node.getChildren()) {
            Class<?> childOwner = findOwningClass(child, visited);
            if (childOwner != null) {
                return childOwner;
            }
        }
        return null;
    }

    private static Class<?> unwrap(Class<?> clazz) {
        while (clazz != null && (clazz.isAnonymousClass() || clazz.isSynthetic() || clazz.getName().contains("$$"))) {
            Class<?> enclosing = clazz.getEnclosingClass();
            if (enclosing == null) {
                break;
            }
            clazz = enclosing;
        }
        return clazz;
    }

    private static boolean isInfrastructure(Class<?> clazz) {
        if (clazz == null) {
            return true;
        }
        String n = clazz.getName();
        return n.startsWith("com.mojang.brigadier.")
                || n.startsWith("java.")
                || n.startsWith("jdk.");
    }

    private static Optional<String> modIdFromClassResource(Class<?> clazz) {
        String resource = clazz.getName().replace('.', '/') + ".class";

        for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
            try {
                Path path = fileInfo.getFile().findResource(resource);
                if (path != null && Files.exists(path)) {
                    return primaryModId(fileInfo);
                }
            } catch (Exception ignored) {
                // try next
            }
        }

        try {
            CodeSource source = clazz.getProtectionDomain().getCodeSource();
            if (source == null) {
                return Optional.empty();
            }
            URL location = source.getLocation();
            if (location == null) {
                return Optional.empty();
            }
            Path jarPath = Path.of(location.toURI()).toAbsolutePath().normalize();
            for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
                Path modPath = fileInfo.getFile().getFilePath().toAbsolutePath().normalize();
                if (jarPath.equals(modPath) || jarPath.startsWith(modPath) || modPath.startsWith(jarPath)) {
                    return primaryModId(fileInfo);
                }
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<String> primaryModId(IModFileInfo fileInfo) {
        List<IModInfo> mods = fileInfo.getMods();
        if (mods == null || mods.isEmpty()) {
            return Optional.empty();
        }
        // Prefer a non-library-looking id when several mods share a jar
        for (IModInfo mod : mods) {
            String id = mod.getModId().toLowerCase(Locale.ROOT);
            if (!id.equals("minecraft") && !id.equals("neoforge") && !id.equals("forge")) {
                return Optional.of(id);
            }
        }
        return Optional.of(mods.getFirst().getModId().toLowerCase(Locale.ROOT));
    }

    private static Optional<String> modIdFromPackage(String className) {
        if (className.startsWith("net.minecraft.")) {
            return Optional.of("minecraft");
        }
        if (className.startsWith("net.neoforged.")) {
            return Optional.of("neoforge");
        }

        String classLower = className.toLowerCase(Locale.ROOT);
        for (IModInfo mod : ModList.get().getMods()) {
            String id = mod.getModId().toLowerCase(Locale.ROOT);
            if (BookOfNormiesMod.MOD_ID.equals(id)) {
                continue;
            }
            String underscored = id.replace('-', '_');
            if (classLower.contains("." + id + ".")
                    || classLower.contains("." + underscored + ".")
                    || classLower.startsWith(id + ".")
                    || classLower.startsWith(underscored + ".")) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    private static Map<String, List<CommandNode<CommandSourceStack>>> sort(
            Map<String, List<CommandNode<CommandSourceStack>>> raw) {
        Map<String, List<CommandNode<CommandSourceStack>>> sorted = new LinkedHashMap<>();

        List<CommandNode<CommandSourceStack>> minecraft = raw.remove("minecraft");
        if (minecraft != null && !minecraft.isEmpty()) {
            minecraft.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            sorted.put("minecraft", minecraft);
        }

        for (String priority : ModConfig.get().priority_mods) {
            if (priority == null) {
                continue;
            }
            String key = priority.toLowerCase(Locale.ROOT);
            if ("minecraft".equals(key)) {
                continue;
            }
            List<CommandNode<CommandSourceStack>> list = raw.remove(key);
            if (list != null && !list.isEmpty()) {
                list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                sorted.put(key, list);
            }
        }

        List<String> remaining = new ArrayList<>(raw.keySet());
        Collections.sort(remaining);
        for (String mod : remaining) {
            List<CommandNode<CommandSourceStack>> list = raw.get(mod);
            if (list == null || list.isEmpty()) {
                continue;
            }
            list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            sorted.put(mod, list);
        }
        return sorted;
    }
}
