package com.normies.book;

import com.mojang.logging.LogUtils;
import com.normies.book.command.BooknCommand;
import com.normies.book.config.ModConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(BookOfNormiesMod.MOD_ID)
public class BookOfNormiesMod {
    public static final String MOD_ID = "book_of_normies";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BookOfNormiesMod() {
        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            LOGGER.info("The Book of Normies skips client load (server-side only).");
            return;
        }
        ModConfig.load();
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        LOGGER.info("The Book of Normies loaded (Forge 1.20.1 server-side).");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        BooknCommand.register(event.getDispatcher());
    }
}
