package com.normies.book.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.normies.book.network.CommandCatalogPayload;
import com.normies.book.permission.PermissionService;
import com.normies.book.service.CommandCatalog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BooknCommand {
    private BooknCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bookn")
                .requires(PermissionService::canUse)
                .executes(ctx -> openGui(ctx.getSource()))
        );
    }

    private static int openGui(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var catalog = CommandCatalog.build(player.getServer());
        PacketDistributor.sendToPlayer(player, new CommandCatalogPayload(catalog));
        return 1;
    }
}
