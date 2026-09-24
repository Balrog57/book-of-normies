package com.normies.book.client;

import com.normies.book.BookOfNormiesMod;
import com.normies.book.network.RunCommandPayload;
import com.normies.book.service.CommandCatalog;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Open Addams Family grimoire: textured pages, animated mod tabs, gothic command dropdown.
 */
public class BookOfNormiesScreen extends Screen {
    private static final ResourceLocation PAGE_TEX =
            ResourceLocation.fromNamespaceAndPath(BookOfNormiesMod.MOD_ID, "textures/gui/grimoire_page.png");
    private static final ResourceLocation BIND_TEX =
            ResourceLocation.fromNamespaceAndPath(BookOfNormiesMod.MOD_ID, "textures/gui/grimoire_binding.png");

    private static final int BG = 0xFF070405;
    private static final int GOLD = 0xFFC4A35A;
    private static final int GOLD_DIM = 0xFF8A7340;
    private static final int BORDEAUX = 0xFF4A0E17;
    private static final int INK = 0xFF2A1A18;
    private static final int SLATE = 0xFF6E5B4B;
    private static final int ANIM_TICKS = 8;

    private final List<CommandCatalog.ModSection> mods;

    private GrimoireTabBar tabBar;
    private GothicDropdown dropdown;
    private EditBox freeInput;
    private Button cycleButton;
    private Button confirmButton;
    private Button closeButton;

    private int selectedModIndex;
    private CommandCatalog.CommandEntry selectedCommand;
    private int choiceIndex;
    private String pendingPreview = "";
    private boolean confirmMode;

    private float animProgress = 1f;
    private int animTicksLeft;
    private int bookLeft;
    private int bookTop;
    private int bookW;
    private int bookH;
    private int pageW;
    private int gutter;

    public BookOfNormiesScreen(List<CommandCatalog.ModSection> mods) {
        super(Component.literal("The Book of Normies"));
        this.mods = mods == null ? List.of() : mods;
        this.selectedModIndex = 0;
    }

    private CommandCatalog.ModSection currentMod() {
        if (mods.isEmpty() || selectedModIndex < 0 || selectedModIndex >= mods.size()) {
            return null;
        }
        return mods.get(selectedModIndex);
    }

    @Override
    protected void init() {
        this.bookW = Mth.clamp((int) (this.width * 0.82f), 320, 520);
        this.bookH = Mth.clamp((int) (this.height * 0.72f), 220, 320);
        this.bookLeft = (this.width - this.bookW) / 2;
        this.bookTop = (this.height - this.bookH) / 2 + 8;
        this.gutter = 18;
        this.pageW = (this.bookW - this.gutter) / 2;

        int tabY = this.bookTop - 26;
        this.tabBar = new GrimoireTabBar(this.bookLeft + 12, tabY, this.bookW - 24, this.mods,
                this.selectedModIndex, this::onTabSelected);
        this.addRenderableWidget(this.tabBar);

        int rightX = this.bookLeft + this.pageW + this.gutter + 16;
        int rightInnerW = this.pageW - 36;

        this.dropdown = new GothicDropdown(rightX, this.bookTop + 56, rightInnerW);
        this.dropdown.setOnSelect(this::onCommandSelected);
        this.addRenderableWidget(this.dropdown);

        this.freeInput = new EditBox(this.font, rightX, this.bookTop + 96, rightInnerW, 20,
                Component.literal("param"));
        this.freeInput.setMaxLength(256);
        this.freeInput.setTextColor(INK);
        this.freeInput.setVisible(false);
        this.addRenderableWidget(this.freeInput);

        this.cycleButton = Button.builder(Component.literal("[ option ⟳ ]"), b -> cycleChoice())
                .bounds(rightX, this.bookTop + 96, Math.min(160, rightInnerW), 20)
                .build();
        this.cycleButton.visible = false;
        this.addRenderableWidget(this.cycleButton);

        int btnY = this.bookTop + this.bookH - 36;
        this.confirmButton = Button.builder(Component.literal("✔ Invoquer"), b -> onConfirm())
                .bounds(rightX, btnY, 100, 20)
                .build();
        this.addRenderableWidget(this.confirmButton);

        this.closeButton = Button.builder(Component.literal("✖ Clore"), b -> onClose())
                .bounds(rightX + rightInnerW - 90, btnY, 90, 20)
                .build();
        this.addRenderableWidget(this.closeButton);

        reloadCommandsForMod();
        updateParamWidgets();
        updatePreview();
    }

