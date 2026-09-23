package com.normies.book.network;

import com.normies.book.BookOfNormiesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RunCommandPayload(String command) implements CustomPacketPayload {
    public static final Type<RunCommandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BookOfNormiesMod.MOD_ID, "run"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RunCommandPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    RunCommandPayload::command,
                    RunCommandPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
