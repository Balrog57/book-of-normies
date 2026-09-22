package com.normies.book.service;

import com.mojang.brigadier.tree.CommandNode;
import com.normies.book.config.ModConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BookGenerator {
    private static final int COLOR_BORDEAUX = 0x4A0E17;
    private static final int COLOR_INK = 0x1A1A1A;
    private static final int COLOR_SLATE = 0x705A65;
    private static final int COLOR_GOLD = 0xB59410;

    private BookGenerator() {}

    public static ItemStack createBook(
            ServerPlayer player,
            Map<String, List<CommandNode<CommandSourceStack>>> modCommands,
            boolean confirmPage) {
        PlayerBookSession session = PlayerBookSession.of(player.getUUID());
        List<Component> pages = new ArrayList<>();

        if (confirmPage && session.getPendingCommand() != null) {
            pages.add(createConfirmPage(session.getPendingCommand()));
        } else {
            for (Map.Entry<String, List<CommandNode<CommandSourceStack>>> entry : modCommands.entrySet()) {
                String modId = entry.getKey();
                List<CommandNode<CommandSourceStack>> commands = entry.getValue();
                int perPage = ModConfig.get().commands_per_page;
                for (int i = 0; i < commands.size(); i += perPage) {
                    int end = Math.min(i + perPage, commands.size());
                    pages.add(createModPage(modId, commands.subList(i, end), session));
                }
            }
            if (pages.isEmpty()) {
                pages.add(Component.literal("❖ Aucune commande détectée.")
                        .withStyle(style -> style.withColor(COLOR_BORDEAUX)));
            }
        }

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "The Book of Normies");
        tag.putString("author", "Famille Addams");
        tag.putBoolean("resolved", true);

        ListTag pageTags = new ListTag();
        for (Component page : pages) {
            pageTags.add(StringTag.valueOf(Component.Serializer.toJson(page)));
        }
        tag.put("pages", pageTags);
        return book;
    }

    private static Component createModPage(
            String modId,
            List<CommandNode<CommandSourceStack>> nodes,
            PlayerBookSession session) {
        boolean minecraft = "minecraft".equals(modId);
        MutableComponent page = Component.empty();

        if (minecraft) {
            page.append(Component.literal("❖ THE BOOK ❖\n")
                    .withStyle(Style.EMPTY.withColor(COLOR_BORDEAUX).withBold(true)));
            page.append(Component.literal("  OF NORMIES\n")
                    .withStyle(Style.EMPTY.withColor(COLOR_INK).withBold(true)));
            page.append(Component.literal("— Grimoire Officiel —\n")
                    .withStyle(Style.EMPTY.withColor(COLOR_SLATE).withItalic(true)));
        } else {
            page.append(Component.literal("✦ " + CommandClassifier.displayName(modId) + " ✦\n")
                    .withStyle(Style.EMPTY.withColor(COLOR_BORDEAUX).withBold(true)));
        }

        page.append(Component.literal("──────────────\n")
                .withStyle(Style.EMPTY.withColor(COLOR_SLATE)));

        for (CommandNode<CommandSourceStack> node : nodes) {
            page.append(buildCommandEntry(node, session));
            page.append(Component.literal("\n"));
        }

        page.append(Component.literal("\n        ⚜ ❖ ⚜")
                .withStyle(Style.EMPTY.withColor(COLOR_GOLD)));
        return page;
    }

    private static MutableComponent buildCommandEntry(
            CommandNode<CommandSourceStack> node,
            PlayerBookSession session) {
        String name = node.getName();
        CommandClassifier.ParamInfo info = CommandClassifier.classify(node);

        MutableComponent label = Component.literal("✦ /" + name)
                .withStyle(Style.EMPTY.withColor(COLOR_INK));

        return switch (info.kind()) {
            case NONE -> label.withStyle(style -> style
                    .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Invoquer : /" + name)
                                    .withStyle(s -> s.withColor(COLOR_BORDEAUX))))
                    .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/bookn confirm_prepare " + name)));

            case CHOICE -> {
                String selected = session.getSelectedChoice(name, info.choices());
                if (selected.isEmpty() && !info.choices().isEmpty()) {
                    selected = info.choices().get(0);
                }
                MutableComponent line = Component.literal("✦ /" + name + " ")
                        .withStyle(Style.EMPTY.withColor(COLOR_INK));
                MutableComponent cycle = Component.literal("[" + selected + " ⟳]")
                        .withStyle(Style.EMPTY
                                .withColor(COLOR_GOLD)
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Cliquer pour changer l'option")
                                                .withStyle(s -> s.withColor(COLOR_SLATE))))
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/bookn cycle " + name)));
                MutableComponent run = Component.literal(" ✔")
                        .withStyle(Style.EMPTY
                                .withColor(COLOR_BORDEAUX)
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Confirmer /" + name + " " + selected)))
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/bookn confirm_prepare " + name + " " + selected)));
                yield line.append(cycle).append(run);
            }

            case FREEFORM -> {
                String placeholder = "[" + info.name() + "]";
                MutableComponent line = Component.literal("✦ /" + name + " ")
                        .withStyle(Style.EMPTY.withColor(COLOR_INK));
                MutableComponent free = Component.literal(placeholder)
                        .withStyle(Style.EMPTY
                                .withColor(COLOR_SLATE)
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Saisie libre — préremplit le tchat.\nConfirmation manuelle requise.")
                                                .withStyle(s -> s.withColor(COLOR_GOLD))))
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.SUGGEST_COMMAND,
                                        "/" + name + " ")));
                yield line.append(free);
            }
        };
    }

    private static Component createConfirmPage(String command) {
        String cmd = command.startsWith("/") ? command : "/" + command;
        MutableComponent page = Component.empty();
        page.append(Component.literal("❖ CONFIRMATION ❖\n\n")
                .withStyle(Style.EMPTY.withColor(COLOR_BORDEAUX).withBold(true)));
        page.append(Component.literal("Rituel à invoquer :\n")
                .withStyle(Style.EMPTY.withColor(COLOR_SLATE).withItalic(true)));
        page.append(Component.literal(cmd + "\n\n")
                .withStyle(Style.EMPTY.withColor(COLOR_INK)));

        page.append(Component.literal("[ ✔ Confirmer ]")
                .withStyle(Style.EMPTY
                        .withColor(COLOR_GOLD)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Exécuter " + cmd)))
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/bookn confirm_run"))));
        page.append(Component.literal("\n\n"));
        page.append(Component.literal("[ ✖ Annuler ]")
                .withStyle(Style.EMPTY
                        .withColor(COLOR_BORDEAUX)
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Retour au grimoire")))
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/bookn cancel"))));
        page.append(Component.literal("\n\n        ⚜ ❖ ⚜")
                .withStyle(Style.EMPTY.withColor(COLOR_GOLD)));
        return page;
    }
}
