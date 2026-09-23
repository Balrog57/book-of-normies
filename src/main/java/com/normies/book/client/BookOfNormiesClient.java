package com.normies.book.client;

import com.normies.book.network.CommandCatalogPayload;
import net.minecraft.client.Minecraft;

public final class BookOfNormiesClient {
    private BookOfNormiesClient() {}

    public static void openCatalog(CommandCatalogPayload payload) {
        Minecraft.getInstance().setScreen(new BookOfNormiesScreen(payload.mods()));
    }
}
