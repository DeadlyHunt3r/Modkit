package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.network.DeleteRecipePacket;
import com.deadlyhunter.modkit.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmDeleteRecipeScreen extends ModkitBaseScreen {

    private static final int TEXT = 0xFFFFFF;

    private final RecipeListScreen listScreen;
    private final String modName;
    private final String recipeId;
    private final String displayName;

    public ConfirmDeleteRecipeScreen(ShapedRecipeEditorScreen parent, String modName,
                                      String recipeId, String displayName) {
        this(parent, parent.getListParent(), modName, recipeId, displayName);
    }

    public ConfirmDeleteRecipeScreen(ShapelessRecipeEditorScreen parent, String modName,
                                      String recipeId, String displayName) {
        this(parent, parent.getListParent(), modName, recipeId, displayName);
    }

    public ConfirmDeleteRecipeScreen(SmeltingRecipeEditorScreen parent, String modName,
                                      String recipeId, String displayName) {
        this(parent, parent.getListParent(), modName, recipeId, displayName);
    }

    public ConfirmDeleteRecipeScreen(StonecuttingRecipeEditorScreen parent, String modName,
                                      String recipeId, String displayName) {
        this(parent, parent.getListParent(), modName, recipeId, displayName);
    }

    private ConfirmDeleteRecipeScreen(Screen visualParent, RecipeListScreen listScreen,
                                       String modName, String recipeId, String displayName) {
        super(Component.literal("Delete Recipe"), visualParent);
        this.listScreen = listScreen;
        this.modName = modName;
        this.recipeId = recipeId;
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
        ModNetworking.CHANNEL.sendToServer(new DeleteRecipePacket(modName, recipeId));
        listScreen.onRecipeChanged();
        this.minecraft.setScreen(listScreen);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int cx = panelX + panelW / 2;
        line(gfx, "Really delete this recipe?",  cx, panelY + 36);
        line(gfx, "Name: " + displayName,         cx, panelY + 56);
        line(gfx, "ID: " + recipeId,              cx, panelY + 70);
        line(gfx, "The recipe will be removed.",  cx, panelY + 92);
    }

    private void line(GuiGraphics gfx, String text, int cx, int y) {
        gfx.drawCenteredString(this.font, Component.literal(text), cx, y, TEXT);
    }
}
