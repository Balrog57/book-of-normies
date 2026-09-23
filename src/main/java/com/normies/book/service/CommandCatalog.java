package com.normies.book.service;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Serializable command catalog sent to the client GUI. */
public final class CommandCatalog {
    private CommandCatalog() {}

    public record CommandEntry(String name, byte kind, String argName, List<String> choices) {
        public static final byte KIND_NONE = 0;
        public static final byte KIND_CHOICE = 1;
        public static final byte KIND_FREEFORM = 2;

        public static final StreamCodec<RegistryFriendlyByteBuf, CommandEntry> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, CommandEntry::name,
                        ByteBufCodecs.BYTE, CommandEntry::kind,
                        ByteBufCodecs.STRING_UTF8, CommandEntry::argName,
                        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), CommandEntry::choices,
                        CommandEntry::new
                );

        public static CommandEntry fromNode(CommandNode<CommandSourceStack> node) {
            CommandClassifier.ParamInfo info = CommandClassifier.classify(node);
            byte kind = switch (info.kind()) {
                case NONE -> KIND_NONE;
                case CHOICE -> KIND_CHOICE;
                case FREEFORM -> KIND_FREEFORM;
            };
            return new CommandEntry(node.getName(), kind, info.name() == null ? "" : info.name(),
                    info.choices() == null ? List.of() : info.choices());
        }
    }

    public record ModSection(String modId, String displayName, List<CommandEntry> commands) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ModSection> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, ModSection::modId,
                        ByteBufCodecs.STRING_UTF8, ModSection::displayName,
                        ByteBufCodecs.collection(ArrayList::new, CommandEntry.STREAM_CODEC), ModSection::commands,
                        ModSection::new
                );
    }

    public static List<ModSection> build(MinecraftServer server) {
        Map<String, List<CommandNode<CommandSourceStack>>> scanned = CommandScanner.scan(server);
        List<ModSection> sections = new ArrayList<>();
        for (Map.Entry<String, List<CommandNode<CommandSourceStack>>> entry : scanned.entrySet()) {
            List<CommandEntry> commands = new ArrayList<>();
            for (CommandNode<CommandSourceStack> node : entry.getValue()) {
                commands.add(CommandEntry.fromNode(node));
            }
            if (!commands.isEmpty()) {
                sections.add(new ModSection(entry.getKey(), resolveDisplayName(entry.getKey()), commands));
            }
        }
        return sections;
    }

    private static String resolveDisplayName(String modId) {
        if ("minecraft".equals(modId)) {
            return "Minecraft";
        }
        if ("unknown".equals(modId)) {
            return "Autres";
        }
        Optional<? extends net.neoforged.fml.ModContainer> container = ModList.get().getModContainerById(modId);
        if (container.isPresent()) {
            String name = container.get().getModInfo().getDisplayName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return modId;
    }

    public static String headerLabel(ModSection section) {
        if ("minecraft".equals(section.modId())) {
            return "THE BOOK OF NORMIES";
        }
        return section.displayName().toUpperCase(Locale.ROOT);
    }
}
