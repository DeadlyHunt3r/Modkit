package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.content.recipe.RecipeDefinition.Ingredient;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class IngredientPickerScreen extends ModkitBaseScreen {

    private static final int TEXT = 0xFFFFFF;

    private final String modName;
    private final Ingredient initial;
    private final Consumer<Ingredient> onApply;
    private final boolean allowEmpty;

    private final Set<String> ownItemIds = new HashSet<>();

    private CycleButton<String> sourceBtn;
    private EditBox idField;
    private String errorMessage = null;

    public IngredientPickerScreen(Screen parent, String modName,
                                   Ingredient initial, boolean allowEmpty,
                                   Consumer<Ingredient> onApply) {
        super(Component.literal("Pick Item"), parent);
        this.modName = modName;
        this.initial = initial != null ? initial : new Ingredient("mine", "");
        this.allowEmpty = allowEmpty;
        this.onApply = onApply;
        this.panelW = 260;
        this.panelH = 165;
    }

    @Override
    protected void init() {
        super.init();
        loadOwnItemIds();

        int fieldX = panelX + 90;
        int fieldW = 150;

        sourceBtn = CycleButton.<String>builder(s ->
                        Component.literal("mine".equals(s) ? "My Item" : "Custom Item"))
                .withValues("mine", "other")
                .withInitialValue(initial.source != null ? initial.source : "mine")
                .displayOnlyValue()
                .create(fieldX, panelY + 36, fieldW, 18, Component.literal(""));
        this.addRenderableWidget(sourceBtn);

        idField = new EditBox(this.font, fieldX, panelY + 60, fieldW, 18,
                Component.literal("id"));
        idField.setMaxLength(80);
        idField.setValue(initial.id != null ? initial.id : "");
        idField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_:/]*"));
        this.addRenderableWidget(idField);

        int footerY = panelY + panelH - 30;
        int centerX = panelX + panelW / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("Apply"),
                btn -> tryApply()
        ).bounds(centerX - 110, footerY, 65, 20).build());

        if (allowEmpty) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Clear"),
                    btn -> {
                        onApply.accept(new Ingredient("mine", ""));
                        this.minecraft.setScreen(parent);
                    }
            ).bounds(centerX - 40, footerY, 50, 20).build());
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose()
        ).bounds(centerX + 15, footerY, 95, 20).build());
    }

    private void loadOwnItemIds() {
        ownItemIds.clear();
        Path dir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("items");
        if (!Files.isDirectory(dir)) return;
        Gson gson = new Gson();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            ItemDefinition d = gson.fromJson(Files.readString(p), ItemDefinition.class);
                            if (d != null && d.id != null) ownItemIds.add(d.id);
                        } catch (Exception ignored) {}
                    });
        } catch (IOException ignored) {}
    }

    private void tryApply() {
        String source = sourceBtn.getValue();
        String id = idField.getValue().trim();
        if (id.isEmpty()) {
            errorMessage = "Item id cannot be empty (use Clear instead).";
            return;
        }

        Ingredient ing = new Ingredient(source, id);

        if ("mine".equals(source)) {
            if (id.contains(":")) {
                errorMessage = "My Item: don't include namespace (just 'fire_essence')";
                return;
            }
            if (!ownItemIds.contains(id)) {
                errorMessage = "No item '" + id + "' in workspace. Available: "
                        + (ownItemIds.isEmpty() ? "(none)" : String.join(", ", ownItemIds));
                return;
            }
        } else {
            if (!id.contains(":")) {
                errorMessage = "Custom Item: full id required (e.g. minecraft:diamond)";
                return;
            }
        }

        String err = ing.validate();
        if (err != null) { errorMessage = err; return; }

        onApply.accept(ing);
        this.minecraft.setScreen(parent);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 18;
        gfx.drawString(this.font, "Source", labelX, panelY + 40, TEXT, true);
        gfx.drawString(this.font, "Item ID", labelX, panelY + 64, TEXT, true);

        int hintY = panelY + panelH - 50;
        if (errorMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(errorMessage),
                    panelX + panelW / 2, hintY, 0xFF5555);
        } else {
            String hint = "mine".equals(sourceBtn != null ? sourceBtn.getValue() : "mine")
                    ? "Just your item's id, e.g. fire_essence"
                    : "Full id with namespace, e.g. minecraft:diamond";
            gfx.drawCenteredString(this.font,
                    Component.literal(hint).withStyle(ChatFormatting.GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        }
    }
}
