package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.content.recipe.RecipeDefinition;
import com.deadlyhunter.modkit.content.recipe.RecipeDefinition.Ingredient;
import com.deadlyhunter.modkit.network.ModNetworking;
import com.deadlyhunter.modkit.network.SaveRecipePacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class StonecuttingRecipeEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int SLOT_SIZE = 32;

    private final RecipeListScreen listParent;
    private final String modName;
    private final RecipeDefinition def;
    private final boolean isNew;

    private EditBox idField;
    private EditBox displayNameField;
    private CycleButton<String> resultSourceBtn;
    private EditBox resultItemField;
    private EditBox resultCountField;
    private String errorMessage = null;

    public StonecuttingRecipeEditorScreen(RecipeListScreen parent, String modName,
                                           RecipeDefinition def, boolean isNew) {
        super(Component.literal(isNew ? "New Stonecutting Recipe" : "Edit: " + def.id), parent);
        this.listParent = parent;
        this.modName = modName;
        this.def = def;
        this.isNew = isNew;
        this.panelW = 320;
        this.panelH = 235;
        this.def.type = "stonecutting";
        if (this.def.input == null) this.def.input = new Ingredient("mine", "");
    }

    public RecipeListScreen getListParent() { return listParent; }

    @Override
    protected void init() {
        super.init();
        rebuildWidgetsForEditor();
    }

    private void rebuildWidgetsForEditor() {
        this.clearWidgets();

        int leftPanelX = panelX + 16;
        int rightPanelX = panelX + panelW / 2 + 8;
        int fieldW = 130;

        idField = new EditBox(this.font, leftPanelX + 50, panelY + 26, panelW - 80, 16,
                Component.literal("id"));
        idField.setMaxLength(40);
        idField.setValue(def.id != null ? def.id : "");
        if (!isNew) idField.setEditable(false);
        idField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_]*"));
        this.addRenderableWidget(idField);

        displayNameField = new EditBox(this.font, leftPanelX + 50, panelY + 48, panelW - 80, 16,
                Component.literal("display"));
        displayNameField.setMaxLength(64);
        displayNameField.setValue(def.displayName != null ? def.displayName : "");
        this.addRenderableWidget(displayNameField);

        Ingredient input = def.input;
        String inputLabel = (input == null || input.isEmpty()) ? "+" : "✓";
        this.addRenderableWidget(Button.builder(
                Component.literal(inputLabel),
                btn -> openInputPicker()
        ).bounds(leftPanelX + 16, panelY + 96, SLOT_SIZE, SLOT_SIZE).build());

        resultSourceBtn = CycleButton.<String>builder(s ->
                        Component.literal("mine".equals(s) ? "My Item" : "Custom"))
                .withValues("mine", "other")
                .withInitialValue(def.resultSource != null ? def.resultSource : "mine")
                .displayOnlyValue()
                .create(rightPanelX + 48, panelY + 92, fieldW - 48, 18, Component.literal(""));
        this.addRenderableWidget(resultSourceBtn);

        resultItemField = new EditBox(this.font, rightPanelX, panelY + 128, fieldW, 18,
                Component.literal("result"));
        resultItemField.setMaxLength(80);
        resultItemField.setValue(def.resultItem != null ? def.resultItem : "");
        resultItemField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_:/]*"));
        this.addRenderableWidget(resultItemField);

        resultCountField = new EditBox(this.font, rightPanelX, panelY + 170, fieldW, 18,
                Component.literal("count"));
        resultCountField.setMaxLength(2);
        resultCountField.setValue(String.valueOf(def.resultCount > 0 ? def.resultCount : 1));
        resultCountField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,2}"));
        this.addRenderableWidget(resultCountField);

        int footerY = panelY + panelH - 30;
        int btnW = 70;
        int gap = 6;
        int centerX = panelX + panelW / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("Save"),
                btn -> trySave()
        ).bounds(centerX - btnW - gap / 2, footerY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose()
        ).bounds(centerX + gap / 2, footerY, btnW, 20).build());

        if (!isNew) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Delete").withStyle(ChatFormatting.RED),
                    btn -> this.minecraft.setScreen(
                            new ConfirmDeleteRecipeScreen(this, modName, def.id, def.displayName))
            ).bounds(panelX + 12, footerY, 60, 20).build());
        }
    }

    private void openInputPicker() {
        Ingredient current = def.input != null ? def.input : new Ingredient("mine", "");
        this.minecraft.setScreen(new IngredientPickerScreen(this, modName, current, true,
                result -> {
                    if (result == null || result.isEmpty()) {
                        def.input = new Ingredient("mine", "");
                    } else {
                        def.input = result;
                    }
                }));
    }

    private void trySave() {
        String newId = idField.getValue().trim();
        String newDisplay = displayNameField.getValue().trim();

        int count;
        try {
            count = resultCountField.getValue().isEmpty() ? 1 : Integer.parseInt(resultCountField.getValue());
        } catch (NumberFormatException e) {
            errorMessage = "Invalid count.";
            return;
        }

        if (def.input == null || def.input.isEmpty()) {
            errorMessage = "Set an input item first.";
            return;
        }

        def.id = newId;
        def.displayName = newDisplay.isEmpty() ? newId : newDisplay;
        def.resultSource = resultSourceBtn.getValue();
        def.resultItem = resultItemField.getValue().trim();
        def.resultCount = count;

        String err = def.validate();
        if (err != null) { errorMessage = err; return; }

        String json = GSON.toJson(def);
        ModNetworking.CHANNEL.sendToServer(new SaveRecipePacket(modName, def.id, json));
        listParent.onRecipeChanged();
        this.minecraft.setScreen(listParent);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 16;

        gfx.drawString(this.font, "ID",      labelX, panelY + 30, LABEL_COLOR, true);
        gfx.drawString(this.font, "Display", labelX, panelY + 52, LABEL_COLOR, true);

        gfx.drawString(this.font, "Input:",  labelX, panelY + 82, LABEL_COLOR, true);
        gfx.drawString(this.font, "→",       labelX + 56, panelY + 110, LABEL_COLOR, true);

        int rightLabelX = panelX + panelW / 2 + 8;
        gfx.drawString(this.font, "Result:",   rightLabelX, panelY + 78,  LABEL_COLOR, true);
        gfx.drawString(this.font, "Source:",   rightLabelX, panelY + 96,  LABEL_COLOR, true);
        gfx.drawString(this.font, "Item ID:",  rightLabelX, panelY + 118, LABEL_COLOR, true);
        gfx.drawString(this.font, "Count:",    rightLabelX, panelY + 160, LABEL_COLOR, true);

        int hintY = panelY + panelH - 46;
        if (errorMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(errorMessage),
                    panelX + panelW / 2, hintY, 0xFF5555);
        } else {
            String hint = isNew
                    ? "Stonecutter recipe — no XP or cook time, just 1 input → N output"
                    : "ID locked — delete & recreate to rename";
            gfx.drawCenteredString(this.font,
                    Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        }
    }
}
