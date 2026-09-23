package com.normies.book.network;

import com.normies.book.client.BookOfNormiesClient;
import com.normies.book.permission.PermissionService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");

        registrar.playToClient(
                CommandCatalogPayload.TYPE,
                CommandCatalogPayload.STREAM_CODEC,
                ModNetwork::handleCatalogClient
        );

        registrar.playToServer(
                RunCommandPayload.TYPE,
                RunCommandPayload.STREAM_CODEC,
                ModNetwork::handleRunServer
        );
    }

    private static void handleCatalogClient(CommandCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                BookOfNormiesClient.openCatalog(payload);
            }
        });
    }

    private static void handleRunServer(RunCommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!PermissionService.canUse(player.createCommandSourceStack())) {
                player.sendSystemMessage(Component.literal("Permission refusée."));
                return;
            }
            String raw = payload.command() == null ? "" : payload.command().trim();
            if (raw.isEmpty()) {
                return;
            }
            if (raw.startsWith("/")) {
                raw = raw.substring(1);
            }
            // Block recursive bookn network abuse via crafted packets for internal-only paths
            if (raw.equalsIgnoreCase("bookn") || raw.toLowerCase().startsWith("bookn ")) {
                return;
            }
            player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), raw);
        });
    }
}
