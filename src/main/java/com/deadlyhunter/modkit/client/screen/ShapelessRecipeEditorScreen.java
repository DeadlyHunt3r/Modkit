package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.content.recipe.RecipeDefinition;
import com.deadlyhunter.modkit.content.recipe.RecipeDefinition.Ingredient;
import net.neoforged.neoforge.network.PacketDistributor;
import com.deadlyhunter.modkit.network.SaveRecipePacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;


public class ShapelessRecipeEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int SLOT_SIZE = 26;
    private static final int SLOT_GAP = 2;
    private final RecipeListScreen listParent;
    private final String modName;
    private final RecipeDefinition def;
    private final boolean isNew;
    private final java.util.function.Consumer<RecipeDefinition> overrideCallback;
    private final net.minecraft.client.gui.screens.Screen returnTo;
    private final Ingredient[] slots = new Ingredient[9];
    private EditBox idField;
    private EditBox displayNameField;
    private CycleButton<String> resultSourceBtn;
    private EditBox resultItemField;
    private EditBox resultCountField;
    private String errorMessage = null;

    public ShapelessRecipeEditorScreen(RecipeListScreen parent, String modName,
                                        RecipeDefinition def, boolean isNew) {
        super(Component.literal(isNew ? "New Shapeless Recipe" : "Edit: " + def.id), parent);
        this.listParent = parent;
        this.modName = modName;
        this.def = def;
        this.isNew = isNew;
        this.overrideCallback = null;
        this.returnTo = null;
        this.panelW = 320;
        this.panelH = 260;
        this.def.type = "shapeless";
        decodeListIntoSlots();
    }

    public ShapelessRecipeEditorScreen(net.minecraft.client.gui.screens.Screen returnTo, String modName,
                                        RecipeDefinition def,
                                        java.util.function.Consumer<RecipeDefinition> overrideCallback) {
        super(Component.literal("Replacement: Shapeless"), returnTo);
        this.listParent = null;
        this.modName = modName;
        this.def = def;
        this.isNew = false;
        this.overrideCallback = overrideCallback;
        this.returnTo = returnTo;
        this.panelW = 320;
        this.panelH = 260;
        this.def.type = "shapeless";
        decodeListIntoSlots();
    }

    public RecipeListScreen getListParent() { return listParent; }

    private void decodeListIntoSlots() {
        for (int i = 0; i < 9; i++) slots[i] = null;
        if (def.ingredientList == null) return;
        int idx = 0;
        for (Ingredient ing : def.ingredientList) {
            if (idx >= 9) break;
            if (ing == null || ing.isEmpty()) continue;
            slots[idx++] = new Ingredient(ing.source, ing.id);
        }
    }

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

        boolean overrideMode = overrideCallback != null;

        idField = new EditBox(this.font, leftPanelX + 50, panelY + 26, panelW - 80, 16,
                Component.literal("id"));
        idField.setMaxLength(40);
        idField.setValue(def.id != null ? def.id : "");
        if (!isNew) idField.setEditable(false);
        idField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_]*"));
        if (!overrideMode) this.addRenderableWidget(idField);

        displayNameField = new EditBox(this.font, leftPanelX + 50, panelY + 48, panelW - 80, 16,
                Component.literal("display"));
        displayNameField.setMaxLength(64);
        displayNameField.setValue(def.displayName != null ? def.displayName : "");
        if (!overrideMode) this.addRenderableWidget(displayNameField);

        int gridLeft = leftPanelX + 6;
        int gridTop = panelY + 78;
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            int sx = gridLeft + col * (SLOT_SIZE + SLOT_GAP);
            int sy = gridTop + row * (SLOT_SIZE + SLOT_GAP);
            final int slotIndex = i;
            Ingredient cur = slots[i];
            String label = cur == null ? "+" : "✓";

            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> openSlotPicker(slotIndex)
            ).bounds(sx, sy, SLOT_SIZE, SLOT_SIZE).build());
        }

        int resultY = panelY + 78;

        resultSourceBtn = CycleButton.<String>builder(s ->
                        Component.literal("mine".equals(s) ? "My Item" : "Custom"))
                .withValues("mine", "other")
                .withInitialValue(def.resultSource != null ? def.resultSource : "mine")
                .displayOnlyValue()
                .create(rightPanelX + 48, resultY + 14, fieldW - 48, 18, Component.literal(""));
        this.addRenderableWidget(resultSourceBtn);

        resultItemField = new EditBox(this.font, rightPanelX, resultY + 50, fieldW, 18,
                Component.literal("result"));
        resultItemField.setMaxLength(80);
        resultItemField.setValue(def.resultItem != null ? def.resultItem : "");
        resultItemField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_:/]*"));
        this.addRenderableWidget(resultItemField);

        resultCountField = new EditBox(this.font, rightPanelX, resultY + 92, fieldW, 18,
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

        if (!isNew && overrideCallback == null) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Delete").withStyle(ChatFormatting.RED),
                    btn -> this.minecraft.setScreen(
                            new ConfirmDeleteRecipeScreen(this, modName, def.id, def.displayName))
            ).bounds(panelX + 12, footerY, 60, 20).build());
        }
    }

    private void openSlotPicker(int slotIndex) {
        Ingredient current = slots[slotIndex];
        this.minecraft.setScreen(new IngredientPickerScreen(this, modName, current, true,
                result -> {
                    if (result == null || result.isEmpty()) {
                        slots[slotIndex] = null;
                    } else {
                        slots[slotIndex] = result;
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

        def.id = newId;
        def.displayName = newDisplay.isEmpty() ? newId : newDisplay;
        def.resultSource = resultSourceBtn.getValue();
        def.resultItem = resultItemField.getValue().trim();
        def.resultCount = count;

        List<Ingredient> list = new ArrayList<>();
        for (Ingredient s : slots) {
            if (s != null && !s.isEmpty()) list.add(s);
        }
        if (list.isEmpty()) {
            errorMessage = "Add at least one ingredient.";
            return;
        }
        def.ingredientList = list;

        String err = def.validate();
        if (err != null) { errorMessage = err; return; }

        if (overrideCallback != null) {
            overrideCallback.accept(def);
            this.minecraft.setScreen(returnTo);
            return;
        }

        String json = GSON.toJson(def);
        PacketDistributor.sendToServer(new SaveRecipePacket(modName, def.id, json));
        listParent.onRecipeChanged();
        this.minecraft.setScreen(listParent);
    }
    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 16;
        if (overrideCallback == null) {
            gfx.drawString(this.font, "ID",      labelX, panelY + 30, LABEL_COLOR, true);
            gfx.drawString(this.font, "Display", labelX, panelY + 52, LABEL_COLOR, true);
        } else {
            gfx.drawString(this.font,
                    Component.literal("Replacement recipe").withStyle(ChatFormatting.GRAY),
                    labelX, panelY + 38, 0xFFFFFF);
        }
        gfx.drawString(this.font, "Ingredients (any order):",
                labelX, panelY + 68, LABEL_COLOR, true);

        int rightLabelX = panelX + panelW / 2 + 8;
        gfx.drawString(this.font, "Result:",   rightLabelX, panelY + 78,  LABEL_COLOR, true);
        gfx.drawString(this.font, "Source:",   rightLabelX, panelY + 96,  LABEL_COLOR, true);
        gfx.drawString(this.font, "Item ID:",  rightLabelX, panelY + 116, LABEL_COLOR, true);
        gfx.drawString(this.font, "Count:",    rightLabelX, panelY + 158, LABEL_COLOR, true);


        int filled = 0;
        for (Ingredient s : slots) if (s != null && !s.isEmpty()) filled++;
        gfx.drawCenteredString(this.font,
                Component.literal(filled + "/9 slots used").withStyle(ChatFormatting.GRAY),
                panelX + 16 + 6 + (SLOT_SIZE + SLOT_GAP) * 3 / 2, panelY + 78 + 3 * (SLOT_SIZE + SLOT_GAP) + 4,
                0xFFFFFF);

        int hintY = panelY + panelH - 46;
        if (errorMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(errorMessage),
                    panelX + panelW / 2, hintY, 0xFF5555);
        } else if (!isNew) {
            gfx.drawCenteredString(this.font,
                    Component.literal("ID locked — delete & recreate to rename")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        }
    }
}
