package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.network.DeleteItemPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ConfirmDeleteScreen extends ModkitBaseScreen {

    private static final int TEXT = 0xFFFFFF;

    private final ItemEditorScreen editorParent;
    private final String modName;
    private final String itemId;
    private final String displayName;

    public ConfirmDeleteScreen(ItemEditorScreen parent, String modName, String itemId, String displayName) {
        super(Component.literal("Delete Item"), parent);
        this.editorParent = parent;
        this.modName = modName;
        this.itemId = itemId;
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
        PacketDistributor.sendToServer(new DeleteItemPacket(modName, itemId));
        ItemListScreen list = editorParent.getListParent();
        list.onItemChanged();
        this.minecraft.setScreen(list);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int cx = panelX + panelW / 2;

        line(gfx, "Really delete this item?",        cx, panelY + 36);
        line(gfx, "Name: " + displayName,            cx, panelY + 56);
        line(gfx, "ID: " + itemId,                   cx, panelY + 70);
        line(gfx, "The JSON and texture will be removed.", cx, panelY + 90);
    }

    private void line(GuiGraphics gfx, String text, int cx, int y) {
        gfx.drawCenteredString(this.font, Component.literal(text), cx, y, TEXT);
    }
}
