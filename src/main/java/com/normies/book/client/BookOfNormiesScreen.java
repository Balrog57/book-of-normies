package com.normies.book.client;

import com.normies.book.network.RunCommandPayload;
import com.normies.book.service.CommandCatalog;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Victorian / Addams Family command grimoire UI.
 */
public class BookOfNormiesScreen extends Screen {
    private static final int BG = 0xFF0A0708;
    private static final int PANEL = 0xFF161012;
    private static final int PANEL_INNER = 0xFF1E1518;
    private static final int BORDEAUX = 0xFF5C101C;
    private static final int BORDEAUX_DEEP = 0xFF3A0A12;
    private static final int GOLD = 0xFFC4A35A;
    private static final int GOLD_DIM = 0xFF8A7340;
    private static final int SLATE = 0xFF9A8A90;
    private static final int INK = 0xFFEDE4E0;
    private static final int ROW = 0xFF24181C;
    private static final int ROW_HOVER = 0xFF3A2228;
    private static final int ROW_SEL = 0xFF4A1520;

    private final List<CommandCatalog.ModSection> mods;

    private ModList modList;
    private CommandList commandList;
    private EditBox freeInput;
    private Button cycleButton;
    private Button confirmButton;
    private Button closeButton;

    private CommandCatalog.ModSection selectedMod;
    private CommandCatalog.CommandEntry selectedCommand;
    private int choiceIndex;
    private String pendingPreview = "";
    private boolean confirmMode;

    public BookOfNormiesScreen(List<CommandCatalog.ModSection> mods) {
        super(Component.literal("The Book of Normies"));
        this.mods = mods == null ? List.of() : mods;
        if (!this.mods.isEmpty()) {
            this.selectedMod = this.mods.getFirst();
        }
    }

    @Override
    protected void init() {
        int outer = 16;
        int headerH = 52;
        int footerH = 64;
        int gap = 10;
        int leftW = Math.max(150, this.width / 3);

        int listTop = outer + headerH;
        int listH = Math.max(80, this.height - outer - footerH - listTop);
        int leftX = outer + 8;
        int rightX = leftX + leftW + gap;
        int rightW = this.width - rightX - outer - 8;

        this.modList = new ModList(leftW, listH);
        this.modList.setX(leftX);
        this.modList.setY(listTop);
        this.addRenderableWidget(this.modList);

        this.commandList = new CommandList(rightW, listH);
        this.commandList.setX(rightX);
        this.commandList.setY(listTop);
        this.addRenderableWidget(this.commandList);

        int bottomY = this.height - outer - 36;
        this.freeInput = new EditBox(this.font, rightX, bottomY, Math.max(100, rightW - 230), 20,
                Component.literal("param"));
        this.freeInput.setMaxLength(256);
        this.freeInput.setTextColor(INK);
        this.freeInput.setTextColorUneditable(SLATE);
        this.freeInput.setVisible(false);
        this.addRenderableWidget(this.freeInput);

        this.cycleButton = Button.builder(Component.literal("⚜ ⟳ ⚜"), b -> cycleChoice())
                .bounds(rightX, bottomY, 100, 20)
                .build();
        this.cycleButton.visible = false;
        this.addRenderableWidget(this.cycleButton);

        this.confirmButton = Button.builder(Component.literal("✔ Invoquer"), b -> onConfirm())
                .bounds(this.width - outer - 210, bottomY, 100, 20)
                .build();
        this.addRenderableWidget(this.confirmButton);

        this.closeButton = Button.builder(Component.literal("✖ Clore"), b -> onClose())
                .bounds(this.width - outer - 100, bottomY, 90, 20)
                .build();
        this.addRenderableWidget(this.closeButton);

        refreshModEntries();
        refreshCommandEntries();
        updateParamWidgets();
    }

    private void refreshModEntries() {
        this.modList.reload(this.mods, this.selectedMod);
    }

    private void refreshCommandEntries() {
        this.selectedCommand = null;
        this.choiceIndex = 0;
        this.confirmMode = false;
        this.commandList.reload(this.selectedMod == null ? List.of() : this.selectedMod.commands());
        updateParamWidgets();
        updatePreview();
    }

    private void selectMod(CommandCatalog.ModSection section) {
        this.selectedMod = section;
        refreshCommandEntries();
    }

    private void selectCommand(CommandCatalog.CommandEntry command) {
        this.selectedCommand = command;
        this.choiceIndex = 0;
        this.confirmMode = false;
        if (command != null && command.kind() == CommandCatalog.CommandEntry.KIND_FREEFORM) {
            this.freeInput.setValue("");
        }
        updateParamWidgets();
        updatePreview();
    }

    private void cycleChoice() {
        if (this.selectedCommand == null || this.selectedCommand.choices().isEmpty()) {
            return;
        }
        this.choiceIndex = (this.choiceIndex + 1) % this.selectedCommand.choices().size();
        updateParamWidgets();
        updatePreview();
    }

