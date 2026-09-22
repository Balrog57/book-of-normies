package com.normies.book.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.normies.book.permission.PermissionService;
import com.normies.book.service.BookGenerator;
import com.normies.book.service.CommandClassifier;
import com.normies.book.service.CommandScanner;
import com.normies.book.service.PlayerBookSession;
import com.normies.book.util.BookOpener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public final class BooknCommand {
    private BooknCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bookn")
                .requires(PermissionService::canUse)
                .executes(ctx -> openBook(ctx.getSource(), false))
                .then(Commands.literal("cycle")
                        .then(Commands.argument("command", StringArgumentType.word())
                                .executes(ctx -> cycle(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "command")))))
                .then(Commands.literal("confirm_prepare")
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> prepareConfirm(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "command")))))
                .then(Commands.literal("confirm_run")
                        .executes(ctx -> runConfirm(ctx.getSource())))
                .then(Commands.literal("cancel")
                        .executes(ctx -> cancel(ctx.getSource())))
        );
    }

    private static int openBook(CommandSourceStack source, boolean confirmPage) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Map<String, List<CommandNode<CommandSourceStack>>> commands =
                CommandScanner.scan(player.getServer());
        ItemStack book = BookGenerator.createBook(player, commands, confirmPage);
        BookOpener.openVirtualBook(player, book);
        return 1;
    }

    private static int cycle(CommandSourceStack source, String commandName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerBookSession session = PlayerBookSession.of(player.getUUID());

        CommandNode<CommandSourceStack> node = findRoot(player, commandName);
        if (node != null) {
            CommandClassifier.ParamInfo info = CommandClassifier.classify(node);
            if (info.kind() == CommandClassifier.ParamKind.CHOICE && !info.choices().isEmpty()) {
                session.cycleChoice(commandName, info.choices().size());
            }
        }

        return openBook(source, false);
    }

    private static int prepareConfirm(CommandSourceStack source, String command) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerBookSession session = PlayerBookSession.of(player.getUUID());
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        session.setPendingCommand(normalized);
        return openBook(source, true);
    }

    private static int runConfirm(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        PlayerBookSession session = PlayerBookSession.of(player.getUUID());
        String pending = session.getPendingCommand();
        session.clearPending();
        if (pending == null || pending.isBlank()) {
            source.sendFailure(Component.literal("Aucune commande en attente."));
            return 0;
        }
        String cmd = pending.startsWith("/") ? pending.substring(1) : pending;
        player.getServer().getCommands().performPrefixedCommand(source, cmd);
        return 1;
    }

    private static int cancel(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerBookSession.of(player.getUUID()).clearPending();
        return openBook(source, false);
    }

    private static CommandNode<CommandSourceStack> findRoot(ServerPlayer player, String name) {
        for (CommandNode<CommandSourceStack> node
                : player.getServer().getCommands().getDispatcher().getRoot().getChildren()) {
            if (node.getName().equalsIgnoreCase(name)) {
                return node;
            }
        }
        return null;
    }
}
