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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ShapedRecipeEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int LABEL_COLOR = 0xFFFFFF;

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


    private static final int SLOT_SIZE = 26;
    private static final int SLOT_GAP = 2;

    public ShapedRecipeEditorScreen(RecipeListScreen parent, String modName,
                                     RecipeDefinition def, boolean isNew) {
        super(Component.literal(isNew ? "New Shaped Recipe" : "Edit: " + def.id), parent);
        this.listParent = parent;
        this.modName = modName;
        this.def = def;
        this.isNew = isNew;
        this.overrideCallback = null;
        this.returnTo = null;
        this.panelW = 320;
        this.panelH = 280;
        this.def.type = "shaped";
        decodePatternIntoSlots();
    }

    public ShapedRecipeEditorScreen(net.minecraft.client.gui.screens.Screen returnTo, String modName,
                                     RecipeDefinition def,
                                     java.util.function.Consumer<RecipeDefinition> overrideCallback) {
        super(Component.literal("Replacement: Shaped"), returnTo);
        this.listParent = null;
        this.modName = modName;
        this.def = def;
        this.isNew = false;
        this.overrideCallback = overrideCallback;
        this.returnTo = returnTo;
        this.panelW = 320;
        this.panelH = 280;
        this.def.type = "shaped";
        decodePatternIntoSlots();
    }

    public RecipeListScreen getListParent() { return listParent; }

    @Override
    protected void init() {
        super.init();
        rebuildWidgetsForEditor();
    }


    private void decodePatternIntoSlots() {
        for (int i = 0; i < 9; i++) slots[i] = null;
        if (def.pattern == null || def.ingredients == null) return;


        for (int r = 0; r < def.pattern.size() && r < 3; r++) {
            String row = def.pattern.get(r);
            if (row == null) continue;
            for (int c = 0; c < row.length() && c < 3; c++) {
                char ch = row.charAt(c);
                if (ch == ' ') continue;
                Ingredient ing = def.ingredients.get(String.valueOf(ch));
                if (ing != null && !ing.isEmpty()) {
                    slots[r * 3 + c] = new Ingredient(ing.source, ing.id);
                }
            }
        }
    }

    private void rebuildWidgetsForEditor() {
        this.clearWidgets();

        int leftPanelX = panelX + 16;
        int rightPanelX = panelX + panelW / 2 + 8;
        int fieldW = 130;


        int topY = panelY + 26;

        boolean overrideMode = overrideCallback != null;

        idField = new EditBox(this.font, leftPanelX + 50, topY, panelW - 80, 16,
                Component.literal("id"));
        idField.setMaxLength(40);
        idField.setValue(def.id != null ? def.id : "");
        if (!isNew) idField.setEditable(false);
        idField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_]*"));
        if (!overrideMode) this.addRenderableWidget(idField);

        displayNameField = new EditBox(this.font, leftPanelX + 50, topY + 22, panelW - 80, 16,
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
            String label = cur == null ? "+" : letterForIngredient(cur);

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


    private String letterForIngredient(Ingredient ing) {

        Map<String, Character> assigned = new LinkedHashMap<>();
        char nextLetter = 'X';
        for (Ingredient s : slots) {
            if (s == null || s.isEmpty()) continue;
            String key = s.source + "|" + s.id;
            if (!assigned.containsKey(key)) {
                assigned.put(key, nextLetter);
                nextLetter = nextLetter(nextLetter);
            }
        }
        Character c = assigned.get(ing.source + "|" + ing.id);
        return c != null ? String.valueOf(c) : "?";
    }


    private static char nextLetter(char c) {
        if (c == 'X') return 'Y';
        if (c == 'Y') return 'Z';
        if (c == 'Z') return 'A';
        if (c == 'W') return 'X';
        return (char) (c + 1);
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


        int minR = 3, maxR = -1, minC = 3, maxC = -1;
        for (int i = 0; i < 9; i++) {
            if (slots[i] == null) continue;
            int r = i / 3, c = i % 3;
            if (r < minR) minR = r;
            if (r > maxR) maxR = r;
            if (c < minC) minC = c;
            if (c > maxC) maxC = c;
        }
        if (maxR < 0) {
            errorMessage = "Pattern is empty — click slots to add ingredients.";
            return;
        }


        Map<String, Character> letterMap = new LinkedHashMap<>();
        char nextL = 'X';
        for (int i = 0; i < 9; i++) {
            if (slots[i] == null) continue;
            String key = slots[i].source + "|" + slots[i].id;
            if (!letterMap.containsKey(key)) {
                letterMap.put(key, nextL);
                nextL = nextLetter(nextL);
            }
        }


        List<String> pattern = new ArrayList<>();
        for (int r = minR; r <= maxR; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = minC; c <= maxC; c++) {
                Ingredient s = slots[r * 3 + c];
                if (s == null) {
                    row.append(' ');
                } else {
                    row.append(letterMap.get(s.source + "|" + s.id));
                }
            }
            pattern.add(row.toString());
        }
        def.pattern = pattern;


        Map<String, Ingredient> ingredientMap = new LinkedHashMap<>();
        for (Map.Entry<String, Character> e : letterMap.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            ingredientMap.put(String.valueOf(e.getValue()),
                    new Ingredient(parts[0], parts.length > 1 ? parts[1] : ""));
        }
        def.ingredients = ingredientMap;


        String err = def.validate();
        if (err != null) { errorMessage = err; return; }

        if (overrideCallback != null) {
            overrideCallback.accept(def);
            this.minecraft.setScreen(returnTo);
            return;
        }

        String json = GSON.toJson(def);
        ModNetworking.CHANNEL.sendToServer(new SaveRecipePacket(modName, def.id, json));
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


        gfx.drawString(this.font, "Pattern (click slots):",
                labelX, panelY + 68, LABEL_COLOR, true);


        int rightLabelX = panelX + panelW / 2 + 8;
        gfx.drawString(this.font, "Result:",  rightLabelX, panelY + 78,  LABEL_COLOR, true);
        gfx.drawString(this.font, "Source:",  rightLabelX, panelY + 96,  LABEL_COLOR, true);
        gfx.drawString(this.font, "Item ID:", rightLabelX, panelY + 116, LABEL_COLOR, true);
        gfx.drawString(this.font, "Count:",   rightLabelX, panelY + 158, LABEL_COLOR, true);


        Map<String, Character> letterMap = new LinkedHashMap<>();
        char nextL = 'X';
        for (Ingredient s : slots) {
            if (s == null || s.isEmpty()) continue;
            String key = s.source + "|" + s.id;
            if (!letterMap.containsKey(key)) {
                letterMap.put(key, nextL);
                nextL = nextLetter(nextL);
            }
        }
        int legendY = panelY + 192;
        gfx.drawString(this.font, "Legend:", labelX, legendY, LABEL_COLOR, true);
        int ly = legendY + 12;
        for (Map.Entry<String, Character> e : letterMap.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            String shown = parts[0] + ":" + (parts.length > 1 ? parts[1] : "");
            gfx.drawString(this.font, e.getValue() + " = " + shown, labelX + 6, ly, LABEL_COLOR, true);
            ly += 10;
            if (ly > panelY + panelH - 60) break;
        }


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
