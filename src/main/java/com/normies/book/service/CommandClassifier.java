package com.normies.book.service;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommandClassifier {
    public enum ParamKind {
        NONE,
        CHOICE,
        FREEFORM
    }

    public record ParamInfo(ParamKind kind, String name, List<String> choices) {
        public static ParamInfo none() {
            return new ParamInfo(ParamKind.NONE, "", List.of());
        }
    }

    private CommandClassifier() {}

    public static boolean hasParameters(CommandNode<CommandSourceStack> root) {
        return !root.getChildren().isEmpty();
    }

    /**
     * Inspect first-level children to decide how the book should present parameters.
     */
    public static ParamInfo classify(CommandNode<CommandSourceStack> root) {
        if (root.getChildren().isEmpty()) {
            return ParamInfo.none();
        }

        List<String> literals = new ArrayList<>();
        List<String> freeNames = new ArrayList<>();

        for (CommandNode<CommandSourceStack> child : root.getChildren()) {
            if (child instanceof LiteralCommandNode<?>) {
                literals.add(child.getName());
            } else if (child instanceof ArgumentCommandNode<?, ?> arg) {
                freeNames.add(arg.getName());
            }
        }

        if (!literals.isEmpty() && freeNames.isEmpty()) {
            return new ParamInfo(ParamKind.CHOICE, "option", List.copyOf(literals));
        }
        if (!freeNames.isEmpty()) {
            String name = freeNames.getFirst();
            return new ParamInfo(ParamKind.FREEFORM, name, List.of());
        }
        if (!literals.isEmpty()) {
            return new ParamInfo(ParamKind.CHOICE, "option", List.copyOf(literals));
        }
        return ParamInfo.none();
    }

    public static String displayName(String modId) {
        if ("minecraft".equals(modId)) {
            return "THE BOOK OF NORMIES";
        }
        return modId.toUpperCase(Locale.ROOT);
    }
}
