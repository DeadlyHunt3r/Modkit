package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.network.DeleteWorkspacePacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ConfirmDeleteWorkspaceScreen extends ModkitBaseScreen {

    private static final int TEXT = 0xFFFFFF;

    private final WorkspaceSelectionScreen listParent;
    private final String modName;
    private final String displayName;

    public ConfirmDeleteWorkspaceScreen(WorkspaceSelectionScreen parent, String modName, String displayName) {
        super(Component.literal("Delete Workspace"), parent);
        this.listParent = parent;
        this.modName = modName;
        this.displayName = displayName;
        this.panelW = 280;
        this.panelH = 170;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = panelX + panelW / 2;
        int footerY = panelY + panelH - 30;

        this.addRenderableWidget(Button.builder(
                Component.literal("Delete").withStyle(ChatFormatting.RED),
                btn -> confirmDelete()
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    private void confirmDelete() {
        PacketDistributor.sendToServer(new DeleteWorkspacePacket(modName));
        if (listParent != null) listParent.refresh();
        this.minecraft.setScreen(listParent);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int cx = panelX + panelW / 2;
        line(gfx, "Really delete this entire workspace?", cx, panelY + 36);
        line(gfx, "Name: " + displayName,                  cx, panelY + 56);
        line(gfx, "Folder: " + modName,                    cx, panelY + 70);
        line(gfx, "All items, textures and project files", cx, panelY + 92);
        line(gfx, "in this workspace will be removed.",    cx, panelY + 106);
    }

    private void line(GuiGraphics gfx, String text, int cx, int y) {
        gfx.drawCenteredString(this.font, Component.literal(text), cx, y, TEXT);
    }
}
