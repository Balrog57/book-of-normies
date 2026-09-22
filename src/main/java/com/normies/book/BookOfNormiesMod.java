package com.normies.book;

import com.normies.book.command.BooknCommand;
import com.normies.book.config.ModConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(value = BookOfNormiesMod.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class BookOfNormiesMod {
    public static final String MOD_ID = "book_of_normies";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BookOfNormiesMod(IEventBus modEventBus) {
        ModConfig.load();
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        LOGGER.info("The Book of Normies loaded (server-side).");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        BooknCommand.register(event.getDispatcher());
    }
}
