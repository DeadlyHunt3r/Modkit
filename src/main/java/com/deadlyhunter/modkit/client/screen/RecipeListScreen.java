package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.recipe.RecipeDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class RecipeListScreen extends ModkitBaseScreen {

    private static final int ROWS_VISIBLE = 6;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;

    private final String modName;
    private final List<RecipeDefinition> recipes = new ArrayList<>();
    private int scroll = 0;

    public RecipeListScreen(ProjectScreen parent, String modName) {
        super(Component.literal("Recipes — " + modName), parent);
        this.modName = modName;
        this.panelW = 260;
        this.panelH = 276;
    }

    @Override
    protected void init() {
        super.init();
        loadRecipes();
        rebuildList();
    }

    private void loadRecipes() {
        recipes.clear();
        Path dir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("recipes");
        if (!Files.isDirectory(dir)) return;

        Gson gson = new Gson();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            RecipeDefinition def = gson.fromJson(Files.readString(p), RecipeDefinition.class);
                            if (def != null && def.validate() == null) recipes.add(def);
                        } catch (Exception ignored) {}
                    });
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list recipes", e);
        }
        recipes.sort(Comparator.comparing(d -> d.id));
    }

    private void rebuildList() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int listW = 200;
        int listX = centerX - listW / 2;
        int listY = panelY + 40;

        int rowCount = Math.min(ROWS_VISIBLE, recipes.size() - scroll);
        for (int i = 0; i < rowCount; i++) {
            int idx = scroll + i;
            RecipeDefinition def = recipes.get(idx);
            int rowY = listY + i * (ROW_H + ROW_GAP);

            String label = "§7[" + typeShort(def.type) + "]§r " + def.displayName + " §8(" + def.id + ")";

            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> openEditor(def, false)
            ).bounds(listX, rowY, listW, ROW_H).build());
        }

        if (recipes.size() > ROWS_VISIBLE) {
            int sX = listX + listW + 4;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) { scroll--; rebuildList(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (scroll + ROWS_VISIBLE < recipes.size()) { scroll++; rebuildList(); } })
                    .bounds(sX, listY + (ROWS_VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + ROWS_VISIBLE < recipes.size();
            this.addRenderableWidget(down);
        }

        int footerY = panelY + panelH - 96;
        int btnW = 72;
        int gap = 4;
        int row1X = centerX - (btnW * 3 + gap * 2) / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Shaped"),
                btn -> openEditor(blank("shaped"), true)
        ).bounds(row1X, footerY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Shapeless"),
                btn -> openEditor(blank("shapeless"), true)
        ).bounds(row1X + btnW + gap, footerY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Smelting"),
                btn -> openEditor(blank("smelting"), true)
        ).bounds(row1X + 2 * (btnW + gap), footerY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Blasting"),
                btn -> openEditor(blank("blasting"), true)
        ).bounds(row1X, footerY + 22, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Smoking"),
                btn -> openEditor(blank("smoking"), true)
        ).bounds(row1X + btnW + gap, footerY + 22, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Stonecut."),
                btn -> openEditor(blank("stonecutting"), true)
        ).bounds(row1X + 2 * (btnW + gap), footerY + 22, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Smithing"),
                btn -> openEditor(blank("smithing"), true)
        ).bounds(row1X + btnW + gap, footerY + 44, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX - 50, footerY + 70, 100, 20).build());
    }

    private void openEditor(RecipeDefinition def, boolean isNew) {
        switch (def.type) {
            case "shaped"       -> this.minecraft.setScreen(new ShapedRecipeEditorScreen(this, modName, def, isNew));
            case "shapeless"    -> this.minecraft.setScreen(new ShapelessRecipeEditorScreen(this, modName, def, isNew));
            case "smelting", "blasting", "smoking" ->
                    this.minecraft.setScreen(new SmeltingRecipeEditorScreen(this, modName, def, isNew));
            case "stonecutting" -> this.minecraft.setScreen(new StonecuttingRecipeEditorScreen(this, modName, def, isNew));
            case "smithing"     -> this.minecraft.setScreen(new SmithingRecipeEditorScreen(this, modName, def, isNew));
        }
    }

    private RecipeDefinition blank(String type) {
        RecipeDefinition d = new RecipeDefinition();
        d.type = type;
        d.id = "new_" + type + "_recipe";
        d.displayName = "New " + cap(type) + " Recipe";
        d.resultCount = 1;
        return d;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (recipes.size() > ROWS_VISIBLE) {
            int newScroll = scroll - (int) Math.signum(delta);
            newScroll = Math.max(0, Math.min(recipes.size() - ROWS_VISIBLE, newScroll));
            if (newScroll != scroll) { scroll = newScroll; rebuildList(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (recipes.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("No recipes yet — pick a type below").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 80, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal(recipes.size() + " recipe(s)").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 24, 0xFFFFFF);
        }
    }

    public void onRecipeChanged() {
        loadRecipes();
        rebuildList();
    }

    private static String typeShort(String type) {
        return switch (type) {
            case "shaped"       -> "Shaped";
            case "shapeless"    -> "Shapeless";
            case "smelting"     -> "Smelt";
            case "blasting"     -> "Blast";
            case "smoking"      -> "Smoke";
            case "stonecutting" -> "Stonecut";
            case "smithing"     -> "Smith";
            default             -> type;
        };
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
