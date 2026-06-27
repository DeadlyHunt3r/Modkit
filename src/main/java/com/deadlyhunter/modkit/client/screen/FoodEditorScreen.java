package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.content.item.ItemDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FoodEditorScreen extends ModkitBaseScreen {

    private static final int ROW_H = 18;
    private static final int ROW_GAP = 4;
    private static final int ROW_STEP = ROW_H + ROW_GAP;
    private static final int MAX_EFFECTS = 6;

    private final ItemEditorScreen editorParent;
    private final String modName;
    private final ItemDefinition def;

    private Checkbox isFoodBox;
    private EditBox nutritionField;
    private EditBox saturationField;
    private Checkbox alwaysEatBox;
    private Checkbox fastEatBox;

    private final List<EffectRow> effectRows = new ArrayList<>();

    private String message = null;
    private boolean forceFoodChecked = false;
    private int effectsHeaderY = 0;

    private static class EffectRow {
        EditBox effectField;
        EditBox durationField;
        EditBox amplifierField;
        EditBox chanceField;
    }

    public FoodEditorScreen(ItemEditorScreen parent, String modName, ItemDefinition def) {
        super(Component.literal("Food — " + def.id), parent);
        this.editorParent = parent;
        this.modName = modName;
        this.def = def;
        this.panelW = 340;
        this.panelH = 340;
    }

    @Override
    protected void init() {
        super.init();
        effectRows.clear();

        boolean isFood = def.food != null || forceFoodChecked;
        ItemDefinition.FoodData food = def.food != null ? def.food : new ItemDefinition.FoodData();

        int labelW = 110;
        int fieldX = panelX + 130;
        int y = panelY + 28;

        isFoodBox = checkbox(panelX + 18, y, 200, ROW_H,
                Component.literal("This item is food"), isFood);
        this.addRenderableWidget(isFoodBox);
        y += ROW_STEP + 2;

        nutritionField = new EditBox(this.font, fieldX, y, 80, ROW_H, Component.empty());
        nutritionField.setMaxLength(2);
        nutritionField.setValue(String.valueOf(food.nutrition));
        nutritionField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,2}"));
        nutritionField.setTooltip(Tooltip.create(Component.literal("Hunger points restored (0-20). Bread=5")));
        this.addRenderableWidget(nutritionField);
        y += ROW_STEP;

        saturationField = new EditBox(this.font, fieldX, y, 80, ROW_H, Component.empty());
        saturationField.setMaxLength(5);
        saturationField.setValue(formatFloat(food.saturation));
        saturationField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,2}(\\.\\d{0,2})?"));
        saturationField.setTooltip(Tooltip.create(Component.literal("Saturation modifier. Bread=0.6, Steak=0.8")));
        this.addRenderableWidget(saturationField);
        y += ROW_STEP;

        alwaysEatBox = checkbox(fieldX, y, 200, ROW_H,
                Component.literal("Can always eat"), food.canAlwaysEat);
        alwaysEatBox.setTooltip(Tooltip.create(Component.literal("Edible even with full hunger (like Golden Apple)")));
        this.addRenderableWidget(alwaysEatBox);
        y += ROW_STEP;

        fastEatBox = checkbox(fieldX, y, 200, ROW_H,
                Component.literal("Fast to eat"), food.fastEat);
        fastEatBox.setTooltip(Tooltip.create(Component.literal("Eats quickly (like Dried Kelp)")));
        this.addRenderableWidget(fastEatBox);
        y += ROW_STEP + 10;

        int effectX = panelX + 18;
        effectsHeaderY = y;
        y += 14;

        int colEffect = effectX;
        int colDur = effectX + 150;
        int colAmp = effectX + 210;
        int colChance = effectX + 250;
        int colDel = effectX + 296;

        List<ItemDefinition.FoodEffect> effects = food.effects != null ? food.effects : new ArrayList<>();
        for (int i = 0; i < effects.size() && i < MAX_EFFECTS; i++) {
            ItemDefinition.FoodEffect e = effects.get(i);
            addEffectRow(colEffect, colDur, colAmp, colChance, colDel, y,
                    e.effect, e.duration, e.amplifier, e.chance);
            y += ROW_STEP;
        }

        if (effectRows.size() < MAX_EFFECTS) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("+ Add Effect"),
                    b -> addAndRefresh()
            ).bounds(effectX, y, 100, ROW_H).build());
        }

        int footerY = panelY + panelH - 30;
        int centerX = panelX + panelW / 2;
        this.addRenderableWidget(Button.builder(
                Component.literal("Save"),
                b -> save()
        ).bounds(centerX - 103, footerY, 100, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                b -> this.onClose()
        ).bounds(centerX + 3, footerY, 100, 20).build());
    }

    private void addEffectRow(int colEffect, int colDur, int colAmp, int colChance, int colDel, int y,
                              String effect, int duration, int amplifier, float chance) {
        EffectRow row = new EffectRow();

        row.effectField = new EditBox(this.font, colEffect, y, 146, ROW_H, Component.empty());
        row.effectField.setMaxLength(60);
        row.effectField.setValue(effect);
        row.effectField.setHint(Component.literal("minecraft:..."));
        row.effectField.setTooltip(Tooltip.create(Component.literal("Effect id, e.g. minecraft:regeneration")));
        this.addRenderableWidget(row.effectField);

        row.durationField = new EditBox(this.font, colDur, y, 56, ROW_H, Component.empty());
        row.durationField.setMaxLength(6);
        row.durationField.setValue(String.valueOf(duration));
        row.durationField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,6}"));
        row.durationField.setTooltip(Tooltip.create(Component.literal("Duration in ticks (20 = 1 sec)")));
        this.addRenderableWidget(row.durationField);

        row.amplifierField = new EditBox(this.font, colAmp, y, 36, ROW_H, Component.empty());
        row.amplifierField.setMaxLength(3);
        row.amplifierField.setValue(String.valueOf(amplifier));
        row.amplifierField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}"));
        row.amplifierField.setTooltip(Tooltip.create(Component.literal("Amplifier (0 = level I)")));
        this.addRenderableWidget(row.amplifierField);

        row.chanceField = new EditBox(this.font, colChance, y, 42, ROW_H, Component.empty());
        row.chanceField.setMaxLength(4);
        row.chanceField.setValue(formatFloat(chance));
        row.chanceField.setFilter(s -> s.isEmpty() || s.matches("(0(\\.\\d{0,2})?|1(\\.0{0,2})?)?"));
        row.chanceField.setTooltip(Tooltip.create(Component.literal("Chance 0.0-1.0")));
        this.addRenderableWidget(row.chanceField);

        final int index = effectRows.size();
        this.addRenderableWidget(Button.builder(
                Component.literal("✕").withStyle(ChatFormatting.RED),
                b -> removeAndRefresh(index)
        ).bounds(colDel, y, 18, ROW_H).build());

        effectRows.add(row);
    }

    private void addAndRefresh() {
        captureIntoDef();
        if (def.food == null) def.food = new ItemDefinition.FoodData();
        if (def.food.effects == null) def.food.effects = new ArrayList<>();
        if (def.food.effects.size() < MAX_EFFECTS) {
            ItemDefinition.FoodEffect e = new ItemDefinition.FoodEffect();
            def.food.effects.add(e);
        }
        forceFoodChecked = true;
        this.clearWidgets();
        this.init();
    }

    private void removeAndRefresh(int index) {
        captureIntoDef();
        if (def.food != null && def.food.effects != null && index < def.food.effects.size()) {
            def.food.effects.remove(index);
        }
        this.clearWidgets();
        this.init();
    }

    private void captureIntoDef() {
        forceFoodChecked = false;
        boolean isFood = isFoodBox.selected();
        if (!isFood) {
            def.food = null;
            return;
        }
        if (def.food == null) def.food = new ItemDefinition.FoodData();
        try {
            def.food.nutrition = parseIntOr(nutritionField.getValue(), 1);
            def.food.saturation = parseFloatOr(saturationField.getValue(), 0.1f);
        } catch (NumberFormatException ignored) {}
        def.food.canAlwaysEat = alwaysEatBox.selected();
        def.food.fastEat = fastEatBox.selected();

        List<ItemDefinition.FoodEffect> effects = new ArrayList<>();
        for (EffectRow row : effectRows) {
            ItemDefinition.FoodEffect e = new ItemDefinition.FoodEffect();
            e.effect = row.effectField.getValue().trim();
            e.duration = parseIntOr(row.durationField.getValue(), 100);
            e.amplifier = parseIntOr(row.amplifierField.getValue(), 0);
            e.chance = parseFloatOr(row.chanceField.getValue(), 1.0f);
            effects.add(e);
        }
        def.food.effects = effects;
    }

    private void save() {
        captureIntoDef();
        if (def.food != null) {
            String err = def.food.validate();
            if (err != null) { message = err; return; }
        }
        this.onClose();
    }

    private static int parseIntOr(String s, int fallback) {
        return s == null || s.isBlank() ? fallback : Integer.parseInt(s);
    }

    private static float parseFloatOr(String s, float fallback) {
        return s == null || s.isBlank() ? fallback : Float.parseFloat(s);
    }

    private static String formatFloat(float v) {
        if (v == (int) v) return String.valueOf((int) v);
        return String.valueOf(v);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 18;
        int y = panelY + 28 + ROW_STEP + 2 + 4;

        gfx.drawString(this.font, "Nutrition", labelX, y, 0xFFFFFF, true);
        y += ROW_STEP;
        gfx.drawString(this.font, "Saturation", labelX, y, 0xFFFFFF, true);

        if (effectsHeaderY > 0) {
            gfx.drawString(this.font,
                    Component.literal("Effects  (effect / duration / amplifier / chance)")
                            .withStyle(ChatFormatting.GRAY),
                    labelX, effectsHeaderY, 0xFFFFFF);
        }

        if (message != null) {
            gfx.drawCenteredString(this.font, Component.literal(message),
                    panelX + panelW / 2, panelY + panelH - 44, 0xFF5555);
        }
    }
}
