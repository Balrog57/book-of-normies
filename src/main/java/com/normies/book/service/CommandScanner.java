package com.normies.book.service;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.normies.book.BookOfNormiesMod;
import com.normies.book.config.ModConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class CommandScanner {
    private CommandScanner() {}

    public static Map<String, List<CommandNode<CommandSourceStack>>> scan(MinecraftServer server) {
        Map<String, List<CommandNode<CommandSourceStack>>> raw = new HashMap<>();
        Set<String> knownMods = ModList.get().getMods().stream()
                .map(IModInfo::getModId)
                .map(id -> id.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        for (CommandNode<CommandSourceStack> node : server.getCommands().getDispatcher().getRoot().getChildren()) {
            String name = node.getName();
            if (BookOfNormiesMod.MOD_ID.equals(name) || "bookn".equals(name)) {
                continue;
            }
            String modId = resolveModId(node, name, knownMods);
            raw.computeIfAbsent(modId, k -> new ArrayList<>()).add(node);
        }

        return sort(raw);
    }

    private static String resolveModId(CommandNode<CommandSourceStack> node, String name, Set<String> knownMods) {
        if (name.contains(":")) {
            return name.substring(0, name.indexOf(':')).toLowerCase(Locale.ROOT);
        }

        Optional<String> fromClass = modIdFromNodeClass(node);
        if (fromClass.isPresent()) {
            return fromClass.get();
        }

        String lower = name.toLowerCase(Locale.ROOT);
        if (knownMods.contains(lower)) {
            return lower;
        }
        for (String modId : knownMods) {
            if (lower.startsWith(modId + "_") || lower.startsWith(modId + "-")) {
                return modId;
            }
        }

        if (isLikelyVanilla(node)) {
            return "minecraft";
        }
        return "unknown";
    }

    private static Optional<String> modIdFromNodeClass(CommandNode<CommandSourceStack> node) {
        Class<?> clazz = null;
        if (node.getCommand() != null) {
            clazz = node.getCommand().getClass();
        } else if (node instanceof ArgumentCommandNode<?, ?> arg && arg.getCustomSuggestions() != null) {
            clazz = arg.getCustomSuggestions().getClass();
        }
        if (clazz == null) {
            clazz = node.getClass();
        }
        while (clazz != null && (clazz.isAnonymousClass() || clazz.isSynthetic())) {
            clazz = clazz.getEnclosingClass();
        }
        if (clazz == null) {
            return Optional.empty();
        }

        String className = clazz.getName();
        if (className.startsWith("net.minecraft.") || className.startsWith("com.mojang.")) {
            return Optional.of("minecraft");
        }
        if (className.startsWith("net.neoforged.")) {
            return Optional.of("neoforge");
        }

        String classLower = className.toLowerCase(Locale.ROOT);
        for (IModInfo mod : ModList.get().getMods()) {
            String modId = mod.getModId();
            if (BookOfNormiesMod.MOD_ID.equals(modId)) {
                continue;
            }
            String idLower = modId.toLowerCase(Locale.ROOT);
            String needle = "." + idLower.replace('-', '_') + ".";
            String needle2 = "." + idLower.replace('-', '.') + ".";
            if (classLower.contains(needle) || classLower.contains(needle2)
                    || classLower.contains("." + idLower + ".")) {
                return Optional.of(idLower);
            }
        }
        return Optional.empty();
    }

    private static boolean isLikelyVanilla(CommandNode<CommandSourceStack> node) {
        if (!(node instanceof LiteralCommandNode<?>)) {
            return false;
        }
        Class<?> c = node.getCommand() != null ? node.getCommand().getClass() : node.getClass();
        String n = c.getName();
        return n.startsWith("net.minecraft.") || n.startsWith("com.mojang.");
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
