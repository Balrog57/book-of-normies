package com.normies.book.util;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Opens a written book UI without permanently putting an item in the player's inventory.
 */
public final class BookOpener {
    private BookOpener() {}

    public static void openVirtualBook(ServerPlayer player, ItemStack book) {
        int slot = player.getInventory().selected + 36;
        int stateId = player.containerMenu.getStateId();

        player.connection.send(new ClientboundContainerSetSlotPacket(0, stateId, slot, book));
        player.connection.send(new ClientboundOpenBookPacket(InteractionHand.MAIN_HAND));
        player.connection.send(new ClientboundContainerSetSlotPacket(
                0, player.containerMenu.getStateId(), slot, player.getMainHandItem()));
    }
}
