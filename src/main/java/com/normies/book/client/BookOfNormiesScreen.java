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
import java.util.Locale;

/**
 * Addams-gothic command browser: mods on the left, commands on the right, params at the bottom.
 */
public class BookOfNormiesScreen extends Screen {
    private static final int COLOR_BG = 0xFF140F12;
    private static final int COLOR_PANEL = 0xFF1B1518;
    private static final int COLOR_BORDEAUX = 0xFF4A0E17;
    private static final int COLOR_GOLD = 0xFFB59410;
    private static final int COLOR_SLATE = 0xFFAE9AA3;
    private static final int COLOR_INK = 0xFFE8E0E4;

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
        int margin = 20;
        int leftW = Math.max(120, this.width / 4);
        int listTop = 40;
        int listBottom = this.height - 78;
        int listH = Math.max(60, listBottom - listTop);

        this.modList = new ModList(leftW - margin, listH);
        this.modList.setX(margin);
        this.modList.setY(listTop);
        this.addRenderableWidget(this.modList);

        int rightX = margin + leftW + 8;
        int rightW = this.width - rightX - margin;
        this.commandList = new CommandList(rightW, listH);
        this.commandList.setX(rightX);
        this.commandList.setY(listTop);
        this.addRenderableWidget(this.commandList);

        int bottomY = this.height - 52;
        this.freeInput = new EditBox(this.font, rightX, bottomY, Math.max(80, rightW - 220), 20,
                Component.literal("param"));
        this.freeInput.setMaxLength(256);
        this.freeInput.setVisible(false);
        this.addRenderableWidget(this.freeInput);

        this.cycleButton = Button.builder(Component.literal("[ ⟳ ]"), b -> cycleChoice())
                .bounds(rightX, bottomY, 90, 20)
                .build();
        this.cycleButton.visible = false;
        this.addRenderableWidget(this.cycleButton);

        this.confirmButton = Button.builder(Component.literal("Confirmer"), b -> onConfirm())
                .bounds(this.width - margin - 200, bottomY, 95, 20)
                .build();
        this.addRenderableWidget(this.confirmButton);

        this.closeButton = Button.builder(Component.literal("Fermer"), b -> onClose())
                .bounds(this.width - margin - 95, bottomY, 95, 20)
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
            this.cycleButton.setMessage(Component.literal("[" + choice + " ⟳]"));
        }
        this.confirmButton.setMessage(Component.literal(this.confirmMode ? "✔ Exécuter" : "Confirmer"));
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
        graphics.fill(0, 0, this.width, this.height, COLOR_BG);
        graphics.fill(12, 12, this.width - 12, this.height - 12, COLOR_PANEL);
        graphics.fill(12, 12, this.width - 12, 14, COLOR_BORDEAUX);
        graphics.fill(12, this.height - 14, this.width - 12, this.height - 12, COLOR_BORDEAUX);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, "❖ THE BOOK OF NORMIES ❖", this.width / 2, 18, COLOR_GOLD);

        String modTitle = this.selectedMod == null
                ? ""
                : ("minecraft".equals(this.selectedMod.modId())
                ? "MINECRAFT"
                : this.selectedMod.modId().toUpperCase(Locale.ROOT));
        graphics.drawString(this.font, "Mods", 24, 28, COLOR_SLATE, false);
        graphics.drawString(this.font, modTitle.isEmpty() ? "Commandes" : ("Commandes — " + modTitle),
                this.commandList.getX() + 4, 28, COLOR_SLATE, false);

        String preview = this.pendingPreview.isBlank() ? "Sélectionnez une commande" : ("/" + this.pendingPreview);
        if (this.confirmMode) {
            preview = "Confirmer : /" + this.pendingPreview;
        }
        graphics.drawString(this.font, preview, 24, this.height - 70, COLOR_INK, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class ModList extends ObjectSelectionList<ModList.Entry> {
        ModList(int width, int height) {
            super(BookOfNormiesScreen.this.minecraft, width, height, 0, 18);
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
            return this.width - 8;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
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
                int color = selected ? COLOR_GOLD : (hovering ? COLOR_INK : COLOR_SLATE);
                String label = "minecraft".equals(this.section.modId())
                        ? "❖ minecraft"
                        : "✦ " + this.section.modId();
                graphics.drawString(BookOfNormiesScreen.this.font, label, left + 4, top + 4, color, false);
                if (selected) {
                    graphics.fill(left, top + height - 1, left + width, top + height, COLOR_BORDEAUX);
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
                return Component.literal(this.section.modId());
            }
        }
    }

    private class CommandList extends ObjectSelectionList<CommandList.Entry> {
        CommandList(int width, int height) {
            super(BookOfNormiesScreen.this.minecraft, width, height, 0, 18);
        }

        void reload(List<CommandCatalog.CommandEntry> commands) {
            this.clearEntries();
            for (CommandCatalog.CommandEntry command : commands) {
                this.addEntry(new Entry(command));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 8;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
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
                int color = selected ? COLOR_GOLD : (hovering ? COLOR_INK : COLOR_SLATE);
                String suffix = switch (this.command.kind()) {
                    case CommandCatalog.CommandEntry.KIND_CHOICE -> " [↻]";
                    case CommandCatalog.CommandEntry.KIND_FREEFORM -> " [" + this.command.argName() + "]";
                    default -> "";
                };
                graphics.drawString(BookOfNormiesScreen.this.font, "/" + this.command.name() + suffix,
                        left + 4, top + 4, color, false);
                if (selected || hovering) {
                    graphics.fill(left, top, left + 2, top + height, COLOR_BORDEAUX);
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
