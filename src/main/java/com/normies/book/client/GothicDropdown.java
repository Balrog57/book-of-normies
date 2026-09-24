package com.normies.book.client;

import com.normies.book.service.CommandCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gothic parchment dropdown for selecting a command ritual.
 */
public class GothicDropdown extends AbstractWidget {
    private static final int GOLD = 0xFFC4A35A;
    private static final int GOLD_DIM = 0xFF8A7340;
    private static final int INK = 0xFF2A1A18;
    private static final int BORDEAUX = 0xFF5C101C;
    private static final int PARCHMENT = 0xFFE8D9C0;
    private static final int PARCHMENT_DARK = 0xFFD4C2A6;
    private static final int HOVER = 0xFF4A1520;
    private static final int ROW_H = 16;
    private static final int MAX_VISIBLE = 8;

    private List<CommandCatalog.CommandEntry> entries = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean expanded;
    private int scroll;
    private int hoverIndex = -1;
    private Consumer<CommandCatalog.CommandEntry> onSelect = e -> {};
    private boolean locked;

    public GothicDropdown(int x, int y, int width) {
        super(x, y, width, 20, Component.literal("Rituel"));
    }

    public void setEntries(List<CommandCatalog.CommandEntry> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        this.selectedIndex = this.entries.isEmpty() ? -1 : 0;
        this.expanded = false;
        this.scroll = 0;
        if (this.selectedIndex >= 0) {
            this.onSelect.accept(this.entries.get(this.selectedIndex));
        } else {
            this.onSelect.accept(null);
        }
    }

    public void setOnSelect(Consumer<CommandCatalog.CommandEntry> onSelect) {
        this.onSelect = onSelect == null ? e -> {} : onSelect;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        if (locked) {
            this.expanded = false;
        }
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public CommandCatalog.CommandEntry getSelected() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) {
            return null;
        }
        return entries.get(selectedIndex);
    }

    private int panelHeight() {
        int visible = Math.min(MAX_VISIBLE, entries.size());
        return visible * ROW_H + 4;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Closed bar
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, PARCHMENT_DARK);
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, PARCHMENT);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, GOLD_DIM);
        graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, GOLD_DIM);

        String label = selectedIndex >= 0 && selectedIndex < entries.size()
                ? "/" + entries.get(selectedIndex).name()
                : "— Choisir un rituel —";
        var font = Minecraft.getInstance().font;
        if (font.width(label) > this.width - 28) {
            label = font.plainSubstrByWidth(label, this.width - 32) + "…";
        }
        graphics.drawString(font, label, this.getX() + 8, this.getY() + 6, locked ? 0xFF887868 : INK, false);
        graphics.drawString(font, expanded ? "▴" : "▾", this.getX() + this.width - 14, this.getY() + 6, GOLD, false);

        if (!expanded || entries.isEmpty()) {
            return;
        }

        int panelTop = this.getY() + this.height + 2;
        int panelH = panelHeight();
        int panelBottom = panelTop + panelH;

        graphics.fill(this.getX(), panelTop, this.getX() + this.width, panelBottom, BORDEAUX);
        graphics.fill(this.getX() + 1, panelTop + 1, this.getX() + this.width - 1, panelBottom - 1, PARCHMENT);

        graphics.enableScissor(this.getX() + 1, panelTop + 1, this.getX() + this.width - 1, panelBottom - 1);

        int maxScroll = Math.max(0, entries.size() - MAX_VISIBLE);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        hoverIndex = -1;

        for (int i = 0; i < MAX_VISIBLE; i++) {
            int idx = i + scroll;
            if (idx >= entries.size()) {
                break;
            }
            int rowY = panelTop + 2 + i * ROW_H;
            boolean hover = mouseX >= this.getX() && mouseX < this.getX() + this.width
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            boolean selected = idx == selectedIndex;
            if (hover) {
                hoverIndex = idx;
                graphics.fill(this.getX() + 2, rowY, this.getX() + this.width - 2, rowY + ROW_H, HOVER);
            } else if (selected) {
                graphics.fill(this.getX() + 2, rowY, this.getX() + this.width - 2, rowY + ROW_H, 0x334A0E17);
            }
            int color = hover || selected ? GOLD : INK;
            String row = "/" + entries.get(idx).name();
            if (font.width(row) > this.width - 16) {
                row = font.plainSubstrByWidth(row, this.width - 20) + "…";
            }
            graphics.drawString(font, row, this.getX() + 8, rowY + 4, color, false);
        }

        graphics.disableScissor();
    }

    /** Full vertical span including open panel, for click-outside detection. */
    public boolean isMouseOverExpanded(double mouseX, double mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            return true;
        }
        if (!expanded) {
            return false;
        }
        int panelTop = this.getY() + this.height + 2;
        return mouseX >= this.getX() && mouseX < this.getX() + this.width
                && mouseY >= panelTop && mouseY < panelTop + panelHeight();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || locked || button != 0) {
            return false;
        }

        if (isMouseOver(mouseX, mouseY)) {
            this.expanded = !this.expanded;
            return true;
        }

        if (expanded) {
            int panelTop = this.getY() + this.height + 2;
            if (mouseX >= this.getX() && mouseX < this.getX() + this.width
                    && mouseY >= panelTop && mouseY < panelTop + panelHeight()) {
                int rel = (int) ((mouseY - panelTop - 2) / ROW_H);
                int idx = rel + scroll;
                if (idx >= 0 && idx < entries.size()) {
                    selectedIndex = idx;
                    expanded = false;
                    onSelect.accept(entries.get(idx));
                }
                return true;
            }
            // Click outside closes
            expanded = false;
            return false;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!expanded || !isMouseOverExpanded(mouseX, mouseY)) {
            return false;
        }
        int maxScroll = Math.max(0, entries.size() - MAX_VISIBLE);
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll);
        return true;
    }

    public void collapse() {
        this.expanded = false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
