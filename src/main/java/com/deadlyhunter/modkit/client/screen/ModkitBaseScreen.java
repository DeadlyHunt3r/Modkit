package com.deadlyhunter.modkit.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ModkitBaseScreen extends Screen {

    private static final int COLOR_DIM       = 0xC0101010;
    private static final int COLOR_BORDER    = 0xFF000000;
    private static final int COLOR_BODY      = 0xFFC6C6C6;
    private static final int COLOR_HIGHLIGHT = 0xFFFFFFFF;
    private static final int COLOR_SHADOW    = 0xFF555555;
    private static final int COLOR_TITLE     = 0xFF404040;

    protected int panelW = 248;
    protected int panelH = 200;

    protected int panelX;
    protected int panelY;

    protected final Screen parent;

    protected ModkitBaseScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.panelX = (this.width - panelW) / 2;
        this.panelY = (this.height - panelH) / 2;
    }

    protected Checkbox checkbox(int x, int y, int width, int height, Component label, boolean selected) {
        return Checkbox.builder(label, this.font)
                .pos(x, y)
                .selected(selected)
                .maxWidth(width)
                .build();
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, COLOR_DIM);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        drawPanel(gfx, panelX, panelY, panelW, panelH);
        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.drawCenteredString(this.font, this.title,
                panelX + panelW / 2, panelY + 8, COLOR_TITLE);
        renderPanelContents(gfx, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, COLOR_BORDER);
        gfx.fill(x, y, x + w, y + h, COLOR_BODY);
        gfx.fill(x, y, x + w, y + 1, COLOR_HIGHLIGHT);
        gfx.fill(x, y, x + 1, y + h, COLOR_HIGHLIGHT);
        gfx.fill(x + w - 1, y + 1, x + w, y + h, COLOR_SHADOW);
        gfx.fill(x + 1, y + h - 1, x + w, y + h, COLOR_SHADOW);
    }

    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (parent != null) {
            this.minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }
}