    private void updateParamWidgets() {
        boolean has = this.selectedCommand != null;
        byte kind = has ? this.selectedCommand.kind() : CommandCatalog.CommandEntry.KIND_NONE;
        this.freeInput.setVisible(has && kind == CommandCatalog.CommandEntry.KIND_FREEFORM && !this.confirmMode);
        this.cycleButton.visible = has && kind == CommandCatalog.CommandEntry.KIND_CHOICE && !this.confirmMode;
        if (this.cycleButton.visible) {
            String choice = this.selectedCommand.choices().isEmpty()
                    ? "?"
                    : this.selectedCommand.choices().get(this.choiceIndex % this.selectedCommand.choices().size());
            this.cycleButton.setMessage(Component.literal("[ " + choice + " ⟳ ]"));
        }
        this.confirmButton.setMessage(Component.literal(this.confirmMode ? "✔ Confirmer" : "✔ Invoquer"));
    }

    private void updatePreview() {
        this.pendingPreview = buildCommandString();
    }

    private String buildCommandString() {
        if (this.selectedCommand == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(this.selectedCommand.name());
        switch (this.selectedCommand.kind()) {
            case CommandCatalog.CommandEntry.KIND_CHOICE -> {
                if (!this.selectedCommand.choices().isEmpty()) {
                    sb.append(' ').append(this.selectedCommand.choices().get(
                            this.choiceIndex % this.selectedCommand.choices().size()));
                }
            }
            case CommandCatalog.CommandEntry.KIND_FREEFORM -> {
                String value = this.freeInput.getValue() == null ? "" : this.freeInput.getValue().trim();
                if (!value.isEmpty()) {
                    sb.append(' ').append(value);
                }
            }
            default -> {
            }
        }
        return sb.toString();
    }

    private void onConfirm() {
        updatePreview();
        if (this.pendingPreview.isBlank()) {
            return;
        }
        if (this.selectedCommand != null
                && this.selectedCommand.kind() != CommandCatalog.CommandEntry.KIND_NONE
                && !this.confirmMode) {
            if (this.selectedCommand.kind() == CommandCatalog.CommandEntry.KIND_FREEFORM
                    && (this.freeInput.getValue() == null || this.freeInput.getValue().isBlank())) {
                return;
            }
            this.confirmMode = true;
            updateParamWidgets();
            return;
        }
        PacketDistributor.sendToServer(new RunCommandPayload(this.pendingPreview));
        this.onClose();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.freeInput.isVisible()) {
            updatePreview();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BG);

        // Outer ornate frame
        int o = 10;
        graphics.fill(o, o, this.width - o, this.height - o, BORDEAUX_DEEP);
        graphics.fill(o + 2, o + 2, this.width - o - 2, this.height - o - 2, PANEL);
        graphics.fill(o + 5, o + 5, this.width - o - 5, this.height - o - 5, PANEL_INNER);

        // Gold corner marks
        drawCorner(graphics, o + 6, o + 6, true, true);
        drawCorner(graphics, this.width - o - 6, o + 6, false, true);
        drawCorner(graphics, o + 6, this.height - o - 6, true, false);
        drawCorner(graphics, this.width - o - 6, this.height - o - 6, false, false);

        // Top blood rule
        graphics.fill(o + 20, o + 8, this.width - o - 20, o + 10, BORDEAUX);
        graphics.fill(o + 20, this.height - o - 10, this.width - o - 20, this.height - o - 8, BORDEAUX);

        // Subtle vertical motif
        for (int y = o + 40; y < this.height - o - 40; y += 28) {
            graphics.drawString(this.font, "❖", o + 14, y, BORDEAUX_DEEP, false);
            graphics.drawString(this.font, "❖", this.width - o - 22, y, BORDEAUX_DEEP, false);
        }
    }

