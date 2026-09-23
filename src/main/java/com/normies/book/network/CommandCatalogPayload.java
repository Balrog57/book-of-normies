package com.normies.book.network;

import com.normies.book.BookOfNormiesMod;
import com.normies.book.service.CommandCatalog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record CommandCatalogPayload(List<CommandCatalog.ModSection> mods) implements CustomPacketPayload {
    public static final Type<CommandCatalogPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BookOfNormiesMod.MOD_ID, "catalog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CommandCatalogPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, CommandCatalog.ModSection.STREAM_CODEC),
                    CommandCatalogPayload::mods,
                    CommandCatalogPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
