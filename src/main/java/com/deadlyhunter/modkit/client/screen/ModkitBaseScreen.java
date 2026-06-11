package com.deadlyhunter.modkit.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class ModkitBaseScreen extends Screen {

    private static final ResourceLocation INVENTORY_TEX =
            new ResourceLocation("textures/gui/container/inventory.png");

    private static final int BORDER = 4;

    private static final int SRC_W = 176;
    private static final int SRC_H = 166;

    private static final int FILL_SRC_X = 10;
    private static final int FILL_SRC_Y = 10;
    private static final int FILL_PATCH = 8;

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

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        drawPanel(gfx, panelX, panelY, panelW, panelH);
        gfx.drawCenteredString(this.font, this.title,
                panelX + panelW / 2, panelY + 8, 0xFFFFFF);
        renderPanelContents(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        final int sx = 0;
        final int sy = 0;

        gfx.blit(INVENTORY_TEX, x,                 y,                 sx,                 sy,                 BORDER, BORDER, 256, 256);
        gfx.blit(INVENTORY_TEX, x + w - BORDER,    y,                 sx + SRC_W - BORDER, sy,                BORDER, BORDER, 256, 256);
        gfx.blit(INVENTORY_TEX, x,                 y + h - BORDER,    sx,                 sy + SRC_H - BORDER, BORDER, BORDER, 256, 256);
        gfx.blit(INVENTORY_TEX, x + w - BORDER,    y + h - BORDER,    sx + SRC_W - BORDER, sy + SRC_H - BORDER, BORDER, BORDER, 256, 256);

        final int innerW = w - 2 * BORDER;
        final int innerH = h - 2 * BORDER;
        final int srcInnerW = SRC_W - 2 * BORDER;

        tile(gfx, x + BORDER,        y,              innerW, BORDER,
                sx + BORDER,        sy,             srcInnerW, BORDER, false);
        tile(gfx, x + BORDER,        y + h - BORDER, innerW, BORDER,
                sx + BORDER,        sy + SRC_H - BORDER, srcInnerW, BORDER, false);

        tile(gfx, x,                 y + BORDER,     BORDER, innerH,
                sx,                 sy + BORDER,    BORDER, FILL_PATCH, false);
        tile(gfx, x + w - BORDER,    y + BORDER,     BORDER, innerH,
                sx + SRC_W - BORDER, sy + BORDER,   BORDER, FILL_PATCH, false);
        tile(gfx, x + BORDER,        y + BORDER,     innerW, innerH,
                FILL_SRC_X,         FILL_SRC_Y,     FILL_PATCH, FILL_PATCH, true);
    }

    private void tile(GuiGraphics gfx, int dx, int dy, int destW, int destH,
                      int sx, int sy, int srcW, int srcH, boolean twoD) {
        int yPos = 0;
        while (yPos < destH) {
            int chunkH = Math.min(srcH, destH - yPos);
            int xPos = 0;
            while (xPos < destW) {
                int chunkW = Math.min(srcW, destW - xPos);
                gfx.blit(INVENTORY_TEX, dx + xPos, dy + yPos,
                        sx, sy, chunkW, chunkH, 256, 256);
                xPos += chunkW;
            }
            yPos += chunkH;
        }
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
