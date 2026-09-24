package com.normies.book;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, BookOfNormiesMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TAB_TURN = SOUND_EVENTS.register(
            "tab_turn",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(BookOfNormiesMod.MOD_ID, "tab_turn"))
    );

    private ModSounds() {}
}
