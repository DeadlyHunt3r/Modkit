package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.network.DeleteToolPacket;
import com.deadlyhunter.modkit.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ConfirmDeleteToolScreen extends ModkitBaseScreen {

    private static final int TEXT = 0xFFFFFF;

    private final ToolEditorScreen editorParent;
    private final String modName;
    private final String toolId;
    private final String displayName;

    public ConfirmDeleteToolScreen(ToolEditorScreen parent, String modName,
                                    String toolId, String displayName) {
        super(Component.literal("Delete Tool"), parent);
        this.editorParent = parent;
        this.modName = modName;
        this.toolId = toolId;
        this.displayName = displayName;
        this.panelW = 260;
        this.panelH = 150;
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
        ModNetworking.CHANNEL.sendToServer(new DeleteToolPacket(modName, toolId));
        ToolListScreen list = editorParent.getListParent();
        list.onToolChanged();
        this.minecraft.setScreen(list);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int cx = panelX + panelW / 2;
        line(gfx, "Really delete this tool?",       cx, panelY + 36);
        line(gfx, "Name: " + displayName,            cx, panelY + 56);
        line(gfx, "ID: " + toolId,                   cx, panelY + 70);
        line(gfx, "The tool will be removed.",       cx, panelY + 92);
    }

    private void line(GuiGraphics gfx, String text, int cx, int y) {
        gfx.drawCenteredString(this.font, Component.literal(text), cx, y, TEXT);
    }
}
