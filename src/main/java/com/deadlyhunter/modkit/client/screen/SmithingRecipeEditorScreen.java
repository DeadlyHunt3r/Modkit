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

public class SmithingRecipeEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int SLOT_SIZE = 30;

    private final RecipeListScreen listParent;
    private final String modName;
    private final RecipeDefinition def;
    private final boolean isNew;

    private EditBox idField;
    private EditBox displayNameField;
    private CycleButton<String> resultSourceBtn;
    private EditBox resultItemField;
    private String errorMessage = null;

    public SmithingRecipeEditorScreen(RecipeListScreen parent, String modName,
                                       RecipeDefinition def, boolean isNew) {
        super(Component.literal(isNew ? "New Smithing Recipe" : "Edit: " + def.id), parent);
        this.listParent = parent;
        this.modName = modName;
        this.def = def;
        this.isNew = isNew;
        this.panelW = 340;
        this.panelH = 240;
        this.def.type = "smithing";
        if (this.def.smithingTemplate == null) this.def.smithingTemplate = new Ingredient("other", "");
        if (this.def.smithingBase == null) this.def.smithingBase = new Ingredient("other", "");
        if (this.def.smithingAddition == null) this.def.smithingAddition = new Ingredient("mine", "");
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
        int rightPanelX = panelX + panelW / 2 + 20;
        int fieldW = 130;

        idField = new EditBox(this.font, leftPanelX + 50, panelY + 26, panelW - 80, 16, Component.empty());
        idField.setMaxLength(40);
        idField.setValue(def.id != null ? def.id : "");
        if (!isNew) idField.setEditable(false);
        idField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_]*"));
        this.addRenderableWidget(idField);

        displayNameField = new EditBox(this.font, leftPanelX + 50, panelY + 48, panelW - 80, 16, Component.empty());
        displayNameField.setMaxLength(64);
        displayNameField.setValue(def.displayName != null ? def.displayName : "");
        this.addRenderableWidget(displayNameField);

        int slotY = panelY + 96;
        int slotGap = 44;

        this.addRenderableWidget(Button.builder(
                Component.literal(slotLabel(def.smithingTemplate)),
                btn -> openPicker("template")
        ).bounds(leftPanelX + 4, slotY, SLOT_SIZE, SLOT_SIZE).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(slotLabel(def.smithingBase)),
                btn -> openPicker("base")
        ).bounds(leftPanelX + 4 + slotGap, slotY, SLOT_SIZE, SLOT_SIZE).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(slotLabel(def.smithingAddition)),
                btn -> openPicker("addition")
        ).bounds(leftPanelX + 4 + 2 * slotGap, slotY, SLOT_SIZE, SLOT_SIZE).build());

        resultSourceBtn = CycleButton.<String>builder(s ->
                        Component.literal("mine".equals(s) ? "My Item" : "Custom"))
                .withValues("mine", "other")
                .withInitialValue(def.resultSource != null ? def.resultSource : "mine")
                .displayOnlyValue()
                .create(rightPanelX + 48, panelY + 92, fieldW - 48, 18, Component.literal(""));
        this.addRenderableWidget(resultSourceBtn);

        resultItemField = new EditBox(this.font, rightPanelX, panelY + 128, fieldW, 18, Component.empty());
        resultItemField.setMaxLength(80);
        resultItemField.setValue(def.resultItem != null ? def.resultItem : "");
        resultItemField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_:/]*"));
        this.addRenderableWidget(resultItemField);

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

    private static String slotLabel(Ingredient ing) {
        return (ing == null || ing.isEmpty()) ? "+" : "✓";
    }

    private void openPicker(String which) {
        Ingredient current = switch (which) {
            case "template" -> def.smithingTemplate;
            case "base"     -> def.smithingBase;
            default         -> def.smithingAddition;
        };
        if (current == null) current = new Ingredient("other", "");
        final Ingredient start = current;
        this.minecraft.setScreen(new IngredientPickerScreen(this, modName, start, true,
                result -> {
                    Ingredient value = (result == null || result.isEmpty())
                            ? new Ingredient("other", "") : result;
                    switch (which) {
                        case "template" -> def.smithingTemplate = value;
                        case "base"     -> def.smithingBase = value;
                        default         -> def.smithingAddition = value;
                    }
                }));
    }

    private void trySave() {
        String newId = idField.getValue().trim();
        String newDisplay = displayNameField.getValue().trim();

        if (def.smithingTemplate == null || def.smithingTemplate.isEmpty()) {
            errorMessage = "Set a template item first.";
            return;
        }
        if (def.smithingBase == null || def.smithingBase.isEmpty()) {
            errorMessage = "Set a base item first.";
            return;
        }
        if (def.smithingAddition == null || def.smithingAddition.isEmpty()) {
            errorMessage = "Set an addition item first.";
            return;
        }

        def.id = newId;
        def.displayName = newDisplay.isEmpty() ? newId : newDisplay;
        def.resultSource = resultSourceBtn.getValue();
        def.resultItem = resultItemField.getValue().trim();
        def.resultCount = 1;

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

        gfx.drawString(this.font, "Smithing Inputs:", labelX, panelY + 80, LABEL_COLOR, true);

        int slotGap = 44;
        gfx.drawString(this.font, "Tmpl",  labelX + 4,            panelY + 128, 0xAAAAAA, true);
        gfx.drawString(this.font, "Base",  labelX + 4 + slotGap,  panelY + 128, 0xAAAAAA, true);
        gfx.drawString(this.font, "Add",   labelX + 4 + 2 * slotGap, panelY + 128, 0xAAAAAA, true);

        int rightLabelX = panelX + panelW / 2 + 20;
        gfx.drawString(this.font, "Result:",  rightLabelX, panelY + 78,  LABEL_COLOR, true);
        gfx.drawString(this.font, "Source:",  rightLabelX, panelY + 96,  LABEL_COLOR, true);
        gfx.drawString(this.font, "Item ID:", rightLabelX, panelY + 118, LABEL_COLOR, true);

        int hintY = panelY + panelH - 46;
        if (errorMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(errorMessage),
                    panelX + panelW / 2, hintY, 0xFF5555);
        } else {
            String hint = isNew
                    ? "Template + Base + Addition → Result (count always 1)"
                    : "ID locked — delete & recreate to rename";
            gfx.drawCenteredString(this.font,
                    Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        }
    }
}
