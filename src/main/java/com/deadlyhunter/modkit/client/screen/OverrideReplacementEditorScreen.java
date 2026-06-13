package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.content.recipe.RecipeDefinition;
import com.deadlyhunter.modkit.content.recipe.RecipeDefinition.Ingredient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class OverrideReplacementEditorScreen extends ModkitBaseScreen {

    private static final int LABEL_COLOR = 0xFFFFFF;

    private final OverrideEditorScreen editorParent;
    private final String modName;
    private final RecipeDefinition recipe;

    private CycleButton<String> typeBtn;
    private CycleButton<String> resultSourceBtn;
    private EditBox resultItemField;
    private EditBox resultCountField;
    private String errorMessage = null;

    public OverrideReplacementEditorScreen(OverrideEditorScreen parent, String modName,
                                            com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition ov) {
        super(Component.literal("Replacement Recipe"), parent);
        this.editorParent = parent;
        this.modName = modName;
        this.recipe = ov.replacement;
        this.panelW = 320;
        this.panelH = 260;
        if (this.recipe.ingredientList == null) this.recipe.ingredientList = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int fieldX = panelX + 110;
        int fieldW = panelW - 126;

        typeBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                .withValues("shaped", "shapeless", "smelting", "blasting", "smoking", "stonecutting")
                .withInitialValue(validType(recipe.type))
                .displayOnlyValue()
                .create(fieldX, panelY + 26, fieldW, 18, Component.literal(""),
                        (btn, value) -> { recipe.type = value; rebuild(); });
        this.addRenderableWidget(typeBtn);

        resultSourceBtn = CycleButton.<String>builder(s ->
                        Component.literal("mine".equals(s) ? "My Item" : "Custom"))
                .withValues("mine", "other")
                .withInitialValue(recipe.resultSource != null ? recipe.resultSource : "other")
                .displayOnlyValue()
                .create(fieldX, panelY + 50, fieldW, 18, Component.literal(""));
        this.addRenderableWidget(resultSourceBtn);

        resultItemField = new EditBox(this.font, fieldX, panelY + 74, fieldW, 18, Component.empty());
        resultItemField.setMaxLength(80);
        resultItemField.setValue(recipe.resultItem != null ? recipe.resultItem : "");
        resultItemField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_:/]*"));
        this.addRenderableWidget(resultItemField);

        boolean cooking = isCooking(recipe.type);
        if (!cooking && !"stonecutting".equals(recipe.type)) {
            resultCountField = new EditBox(this.font, fieldX, panelY + 98, 50, 18, Component.empty());
            resultCountField.setMaxLength(2);
            resultCountField.setValue(String.valueOf(Math.max(1, recipe.resultCount)));
            resultCountField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,2}"));
            this.addRenderableWidget(resultCountField);
        }

        int ingY = panelY + 128;
        int maxIng = "shaped".equals(recipe.type) || "shapeless".equals(recipe.type) ? 9 : 1;
        int shown = Math.min(recipe.ingredientList.size(), maxIng);
        for (int i = 0; i < shown; i++) {
            final int index = i;
            Ingredient ing = recipe.ingredientList.get(i);
            String lbl = (ing == null || ing.isEmpty()) ? "+ ingredient" : "✓ " + shortIng(ing);
            int col = i % 3;
            int row = i / 3;
            Button b = Button.builder(Component.literal(lbl),
                    btn -> openIngredientPicker(index))
                    .bounds(panelX + 16 + col * 98, ingY + row * 22, 94, 18).build();
            this.addRenderableWidget(b);
        }
        if (recipe.ingredientList.size() < maxIng) {
            int i = recipe.ingredientList.size();
            int col = i % 3;
            int row = i / 3;
            this.addRenderableWidget(Button.builder(
                    Component.literal("+ Add"),
                    btn -> { recipe.ingredientList.add(new Ingredient("other", "")); rebuild(); })
                    .bounds(panelX + 16 + col * 98, ingY + row * 22, 94, 18).build());
        }

        int footerY = panelY + panelH - 30;
        int centerX = panelX + panelW / 2;
        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                b -> applyAndClose()
        ).bounds(centerX - 103, footerY, 100, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                b -> this.onClose()
        ).bounds(centerX + 3, footerY, 100, 20).build());
    }

    private void rebuild() {
        captureResult();
        this.clearWidgets();
        this.init();
    }

    private void captureResult() {
        if (resultSourceBtn != null) recipe.resultSource = resultSourceBtn.getValue();
        if (resultItemField != null) recipe.resultItem = resultItemField.getValue().trim();
        if (resultCountField != null && !resultCountField.getValue().isEmpty()) {
            try { recipe.resultCount = Math.max(1, Integer.parseInt(resultCountField.getValue())); }
            catch (NumberFormatException ignored) {}
        }
    }

    private void openIngredientPicker(int index) {
        captureResult();
        Ingredient current = recipe.ingredientList.get(index);
        if (current == null) current = new Ingredient("other", "");
        this.minecraft.setScreen(new IngredientPickerScreen(this, modName, current, true,
                result -> {
                    if (result == null || result.isEmpty()) {
                        recipe.ingredientList.remove(index);
                    } else {
                        recipe.ingredientList.set(index, result);
                    }
                }));
    }

    private void applyAndClose() {
        captureResult();

        if (recipe.resultItem == null || recipe.resultItem.isBlank()) {
            errorMessage = "Set a result item first.";
            return;
        }
        recipe.ingredientList.removeIf(i -> i == null || i.isEmpty());

        if ("shaped".equals(recipe.type)) {
            buildShapedPatternFromList();
        } else if (isCooking(recipe.type) || "stonecutting".equals(recipe.type)) {
            if (recipe.ingredientList.isEmpty()) {
                errorMessage = "Set an input item first.";
                return;
            }
            recipe.input = recipe.ingredientList.get(0);
        }

        this.minecraft.setScreen(editorParent);
    }

    private void buildShapedPatternFromList() {
        List<Ingredient> list = recipe.ingredientList;
        recipe.ingredients.clear();
        List<String> pattern = new ArrayList<>();
        char key = 'A';
        int idx = 0;
        for (int row = 0; row < 3; row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < 3; col++) {
                if (idx < list.size()) {
                    char c = key++;
                    recipe.ingredients.put(String.valueOf(c), list.get(idx));
                    sb.append(c);
                    idx++;
                } else {
                    sb.append(' ');
                }
            }
            pattern.add(sb.toString());
        }
        recipe.pattern = pattern;
    }

    private static boolean isCooking(String type) {
        return "smelting".equals(type) || "blasting".equals(type) || "smoking".equals(type);
    }

    private static String validType(String t) {
        if (t == null) return "shaped";
        return switch (t) {
            case "shaped", "shapeless", "smelting", "blasting", "smoking", "stonecutting" -> t;
            default -> "shaped";
        };
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String shortIng(Ingredient ing) {
        String id = ing.id == null ? "" : ing.id;
        if (id.length() > 8) id = id.substring(0, 7) + "…";
        return id;
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 16;
        gfx.drawString(this.font, "Type",    labelX, panelY + 30, LABEL_COLOR, true);
        gfx.drawString(this.font, "Result",  labelX, panelY + 54, LABEL_COLOR, true);
        gfx.drawString(this.font, "Item ID", labelX, panelY + 78, LABEL_COLOR, true);
        if (resultCountField != null) {
            gfx.drawString(this.font, "Count", labelX, panelY + 102, LABEL_COLOR, true);
        }
        gfx.drawString(this.font, isCooking(recipe.type) || "stonecutting".equals(recipe.type)
                        ? "Input:" : "Ingredients:",
                labelX, panelY + 116, 0xAAAAAA, true);

        if (errorMessage != null) {
            gfx.drawCenteredString(this.font, Component.literal(errorMessage),
                    panelX + panelW / 2, panelY + panelH - 46, 0xFF5555);
        }
    }
}
