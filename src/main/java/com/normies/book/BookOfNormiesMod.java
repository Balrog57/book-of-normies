package com.normies.book;

import com.mojang.logging.LogUtils;
import com.normies.book.command.BooknCommand;
import com.normies.book.config.ModConfig;
import com.normies.book.network.ModNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(BookOfNormiesMod.MOD_ID)
public class BookOfNormiesMod {
    public static final String MOD_ID = "book_of_normies";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BookOfNormiesMod(IEventBus modEventBus) {
        ModConfig.load();
        ModSounds.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        LOGGER.info("The Book of Normies loaded (NeoForge GUI).");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        BooknCommand.register(event.getDispatcher());
    }
}
