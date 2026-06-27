package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class BlockDropsScreen extends ModkitBaseScreen {

    private static final int ROW_H = 18;
    private static final int ROW_GAP = 4;
    private static final int ROW_STEP = ROW_H + ROW_GAP;
    private static final int LABEL_COLOR = 0xFFFFFF;

    private final BlockEditorScreen editorParent;
    private final String modName;
    private final BlockDefinition def;

    private final Set<String> ownItemIds = new HashSet<>();

    private CycleButton<String> modeBtn;
    private EditBox itemField;
    private EditBox minField;
    private EditBox maxField;
    private Checkbox fortuneBox;
    private EditBox xpMinField;
    private EditBox xpMaxField;

    private String errorMessage = null;

    public BlockDropsScreen(BlockEditorScreen parent, String modName, BlockDefinition def) {
        super(Component.literal("Drops — " + def.id), parent);
        this.editorParent = parent;
        this.modName = modName;
        this.def = def;
        this.panelW = 280;
        this.panelH = 245;
    }

    @Override
    protected void init() {
        super.init();
        loadOwnItemIds();

        int fieldX = panelX + 110;
        int fieldW = 150;
        int y = panelY + 30;

        modeBtn = CycleButton.<String>builder(s -> Component.literal(modeLabel(s)))
                .withValues("self", "item_mine", "item_other", "nothing")
                .withInitialValue(def.dropMode != null ? def.dropMode : "self")
                .displayOnlyValue()
                .create(fieldX, y, fieldW, ROW_H, Component.literal(""),
                        (btn, value) -> updateFieldsAvailability(value));
        this.addRenderableWidget(modeBtn);
        y += ROW_STEP;

        itemField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("item"));
        itemField.setMaxLength(80);
        itemField.setValue(def.dropItem != null ? def.dropItem : "");
        itemField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_:/]*"));
        this.addRenderableWidget(itemField);
        y += ROW_STEP;

        minField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("min"));
        minField.setMaxLength(3);
        minField.setValue(String.valueOf(def.dropMin));
        minField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}"));
        this.addRenderableWidget(minField);
        y += ROW_STEP;

        maxField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("max"));
        maxField.setMaxLength(3);
        maxField.setValue(String.valueOf(def.dropMax));
        maxField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}"));
        this.addRenderableWidget(maxField);
        y += ROW_STEP;

        fortuneBox = checkbox(fieldX, y, fieldW, ROW_H,
                Component.literal(def.dropFortune ? "Yes" : "No"), def.dropFortune);
        this.addRenderableWidget(fortuneBox);
        y += ROW_STEP;

        xpMinField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("xp min"));
        xpMinField.setMaxLength(3);
        xpMinField.setValue(String.valueOf(def.xpMin));
        xpMinField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}"));
        this.addRenderableWidget(xpMinField);
        y += ROW_STEP;

        xpMaxField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("xp max"));
        xpMaxField.setMaxLength(3);
        xpMaxField.setValue(String.valueOf(def.xpMax));
        xpMaxField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}"));
        this.addRenderableWidget(xpMaxField);

        updateFieldsAvailability(modeBtn.getValue());

        int footerY = panelY + panelH - 30;
        int centerX = panelX + panelW / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("Apply"),
                btn -> tryApply()
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
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

    private void updateFieldsAvailability(String mode) {
        boolean isItem = "item_mine".equals(mode) || "item_other".equals(mode);
        itemField.setEditable(isItem);
        minField.setEditable(isItem);
        maxField.setEditable(isItem);
        fortuneBox.active = isItem;
    }

    private void tryApply() {
        int min, max, xpMin, xpMax;
        try {
            min = minField.getValue().isEmpty() ? 1 : Integer.parseInt(minField.getValue());
            max = maxField.getValue().isEmpty() ? 1 : Integer.parseInt(maxField.getValue());
            xpMin = xpMinField.getValue().isEmpty() ? 0 : Integer.parseInt(xpMinField.getValue());
            xpMax = xpMaxField.getValue().isEmpty() ? 0 : Integer.parseInt(xpMaxField.getValue());
        } catch (NumberFormatException e) {
            errorMessage = "Invalid number.";
            return;
        }

        String mode = modeBtn.getValue();
        String item = itemField.getValue().trim();

        if ("item_mine".equals(mode) && !item.isEmpty() && !ownItemIds.contains(item)) {
            errorMessage = "No item named '" + item + "' in this workspace. Available: "
                    + (ownItemIds.isEmpty() ? "(none — create one first)"
                                            : String.join(", ", ownItemIds));
            return;
        }

        def.dropMode = mode;
        def.dropItem = item;
        def.dropMin = min;
        def.dropMax = max;
        def.dropFortune = fortuneBox.selected();
        def.xpMin = xpMin;
        def.xpMax = xpMax;
        def.dropSelf = "self".equals(def.dropMode);

        String err = def.validate();
        if (err != null) { errorMessage = err; return; }

        this.minecraft.setScreen(editorParent);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 18;
        int y = panelY + 34;

        String[] labels = {"Mode", "Item ID", "Min", "Max", "Fortune", "XP Min", "XP Max"};
        for (String l : labels) {
            gfx.drawString(this.font, l, labelX, y, LABEL_COLOR, true);
            y += ROW_STEP;
        }

        int hintY = panelY + panelH - 44;
        if (errorMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(errorMessage),
                    panelX + panelW / 2, hintY, 0xFF5555);
        } else {
            String hint = switch (modeBtn != null ? modeBtn.getValue() : "self") {
                case "self"       -> "Block drops itself when broken.";
                case "nothing"    -> "Block drops nothing.";
                case "item_mine"  -> "Just your item's id, e.g. fire_essence";
                case "item_other" -> "Full id with namespace, e.g. minecraft:diamond";
                default           -> "";
            };
            gfx.drawCenteredString(this.font,
                    Component.literal(hint).withStyle(ChatFormatting.GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        }
    }

    private static String modeLabel(String mode) {
        return switch (mode) {
            case "self"       -> "Self";
            case "nothing"    -> "Nothing";
            case "item_mine"  -> "My Item";
            case "item_other" -> "Custom Item";
            default           -> mode;
        };
    }
}
