package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.content.recipe.RecipeDefinition;
import com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition;
import com.deadlyhunter.modkit.network.ModNetworking;
import com.deadlyhunter.modkit.network.SaveOverridePacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class OverrideEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int LABEL_COLOR = 0xFFFFFF;

    private final OverrideListScreen listParent;
    private final String modName;
    private final RecipeOverrideDefinition def;
    private final boolean isNew;

    private EditBox idField;
    private CycleButton<String> modeBtn;
    private String errorMessage = null;

    public OverrideEditorScreen(OverrideListScreen parent, String modName,
                                 RecipeOverrideDefinition def, boolean isNew) {
        super(Component.literal(isNew ? "New Override" : "Edit: " + def.id), parent);
        this.listParent = parent;
        this.modName = modName;
        this.def = def;
        this.isNew = isNew;
        this.panelW = 320;
        this.panelH = 260;
    }

    public OverrideListScreen getListParent() { return listParent; }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int fieldX = panelX + 110;
        int fieldW = panelW - 126;

        idField = new EditBox(this.font, fieldX, panelY + 26, fieldW, 18, Component.empty());
        idField.setMaxLength(48);
        idField.setValue(def.id != null ? def.id : "");
        if (!isNew) idField.setEditable(false);
        idField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_]*"));
        this.addRenderableWidget(idField);

        modeBtn = CycleButton.<String>builder(s ->
                        Component.literal("disable".equals(s) ? "Disable" : "Replace"))
                .withValues("disable", "replace")
                .withInitialValue(def.mode != null ? def.mode : "disable")
                .displayOnlyValue()
                .create(fieldX, panelY + 50, fieldW, 18, Component.literal(""),
                        (btn, value) -> { def.mode = value; rebuild(); });
        this.addRenderableWidget(modeBtn);

        Button targetBtn = Button.builder(
                Component.literal(def.targetRecipe == null || def.targetRecipe.isBlank()
                        ? "Choose recipe..." : "Change recipe..."),
                b -> openRecipePicker()
        ).bounds(fieldX, panelY + 78, fieldW, 18).build();
        this.addRenderableWidget(targetBtn);

        if ("replace".equals(def.mode)) {
            if (def.replacement == null) def.replacement = blankReplacement(null);
            CycleButton<String> typeBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                    .withValues("shaped", "shapeless", "smelting", "blasting", "smoking", "stonecutting")
                    .withInitialValue(validReplacementType(def.replacement.type))
                    .displayOnlyValue()
                    .create(fieldX, panelY + 124, fieldW, 18, Component.literal(""),
                            (btn, value) -> def.replacement.type = value);
            this.addRenderableWidget(typeBtn);

            Button replBtn = Button.builder(
                    Component.literal(hasReplacementContent() ? "Edit recipe ✓" : "Set up recipe..."),
                    b -> openReplacementEditor()
            ).bounds(fieldX, panelY + 148, fieldW, 18).build();
            this.addRenderableWidget(replBtn);
        }

        int footerY = panelY + panelH - 30;
        int btnW = 70;
        int gap = 6;
        int centerX = panelX + panelW / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("Save"),
                b -> trySave()
        ).bounds(centerX - btnW - gap / 2, footerY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                b -> this.onClose()
        ).bounds(centerX + gap / 2, footerY, btnW, 20).build());

        if (!isNew) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Delete").withStyle(ChatFormatting.RED),
                    b -> this.minecraft.setScreen(
                            new ConfirmDeleteOverrideScreen(this, modName, def.id, def.displayName))
            ).bounds(panelX + 12, footerY, 60, 20).build());
        }
    }

    private void rebuild() {
        captureBasics();
        this.clearWidgets();
        this.init();
    }

    private void captureBasics() {
        if (idField != null) def.id = idField.getValue().trim();
        if (modeBtn != null) def.mode = modeBtn.getValue();
    }

    private void openRecipePicker() {
        captureBasics();
        this.minecraft.setScreen(new RecipePickerScreen(this, picked -> {
            def.targetNamespace = picked.namespace();
            def.targetRecipe = picked.path();
            if ("replace".equals(def.mode)) {
                if (def.replacement == null) {
                    def.replacement = blankReplacement(picked.resultId());
                } else if (def.replacement.resultItem == null || def.replacement.resultItem.isBlank()) {
                    def.replacement.resultSource = "other";
                    def.replacement.resultItem = picked.resultId();
                    if (def.replacement.resultCount <= 0) def.replacement.resultCount = 1;
                }
            }
            this.minecraft.setScreen(this);
        }));
    }

    private RecipeDefinition blankReplacement(String resultId) {
        RecipeDefinition r = new RecipeDefinition();
        r.type = "shaped";
        r.id = def.id + "_replacement";
        r.displayName = "Replacement";
        if (resultId != null && resultId.contains(":")) {
            r.resultSource = "other";
            r.resultItem = resultId;
        }
        r.resultCount = 1;
        return r;
    }

    private void openReplacementEditor() {
        captureBasics();
        if (def.replacement == null) def.replacement = blankReplacement(null);
        RecipeDefinition r = def.replacement;
        java.util.function.Consumer<RecipeDefinition> cb = result -> def.replacement = result;

        switch (validReplacementType(r.type)) {
            case "shapeless" ->
                    this.minecraft.setScreen(new ShapelessRecipeEditorScreen(this, modName, r, cb));
            case "smelting", "blasting", "smoking" ->
                    this.minecraft.setScreen(new SmeltingRecipeEditorScreen(this, modName, r, cb));
            case "stonecutting" ->
                    this.minecraft.setScreen(new StonecuttingRecipeEditorScreen(this, modName, r, cb));
            default ->
                    this.minecraft.setScreen(new ShapedRecipeEditorScreen(this, modName, r, cb));
        }
    }

    private static String validReplacementType(String t) {
        if (t == null) return "shaped";
        return switch (t) {
            case "shaped", "shapeless", "smelting", "blasting", "smoking", "stonecutting" -> t;
            default -> "shaped";
        };
    }

    private void trySave() {
        captureBasics();

        if (def.targetRecipe == null || def.targetRecipe.isBlank()) {
            errorMessage = "Choose a target recipe first.";
            return;
        }
        if ("replace".equals(def.mode) && def.replacement == null) {
            errorMessage = "Replace mode needs a replacement recipe.";
            return;
        }

        def.displayName = def.id;
        if ("replace".equals(def.mode) && def.replacement != null) {
            def.replacement.id = def.id + "_replacement";
        }

        String err = def.validate();
        if (err != null) { errorMessage = err; return; }

        String json = GSON.toJson(def);
        ModNetworking.CHANNEL.sendToServer(new SaveOverridePacket(modName, def.id, json));
        listParent.onOverrideChanged();
        this.minecraft.setScreen(listParent);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 16;
        gfx.drawString(this.font, "ID",      labelX, panelY + 30, LABEL_COLOR, true);
        gfx.drawString(this.font, "Mode",    labelX, panelY + 54, LABEL_COLOR, true);
        gfx.drawString(this.font, "Target",  labelX, panelY + 82, LABEL_COLOR, true);

        boolean replace = "replace".equals(def.mode);

        if (def.targetRecipe != null && !def.targetRecipe.isBlank()) {
            String targetText = "→ " + def.targetNamespace + ":" + truncate(def.targetRecipe, 34);
            gfx.drawString(this.font,
                    Component.literal(targetText).withStyle(ChatFormatting.GRAY),
                    labelX, panelY + 102, 0xFFFFFF);
        }

        if (replace) {
            gfx.drawString(this.font, "Type",   labelX, panelY + 128, LABEL_COLOR, true);
            gfx.drawString(this.font, "Recipe", labelX, panelY + 152, LABEL_COLOR, true);
        }

        int hintY = panelY + panelH - 48;
        if (errorMessage != null) {
            gfx.drawCenteredString(this.font, Component.literal(errorMessage),
                    panelX + panelW / 2, hintY, 0xFF5555);
        } else {
            String hint = "disable".equals(def.mode)
                    ? "Disable: removes the chosen recipe entirely"
                    : "Replace: swaps in your own recipe at same path";
            gfx.drawCenteredString(this.font,
                    Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        }
    }

    private boolean hasReplacementContent() {
        RecipeDefinition r = def.replacement;
        if (r == null) return false;
        if (r.resultItem != null && !r.resultItem.isBlank()) return true;
        if (r.ingredients != null && !r.ingredients.isEmpty()) return true;
        if (r.ingredientList != null && !r.ingredientList.isEmpty()) return true;
        if (r.input != null && !r.input.isEmpty()) return true;
        return false;
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
