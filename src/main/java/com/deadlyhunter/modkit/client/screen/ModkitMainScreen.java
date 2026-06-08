package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.core.AuthorConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ModkitMainScreen extends ModkitBaseScreen {

    public ModkitMainScreen() {
        super(Component.literal("Modkit"), null);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int btnW = 200;
        int btnH = 20;
        int gap = 6;
        int startY = panelY + 60;

        boolean hasAuthor = AuthorConfig.isSet();

        if (!hasAuthor) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Set up Author"),
                    btn -> this.minecraft.setScreen(new AuthorSetupScreen(this))
            ).bounds(centerX - btnW / 2, startY, btnW, btnH).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("Close"),
                    btn -> this.onClose()
            ).bounds(centerX - btnW / 2, startY + btnH + gap, btnW, btnH).build());
        } else {
            this.addRenderableWidget(Button.builder(
                    Component.literal("My Projects"),
                    btn -> this.minecraft.setScreen(new WorkspaceSelectionScreen(this))
            ).bounds(centerX - btnW / 2, startY, btnW, btnH).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("Change Author (" + AuthorConfig.getAuthor() + ")"),
                    btn -> this.minecraft.setScreen(new AuthorSetupScreen(this))
            ).bounds(centerX - btnW / 2, startY + btnH + gap, btnW, btnH).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("Close"),
                    btn -> this.onClose()
            ).bounds(centerX - btnW / 2, startY + 2 * (btnH + gap), btnW, btnH).build());
        }
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (!AuthorConfig.isSet()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("Welcome! Let's set up your author name first."),
                    this.width / 2, panelY + 36, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal("Author: " + AuthorConfig.getAuthor()),
                    this.width / 2, panelY + 36, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }
}
