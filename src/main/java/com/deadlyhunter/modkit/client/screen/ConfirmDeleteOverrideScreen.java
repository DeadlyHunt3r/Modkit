package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.network.DeleteOverridePacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ConfirmDeleteOverrideScreen extends ModkitBaseScreen {

    private static final int TEXT = 0xFFFFFF;

    private final OverrideEditorScreen editorParent;
    private final String modName;
    private final String overrideId;
    private final String displayName;

    public ConfirmDeleteOverrideScreen(OverrideEditorScreen parent, String modName,
                                        String overrideId, String displayName) {
        super(Component.literal("Delete Override"), parent);
        this.editorParent = parent;
        this.modName = modName;
        this.overrideId = overrideId;
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
        PacketDistributor.sendToServer(new DeleteOverridePacket(modName, overrideId));
        OverrideListScreen list = editorParent.getListParent();
        list.onOverrideChanged();
        this.minecraft.setScreen(list);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int cx = panelX + panelW / 2;
        gfx.drawCenteredString(this.font, Component.literal("Really delete this override?"), cx, panelY + 40, TEXT);
        gfx.drawCenteredString(this.font, Component.literal("ID: " + overrideId), cx, panelY + 62, TEXT);
        gfx.drawCenteredString(this.font, Component.literal("The original recipe returns to normal."), cx, panelY + 84, TEXT);
    }
}
