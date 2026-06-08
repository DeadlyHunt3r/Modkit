package com.deadlyhunter.modkit.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ViewJsonScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int TEXT = 0xFFFFFF;

    private final String json;
    private final String[] lines;
    private final String title;
    private int scroll = 0;
    private int visibleLines;

    public ViewJsonScreen(Screen parent, String title, Object definition) {
        super(Component.literal(title), parent);
        this.title = title;
        this.json = GSON.toJson(definition);
        this.lines = json.split("\n");
        this.panelW = 320;
        this.panelH = 260;
    }

    @Override
    protected void init() {
        super.init();
        this.visibleLines = (panelH - 70) / 10;

        int centerX = panelX + panelW / 2;
        int footerY = panelY + panelH - 30;

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX - 50, footerY, 100, 20).build());

        if (lines.length > visibleLines) {
            int sX = panelX + panelW - 24;
            this.addRenderableWidget(Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) scroll--; })
                    .bounds(sX, panelY + 28, 16, 16).build());
            this.addRenderableWidget(Button.builder(Component.literal("▼"),
                    b -> { if (scroll + visibleLines < lines.length) scroll++; })
                    .bounds(sX, panelY + 28 + 18, 16, 16).build());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (lines.length > visibleLines) {
            int newScroll = scroll - (int) Math.signum(delta);
            newScroll = Math.max(0, Math.min(lines.length - visibleLines, newScroll));
            if (newScroll != scroll) scroll = newScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.drawCenteredString(this.font,
                Component.literal("Read-only — edit values in the editor")
                        .withStyle(ChatFormatting.GRAY),
                panelX + panelW / 2, panelY + 22, TEXT);

        int x = panelX + 16;
        int y = panelY + 50;
        int end = Math.min(lines.length, scroll + visibleLines);
        for (int i = scroll; i < end; i++) {
            gfx.drawString(this.font, lines[i], x, y, TEXT, true);
            y += 10;
        }

        if (lines.length > visibleLines) {
            String info = (scroll + 1) + "-" + Math.min(scroll + visibleLines, lines.length)
                    + " of " + lines.length;
            gfx.drawString(this.font, info,
                    panelX + panelW - 80, panelY + panelH - 46, TEXT, true);
        }
    }
}
