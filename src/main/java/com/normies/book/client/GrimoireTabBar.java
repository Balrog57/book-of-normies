package com.normies.book.client;

import com.normies.book.BookOfNormiesMod;
import com.normies.book.ModSounds;
import com.normies.book.service.CommandCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Horizontal fake grimoire tabs — one per mod — with scroll and page-turn sound.
 */
public class GrimoireTabBar extends AbstractWidget {
    private static final ResourceLocation TAB_TEX =
            ResourceLocation.fromNamespaceAndPath(BookOfNormiesMod.MOD_ID, "textures/gui/grimoire_tab.png");

    private static final int TAB_W = 88;
    private static final int TAB_H = 22;
    private static final int TAB_GAP = 4;
    private static final int GOLD = 0xFFC4A35A;
    private static final int INK = 0xFFEDE4E0;
    private static final int SLATE = 0xFFB8A8A0;

    private final List<CommandCatalog.ModSection> mods;
    private final IntConsumer onSelect;
    private int selectedIndex;
    private int scrollPx;
    private int hoveredIndex = -1;

    public GrimoireTabBar(int x, int y, int width, List<CommandCatalog.ModSection> mods,
                          int selectedIndex, IntConsumer onSelect) {
        super(x, y, width, TAB_H + 4, Component.literal("Tabs"));
        this.mods = mods;
        this.selectedIndex = Mth.clamp(selectedIndex, 0, Math.max(0, mods.size() - 1));
        this.onSelect = onSelect;
        ensureVisible(this.selectedIndex);
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = Mth.clamp(index, 0, Math.max(0, mods.size() - 1));
        ensureVisible(this.selectedIndex);
    }

    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    private int contentWidth() {
        if (mods.isEmpty()) {
            return 0;
        }
        return mods.size() * (TAB_W + TAB_GAP) - TAB_GAP;
    }

    private void ensureVisible(int index) {
        int maxScroll = Math.max(0, contentWidth() - this.width);
        int tabLeft = index * (TAB_W + TAB_GAP);
        int tabRight = tabLeft + TAB_W;
        if (tabLeft < scrollPx) {
            scrollPx = tabLeft;
        } else if (tabRight > scrollPx + this.width) {
            scrollPx = tabRight - this.width;
        }
        scrollPx = Mth.clamp(scrollPx, 0, maxScroll);
    }

    private int hitIndex(double mouseX) {
        double local = mouseX - this.getX() + scrollPx;
        if (local < 0) {
            return -1;
        }
        int idx = (int) (local / (TAB_W + TAB_GAP));
        int within = (int) (local % (TAB_W + TAB_GAP));
        if (idx < 0 || idx >= mods.size() || within > TAB_W) {
            return -1;
        }
        return idx;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredIndex = this.isHovered() ? hitIndex(mouseX) : -1;

        graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);

        for (int i = 0; i < mods.size(); i++) {
            int tx = this.getX() + i * (TAB_W + TAB_GAP) - scrollPx;
            if (tx + TAB_W < this.getX() || tx > this.getX() + this.width) {
                continue;
            }
            boolean selected = i == selectedIndex;
            boolean hovered = i == hoveredIndex;
            int ty = this.getY() + (selected ? 0 : 3);

            graphics.blit(TAB_TEX, tx, ty, 0, 0, TAB_W, TAB_H, 128, 32);

            if (selected) {
                graphics.fill(tx, ty + TAB_H - 2, tx + TAB_W, ty + TAB_H, GOLD);
            }

            String label = mods.get(i).displayName();
            if (Minecraft.getInstance().font.width(label) > TAB_W - 12) {
                label = Minecraft.getInstance().font.plainSubstrByWidth(label, TAB_W - 16) + "…";
            }
            int color = selected ? GOLD : (hovered ? INK : SLATE);
            graphics.drawCenteredString(Minecraft.getInstance().font, label,
                    tx + TAB_W / 2, ty + 7, color);
        }

        graphics.disableScissor();

        // Scroll cues
        if (scrollPx > 0) {
            graphics.drawString(Minecraft.getInstance().font, "‹", this.getX() - 8, this.getY() + 6, GOLD, false);
        }
        if (scrollPx < Math.max(0, contentWidth() - this.width)) {
            graphics.drawString(Minecraft.getInstance().font, "›", this.getX() + this.width + 2, this.getY() + 6, GOLD, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !this.isMouseOver(mouseX, mouseY) || button != 0) {
            return false;
        }
        int idx = hitIndex(mouseX);
        if (idx >= 0 && idx != selectedIndex) {
            this.selectedIndex = idx;
            ensureVisible(idx);
            playTabSound();
            this.onSelect.accept(idx);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        int maxScroll = Math.max(0, contentWidth() - this.width);
        scrollPx = Mth.clamp(scrollPx - (int) (scrollY * 24), 0, maxScroll);
        return true;
    }

    private void playTabSound() {
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        float pitch = 0.9f + (float) (Math.random() * 0.2);
        sounds.play(SimpleSoundInstance.forUI(ModSounds.TAB_TURN.get(), pitch));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
