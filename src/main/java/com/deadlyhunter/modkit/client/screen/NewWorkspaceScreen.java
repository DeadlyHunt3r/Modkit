package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.deadlyhunter.modkit.network.CreateWorkspacePacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class NewWorkspaceScreen extends ModkitBaseScreen {

    private final WorkspaceSelectionScreen listParent;
    private EditBox nameField;
    private String errorMessage = null;

    public NewWorkspaceScreen(WorkspaceSelectionScreen parent) {
        super(Component.literal("New Project"), parent);
        this.listParent = parent;
        this.panelW = 260;
        this.panelH = 170;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = panelX + panelW / 2;
        int fieldW = 180;

        nameField = new EditBox(this.font,
                centerX - fieldW / 2, panelY + 60, fieldW, 20,
                Component.literal("modname"));
        nameField.setMaxLength(30);
        nameField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_]*"));
        this.addRenderableWidget(nameField);
        this.setInitialFocus(nameField);

        int footerY = panelY + panelH - 28;
        this.addRenderableWidget(Button.builder(
                Component.literal("Create"),
                btn -> tryCreate()
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    private void tryCreate() {
        String name = nameField.getValue().trim();
        if (!WorkspaceManager.isValidModName(name)) {
            errorMessage = WorkspaceManager.getValidationHint();
            return;
        }
        if (WorkspaceManager.exists(name)) {
            errorMessage = "A project named '" + name + "' already exists.";
            return;
        }

        PacketDistributor.sendToServer(new CreateWorkspacePacket(name));
        if (listParent != null) listParent.refresh();
        this.minecraft.setScreen(listParent);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.drawCenteredString(this.font,
                Component.literal("Pick a name for your new mod"),
                panelX + panelW / 2, panelY + 30, 0xFFFFFF);

        gfx.drawString(this.font, "Name:",
                panelX + panelW / 2 - 90 - 30, panelY + 66, 0xFFFFFF, false);

        gfx.drawCenteredString(this.font,
                Component.literal("3-30 chars, lowercase a-z, 0-9, underscore"),
                panelX + panelW / 2, panelY + 88, 0xFFFFFF);

        if (errorMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(errorMessage),
                    panelX + panelW / 2, panelY + 108, 0xAA0000);
        }
    }
}