    private void drawCorner(GuiGraphics graphics, int x, int y, boolean left, boolean top) {
        int dx = left ? 1 : -1;
        int dy = top ? 1 : -1;
        graphics.fill(x - (left ? 0 : 18), y - (top ? 0 : 2), x + (left ? 18 : 0), y + (top ? 2 : 0), GOLD_DIM);
        graphics.fill(x - (left ? 0 : 2), y - (top ? 0 : 18), x + (left ? 2 : 0), y + (top ? 18 : 0), GOLD_DIM);
        graphics.drawString(this.font, "⚜", x - (left ? 0 : 8) + dx, y - (top ? 0 : 8) + dy, GOLD, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, "⚜  THE BOOK OF NORMIES  ⚜", this.width / 2, 20, GOLD);
        graphics.drawCenteredString(this.font, "— Grimoire de la Famille Addams —", this.width / 2, 32, SLATE);

        int count = this.mods.size();
        int cmds = this.mods.stream().mapToInt(m -> m.commands().size()).sum();
        graphics.drawCenteredString(this.font,
                count + " registres  ·  " + cmds + " rituels",
                this.width / 2, 44, GOLD_DIM);

        graphics.drawString(this.font, "✦ REGISTRES", this.modList.getX() + 2, this.modList.getY() - 12, BORDEAUX, false);
        String rightTitle = this.selectedMod == null
                ? "✦ RITUELS"
                : ("✦ " + CommandCatalog.headerLabel(this.selectedMod));
        graphics.drawString(this.font, rightTitle, this.commandList.getX() + 2, this.commandList.getY() - 12, BORDEAUX, false);

        // Preview plaque
        int px = 26;
        int py = this.height - 58;
        graphics.fill(px, py - 2, this.width - 230, py + 12, ROW);
        String preview = this.pendingPreview.isBlank()
                ? "Choisissez un rituel dans les registres…"
                : (this.confirmMode ? ("Confirmer l'invocation : /" + this.pendingPreview)
                : ("Invocation : /" + this.pendingPreview));
        graphics.drawString(this.font, preview, px + 6, py, this.confirmMode ? GOLD : INK, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class ModList extends ObjectSelectionList<ModList.Entry> {
        ModList(int width, int height) {
            super(BookOfNormiesScreen.this.minecraft, width, height, 0, 20);
        }

        void reload(List<CommandCatalog.ModSection> sections, CommandCatalog.ModSection selected) {
            this.clearEntries();
            Entry selectedEntry = null;
            for (CommandCatalog.ModSection section : sections) {
                Entry entry = new Entry(section);
                this.addEntry(entry);
                if (section == selected) {
                    selectedEntry = entry;
                }
            }
            if (selectedEntry != null) {
                this.setSelected(selectedEntry);
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }

        @Override
        protected void renderListBackground(GuiGraphics graphics) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.getHeight(), ROW);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, GOLD_DIM);
            graphics.fill(this.getX(), this.getY() + this.getHeight() - 1, this.getX() + this.width, this.getY() + this.getHeight(), GOLD_DIM);
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final CommandCatalog.ModSection section;

            Entry(CommandCatalog.ModSection section) {
                this.section = section;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                boolean selected = ModList.this.getSelected() == this;
                int bg = selected ? ROW_SEL : (hovering ? ROW_HOVER : 0);
                if (bg != 0) {
                    graphics.fill(left, top, left + width, top + height, bg);
                }
                int color = selected ? GOLD : (hovering ? INK : SLATE);
                String mark = "minecraft".equals(this.section.modId()) ? "❖ " : "✦ ";
                String label = mark + this.section.displayName();
                graphics.drawString(BookOfNormiesScreen.this.font, label, left + 6, top + 6, color, false);
                if (selected) {
                    graphics.fill(left, top, left + 3, top + height, BORDEAUX);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                ModList.this.setSelected(this);
                BookOfNormiesScreen.this.selectMod(this.section);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.section.displayName());
            }
        }
    }

    private class CommandList extends ObjectSelectionList<CommandList.Entry> {
        CommandList(int width, int height) {
            super(BookOfNormiesScreen.this.minecraft, width, height, 0, 20);
        }

        void reload(List<CommandCatalog.CommandEntry> commands) {
            this.clearEntries();
            for (CommandCatalog.CommandEntry command : commands) {
                this.addEntry(new Entry(command));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }

        @Override
        protected void renderListBackground(GuiGraphics graphics) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.getHeight(), ROW);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, GOLD_DIM);
            graphics.fill(this.getX(), this.getY() + this.getHeight() - 1, this.getX() + this.width, this.getY() + this.getHeight(), GOLD_DIM);
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final CommandCatalog.CommandEntry command;

            Entry(CommandCatalog.CommandEntry command) {
                this.command = command;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                boolean selected = CommandList.this.getSelected() == this;
                int bg = selected ? ROW_SEL : (hovering ? ROW_HOVER : 0);
                if (bg != 0) {
                    graphics.fill(left, top, left + width, top + height, bg);
                }
                int color = selected ? GOLD : (hovering ? INK : SLATE);
                String suffix = switch (this.command.kind()) {
                    case CommandCatalog.CommandEntry.KIND_CHOICE -> "  〔↻〕";
                    case CommandCatalog.CommandEntry.KIND_FREEFORM -> "  〔" + this.command.argName() + "〕";
                    default -> "";
                };
                graphics.drawString(BookOfNormiesScreen.this.font, "/" + this.command.name() + suffix,
                        left + 8, top + 6, color, false);
                if (selected || hovering) {
                    graphics.fill(left, top, left + 3, top + height, BORDEAUX);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                CommandList.this.setSelected(this);
                BookOfNormiesScreen.this.selectCommand(this.command);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal("/" + this.command.name());
            }
        }
    }
}