    private void onTabSelected(int index) {
        if (index == this.selectedModIndex) {
            return;
        }
        this.selectedModIndex = index;
        this.animTicksLeft = ANIM_TICKS;
        this.animProgress = 0f;
        this.dropdown.setLocked(true);
        this.dropdown.collapse();
        this.confirmMode = false;
        reloadCommandsForMod();
        updateParamWidgets();
        updatePreview();
    }

    private void reloadCommandsForMod() {
        CommandCatalog.ModSection mod = currentMod();
        this.dropdown.setEntries(mod == null ? List.of() : mod.commands());
        this.choiceIndex = 0;
        this.selectedCommand = this.dropdown.getSelected();
    }

    private void onCommandSelected(CommandCatalog.CommandEntry entry) {
        this.selectedCommand = entry;
        this.choiceIndex = 0;
        this.confirmMode = false;
        if (entry != null && entry.kind() == CommandCatalog.CommandEntry.KIND_FREEFORM) {
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
        boolean animating = this.animTicksLeft > 0;
        boolean has = this.selectedCommand != null && !animating;
        byte kind = has ? this.selectedCommand.kind() : CommandCatalog.CommandEntry.KIND_NONE;

        this.freeInput.setVisible(has && kind == CommandCatalog.CommandEntry.KIND_FREEFORM && !this.confirmMode);
        this.cycleButton.visible = has && kind == CommandCatalog.CommandEntry.KIND_CHOICE && !this.confirmMode;
        if (this.cycleButton.visible) {
            String choice = this.selectedCommand.choices().isEmpty()
                    ? "?"
                    : this.selectedCommand.choices().get(this.choiceIndex % this.selectedCommand.choices().size());
            this.cycleButton.setMessage(Component.literal("[ " + choice + " ⟳ ]"));
        }
        this.confirmButton.active = has;
        this.confirmButton.setMessage(Component.literal(this.confirmMode ? "✔ Confirmer" : "✔ Invoquer"));
        this.dropdown.setLocked(animating);
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
        if (this.pendingPreview.isBlank() || this.animTicksLeft > 0) {
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
        if (this.animTicksLeft > 0) {
            this.animTicksLeft--;
            this.animProgress = 1f - (this.animTicksLeft / (float) ANIM_TICKS);
            this.animProgress = easeOutCubic(this.animProgress);
            if (this.animTicksLeft == 0) {
                this.animProgress = 1f;
                updateParamWidgets();
            }
        }
        if (this.freeInput.isVisible()) {
            updatePreview();
        }
    }

    private static float easeOutCubic(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BG);
        // Vignette
        graphics.fill(0, 0, this.width, 24, 0xAA000000);
        graphics.fill(0, this.height - 24, this.width, this.height, 0xAA000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, "⚜  THE BOOK OF NORMIES  ⚜", this.width / 2, 10, GOLD);
        graphics.drawCenteredString(this.font, "— Grimoire de la Famille Addams —", this.width / 2, 22, GOLD_DIM);

        drawBook(graphics);

        // Page-turn fade overlay on right page during animation
        if (this.animProgress < 1f) {
            int alpha = (int) ((1f - this.animProgress) * 180);
            int color = (alpha << 24) | 0xE8D9C0;
            int rx = this.bookLeft + this.pageW + this.gutter;
            graphics.fill(rx, this.bookTop, this.bookLeft + this.bookW, this.bookTop + this.bookH, color);
            float slide = (1f - this.animProgress) * 24f;
            graphics.pose().pushPose();
            graphics.pose().translate(slide, 0, 0);
            // decorative sweep line
            graphics.fill(rx + 8, this.bookTop + 40, rx + 12, this.bookTop + this.bookH - 40, BORDEAUX);
            graphics.pose().popPose();
        }

        drawLeftPageText(graphics);
        drawRightPageChrome(graphics);
    }

    private void drawBook(GuiGraphics graphics) {
        int left = this.bookLeft;
        int top = this.bookTop;
        int mid = left + this.pageW;

        // Shadow
        graphics.fill(left + 6, top + 8, left + this.bookW + 6, top + this.bookH + 8, 0x66000000);

        // Left page
        graphics.blit(PAGE_TEX, left, top, 0, 0, this.pageW, this.bookH, 256, 256);
        // Right page
        graphics.blit(PAGE_TEX, mid + this.gutter, top, 0, 0, this.pageW, this.bookH, 256, 256);
        // Binding
        graphics.blit(BIND_TEX, mid - 4, top - 4, 0, 0, this.gutter + 8, this.bookH + 8, 256, 256);

        // Gold frame
        graphics.fill(left, top, left + this.bookW, top + 2, GOLD_DIM);
        graphics.fill(left, top + this.bookH - 2, left + this.bookW, top + this.bookH, GOLD_DIM);
        graphics.fill(left, top, left + 2, top + this.bookH, GOLD_DIM);
        graphics.fill(left + this.bookW - 2, top, left + this.bookW, top + this.bookH, GOLD_DIM);

        // Corner fleurs
        graphics.drawString(this.font, "⚜", left + 6, top + 4, BORDEAUX, false);
        graphics.drawString(this.font, "⚜", left + this.bookW - 14, top + 4, BORDEAUX, false);
        graphics.drawString(this.font, "⚜", left + 6, top + this.bookH - 14, BORDEAUX, false);
        graphics.drawString(this.font, "⚜", left + this.bookW - 14, top + this.bookH - 14, BORDEAUX, false);
    }

    private void drawLeftPageText(GuiGraphics graphics) {
        CommandCatalog.ModSection mod = currentMod();
        int lx = this.bookLeft + 18;
        int ly = this.bookTop + 28;

        graphics.drawString(this.font, "❖ REGISTRE ❖", lx, ly, BORDEAUX, false);
        graphics.fill(lx, ly + 12, lx + this.pageW - 36, ly + 13, GOLD_DIM);

        String title = mod == null ? "—" : CommandCatalog.headerLabel(mod);
        // Wrap-ish single line truncate
        if (this.font.width(title) > this.pageW - 40) {
            title = this.font.plainSubstrByWidth(title, this.pageW - 48) + "…";
        }
        graphics.drawString(this.font, title, lx, ly + 24, INK, false);

        String display = mod == null ? "" : mod.displayName();
        graphics.drawString(this.font, display, lx, ly + 40, SLATE, false);

        int count = mod == null ? 0 : mod.commands().size();
        graphics.drawString(this.font, count + " rituel" + (count == 1 ? "" : "s"), lx, ly + 58, BORDEAUX, false);

        graphics.drawString(this.font, "─────────────", lx, ly + 78, GOLD_DIM, false);
        graphics.drawString(this.font, "« Choses funèbres", lx, ly + 96, SLATE, false);
        graphics.drawString(this.font, "et commandes", lx, ly + 108, SLATE, false);
        graphics.drawString(this.font, "occultes. »", lx, ly + 120, SLATE, false);

        graphics.drawCenteredString(this.font, "⚜ ❖ ⚜",
                this.bookLeft + this.pageW / 2, this.bookTop + this.bookH - 28, GOLD);

        int totalMods = this.mods.size();
        int totalCmds = this.mods.stream().mapToInt(m -> m.commands().size()).sum();
        graphics.drawString(this.font, totalMods + " registres · " + totalCmds + " rituels",
                lx, this.bookTop + this.bookH - 44, GOLD_DIM, false);
    }

    private void drawRightPageChrome(GuiGraphics graphics) {
        int rx = this.bookLeft + this.pageW + this.gutter + 16;
        int ry = this.bookTop + 28;

        graphics.drawString(this.font, "✦ INVOCATION ✦", rx, ry, BORDEAUX, false);
        graphics.fill(rx, ry + 12, rx + this.pageW - 36, ry + 13, GOLD_DIM);
        graphics.drawString(this.font, "Rituel", rx, ry + 18, SLATE, false);

        String preview = this.pendingPreview.isBlank()
                ? "Sélectionnez un rituel dans le menu…"
                : (this.confirmMode ? ("Confirmer : /" + this.pendingPreview)
                : ("/" + this.pendingPreview));
        int py = this.bookTop + 128;
        graphics.fill(rx - 2, py, rx + this.pageW - 34, py + 28, 0x33C4A35A);
        graphics.drawWordWrap(this.font, Component.literal(preview), rx, py + 6, this.pageW - 44, INK);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Let dropdown consume first when expanded
        if (this.dropdown != null && this.dropdown.isExpanded()) {
            if (this.dropdown.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (!this.dropdown.isMouseOverExpanded(mouseX, mouseY)) {
                this.dropdown.collapse();
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
