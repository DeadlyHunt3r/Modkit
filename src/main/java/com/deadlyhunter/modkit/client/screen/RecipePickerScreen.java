package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RecipePickerScreen extends ModkitBaseScreen {

    private static final int ROWS_VISIBLE = 7;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 2;

    public record PickedRecipe(String namespace, String path, String typeLabel, String resultId) {}

    private final Consumer<PickedRecipe> onPick;

    private EditBox searchField;
    private final List<RecipeEntry> results = new ArrayList<>();
    private int scroll = 0;
    private String status = "Type an item id or name, then Search";

    private record RecipeEntry(String namespace, String path, String typeLabel, String resultId, String resultName) {}

    public RecipePickerScreen(net.minecraft.client.gui.screens.Screen parent, Consumer<PickedRecipe> onPick) {
        super(Component.literal("Find Recipe to Override"), parent);
        this.onPick = onPick;
        this.panelW = 320;
        this.panelH = 280;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int fieldX = panelX + 16;
        int fieldW = panelW - 90;

        searchField = new EditBox(this.font, fieldX, panelY + 24, fieldW, 18, Component.empty());
        searchField.setMaxLength(80);
        searchField.setHint(Component.literal("e.g. iron_ingot or minecraft:stick"));
        if (lastQuery != null) searchField.setValue(lastQuery);
        this.addRenderableWidget(searchField);

        this.addRenderableWidget(Button.builder(
                Component.literal("Search"),
                b -> runSearch()
        ).bounds(fieldX + fieldW + 6, panelY + 24, 50, 18).build());

        rebuildResultButtons();

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                b -> this.onClose()
        ).bounds(panelX + panelW / 2 - 50, panelY + panelH - 26, 100, 20).build());
    }

    private static String lastQuery = null;

    private void runSearch() {
        lastQuery = searchField.getValue().trim();
        scroll = 0;
        results.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            status = "Open a world first (recipes load with the world).";
            rebuildResultButtons();
            return;
        }

        String query = lastQuery.toLowerCase();
        if (query.isEmpty()) {
            status = "Type something to search.";
            rebuildResultButtons();
            return;
        }

        try {
            RecipeManager rm = mc.level.getRecipeManager();
            var registryAccess = mc.level.registryAccess();

            for (Recipe<?> recipe : rm.getRecipes()) {
                ItemStack result;
                try {
                    result = recipe.getResultItem(registryAccess);
                } catch (Throwable t) {
                    continue;
                }
                if (result == null || result.isEmpty()) continue;

                ResourceLocation resultLoc = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(result.getItem());
                if (resultLoc == null) continue;
                String resultId = resultLoc.toString();
                String resultName = result.getHoverName().getString();

                boolean matches = resultId.toLowerCase().contains(query)
                        || resultName.toLowerCase().contains(query);
                if (!matches) continue;

                ResourceLocation recipeId = recipe.getId();
                String typeLabel = typeLabel(recipe);
                results.add(new RecipeEntry(
                        recipeId.getNamespace(), recipeId.getPath(),
                        typeLabel, resultId, resultName));

                if (results.size() >= 200) break;
            }

            results.sort((a, b) -> {
                int c = a.namespace.compareTo(b.namespace);
                if (c != 0) return c;
                return a.path.compareTo(b.path);
            });

            status = results.isEmpty()
                    ? "No recipes found producing '" + lastQuery + "'."
                    : results.size() + " recipe(s) found";
        } catch (Throwable t) {
            Modkit.LOGGER.error("[Modkit] Recipe search failed", t);
            status = "Search error: " + t.getMessage();
        }

        rebuildResultButtons();
    }

    private static String typeLabel(Recipe<?> recipe) {
        RecipeType<?> type = recipe.getType();
        if (type == RecipeType.CRAFTING) {
            return recipe.isSpecial() ? "Special" : "Crafting";
        }
        if (type == RecipeType.SMELTING) return "Smelting";
        if (type == RecipeType.BLASTING) return "Blasting";
        if (type == RecipeType.SMOKING) return "Smoking";
        if (type == RecipeType.CAMPFIRE_COOKING) return "Campfire";
        if (type == RecipeType.STONECUTTING) return "Stonecutting";
        if (type == RecipeType.SMITHING) return "Smithing";
        ResourceLocation typeKey = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(type);
        return typeKey != null ? typeKey.getPath() : "Other";
    }

    private void rebuildResultButtons() {
        List<net.minecraft.client.gui.components.Renderable> toRemove = new ArrayList<>();
        this.clearWidgets();

        int fieldX = panelX + 16;
        int fieldW = panelW - 90;
        searchField = new EditBox(this.font, fieldX, panelY + 24, fieldW, 18, Component.empty());
        searchField.setMaxLength(80);
        searchField.setHint(Component.literal("e.g. iron_ingot or minecraft:stick"));
        if (lastQuery != null) searchField.setValue(lastQuery);
        this.addRenderableWidget(searchField);

        this.addRenderableWidget(Button.builder(
                Component.literal("Search"),
                b -> runSearch()
        ).bounds(fieldX + fieldW + 6, panelY + 24, 50, 18).build());

        int listX = panelX + 16;
        int listW = panelW - 32;
        int listY = panelY + 52;

        int rowCount = Math.min(ROWS_VISIBLE, results.size() - scroll);
        for (int i = 0; i < rowCount; i++) {
            int idx = scroll + i;
            RecipeEntry e = results.get(idx);
            int rowY = listY + i * (ROW_H + ROW_GAP);

            String label = "§7[" + e.typeLabel + "]§r " + truncate(e.path, 30);
            Button rowBtn = Button.builder(
                    Component.literal(label),
                    b -> {
                        onPick.accept(new PickedRecipe(e.namespace, e.path, e.typeLabel, e.resultId));
                    }
            ).bounds(listX, rowY, listW - 20, ROW_H).build();
            rowBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal(e.namespace + ":" + e.path + "\nResult: " + e.resultName)));
            this.addRenderableWidget(rowBtn);
        }

        if (results.size() > ROWS_VISIBLE) {
            int sX = listX + listW - 18;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) { scroll--; rebuildResultButtons(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (scroll + ROWS_VISIBLE < results.size()) { scroll++; rebuildResultButtons(); } })
                    .bounds(sX, listY + (ROWS_VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + ROWS_VISIBLE < results.size();
            this.addRenderableWidget(down);
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                b -> this.onClose()
        ).bounds(panelX + panelW / 2 - 50, panelY + panelH - 26, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (results.size() > ROWS_VISIBLE) {
            int ns = scroll - (int) Math.signum(delta);
            ns = Math.max(0, Math.min(results.size() - ROWS_VISIBLE, ns));
            if (ns != scroll) { scroll = ns; rebuildResultButtons(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (results.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.literal(status).withStyle(ChatFormatting.GRAY),
                    panelX + panelW / 2, panelY + panelH - 46, 0xFFFFFF);
        } else {
            gfx.drawString(this.font,
                    Component.literal(status).withStyle(ChatFormatting.GRAY),
                    panelX + 16, panelY + panelH - 42, 0xFFFFFF);
        }
    }
}
