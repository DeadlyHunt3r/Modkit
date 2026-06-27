package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.core.AuthorConfig;
import net.neoforged.neoforge.network.PacketDistributor;
import com.deadlyhunter.modkit.network.SetAuthorPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class AuthorSetupScreen extends ModkitBaseScreen {

    private EditBox authorField;
    private String errorMessage = null;

    public AuthorSetupScreen(ModkitMainScreen parent) {
        super(Component.literal("Author Setup"), parent);
        this.panelW = 260;
        this.panelH = 170;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = panelX + panelW / 2;
        int fieldW = 180;

        authorField = new EditBox(this.font,
                centerX - fieldW / 2, panelY + 60, fieldW, 20,
                Component.literal("author"));
        authorField.setMaxLength(20);
        if (AuthorConfig.isSet()) authorField.setValue(AuthorConfig.getAuthor());
        authorField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_]*"));
        this.addRenderableWidget(authorField);
        this.setInitialFocus(authorField);

        int footerY = panelY + panelH - 28;
        this.addRenderableWidget(Button.builder(
                Component.literal("Save"),
                btn -> trySave()
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    private void trySave() {
        String val = authorField.getValue().trim();
        if (!AuthorConfig.isValid(val)) {
            errorMessage = AuthorConfig.getValidationHint();
            return;
        }
        AuthorConfig.setAuthor(val);
        PacketDistributor.sendToServer(new SetAuthorPacket(val));
        this.minecraft.setScreen(new ModkitMainScreen());
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.drawCenteredString(this.font,
                Component.literal("Your author prefix becomes part of every mod id"),
                panelX + panelW / 2, panelY + 26, 0xFFFFFF);
        gfx.drawCenteredString(this.font,
                Component.literal("Example: modkit_<author>_magicmod"),
                panelX + panelW / 2, panelY + 38, 0xFFFFFF);

        gfx.drawString(this.font, "Author:",
                panelX + panelW / 2 - 90 - 36, panelY + 66, 0xFFFFFF, false);

        gfx.drawCenteredString(this.font,
                Component.literal("3-20 chars, lowercase a-z, 0-9, underscore"),
                panelX + panelW / 2, panelY + 88, 0xFFFFFF);

        if (errorMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(errorMessage),
                    panelX + panelW / 2, panelY + 108, 0xAA0000);
        }
    }
}
