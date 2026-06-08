package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.ore.OreDefinition;
import com.deadlyhunter.modkit.network.ModNetworking;
import com.deadlyhunter.modkit.network.SaveOrePacket;
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

public class OreEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int ROW_H = 18;
    private static final int ROW_GAP = 4;
    private static final int ROW_STEP = ROW_H + ROW_GAP;
    private static final int LABEL_COLOR = 0xFFFFFF;

    private final OreListScreen listParent;
    private final String modName;
    private final OreDefinition def;
    private final boolean isNew;

    private EditBox idField;
    private EditBox displayNameField;
    private CycleButton<String> blockBtn;
    private CycleButton<String> dimensionBtn;
    private CycleButton<String> replacesBtn;
    private EditBox minYField;
    private EditBox maxYField;
    private EditBox veinsField;
    private EditBox veinSizeField;

    private String errorMessage = null;

    public OreEditorScreen(OreListScreen parent, String modName, OreDefinition def, boolean isNew) {
        super(Component.literal(isNew ? "New Ore" : "Edit: " + def.id), parent);
        this.listParent = parent;
        this.modName = modName;
        this.def = def;
        this.isNew = isNew;
        this.panelW = 280;
        this.panelH = 275;
    }

    public OreListScreen getListParent() { return listParent; }

    @Override
    protected void init() {
        super.init();

        int fieldX = panelX + 110;
        int fieldW = 150;
        int y = panelY + 26;

        idField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("id"));
        idField.setMaxLength(40);
        idField.setValue(def.id != null ? def.id : "");
        if (!isNew) idField.setEditable(false);
        idField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_]*"));
        this.addRenderableWidget(idField);
        y += ROW_STEP;

        displayNameField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("display"));
        displayNameField.setMaxLength(64);
        displayNameField.setValue(def.displayName != null ? def.displayName : "");
        this.addRenderableWidget(displayNameField);
        y += ROW_STEP;

        List<String> blockIds = new ArrayList<>();
        for (BlockDefinition b : listParent.getAvailableBlocks()) {
            blockIds.add(b.id);
        }
        String initialBlockId = def.blockId != null && blockIds.contains(def.blockId)
                ? def.blockId
                : (blockIds.isEmpty() ? "" : blockIds.get(0));

        if (blockIds.isEmpty()) {
            blockIds.add("");
        }
        blockBtn = CycleButton.<String>builder(s -> Component.literal(s.isEmpty() ? "(none)" : s))
                .withValues(blockIds)
                .withInitialValue(initialBlockId)
                .displayOnlyValue()
                .create(fieldX, y, fieldW, ROW_H, Component.literal(""));
        this.addRenderableWidget(blockBtn);
        y += ROW_STEP;

        dimensionBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                .withValues("overworld", "nether", "end")
                .withInitialValue(def.dimension != null ? def.dimension : "overworld")
                .displayOnlyValue()
                .create(fieldX, y, fieldW, ROW_H, Component.literal(""));
        this.addRenderableWidget(dimensionBtn);
        y += ROW_STEP;

        replacesBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                .withValues("stone", "deepslate", "both")
                .withInitialValue(def.replaces != null ? def.replaces : "stone")
                .displayOnlyValue()
                .create(fieldX, y, fieldW, ROW_H, Component.literal(""));
        this.addRenderableWidget(replacesBtn);
        y += ROW_STEP;

        minYField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("minY"));
        minYField.setMaxLength(5);
        minYField.setValue(String.valueOf(def.minY));
        minYField.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{0,3}"));
        this.addRenderableWidget(minYField);
        y += ROW_STEP;

        maxYField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("maxY"));
        maxYField.setMaxLength(5);
        maxYField.setValue(String.valueOf(def.maxY));
        maxYField.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{0,3}"));
        this.addRenderableWidget(maxYField);
        y += ROW_STEP;

        veinsField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("veins"));
        veinsField.setMaxLength(3);
        veinsField.setValue(String.valueOf(def.veinsPerChunk));
        veinsField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}"));
        this.addRenderableWidget(veinsField);
        y += ROW_STEP;

        veinSizeField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("size"));
        veinSizeField.setMaxLength(3);
        veinSizeField.setValue(String.valueOf(def.veinSize));
        veinSizeField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}"));
        this.addRenderableWidget(veinSizeField);

        int footerY = panelY + panelH - 32;
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
                            new ConfirmDeleteOreScreen(this, modName, def.id, def.displayName))
            ).bounds(panelX + 12, footerY, 60, 20).build());

            int topRightX = panelX + panelW - 12;
            int topRightY = panelY + 6;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Copy"),
                    btn -> duplicateOre()
            ).bounds(topRightX - 38, topRightY, 38, 14).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("JSON"),
                    btn -> this.minecraft.setScreen(new ViewJsonScreen(this, "JSON: " + def.id, def))
            ).bounds(topRightX - 38 - 4 - 38, topRightY, 38, 14).build());
        }
    }

    private void duplicateOre() {
        OreDefinition copy = new OreDefinition();
        copy.id = "";
        copy.displayName = def.displayName + " Copy";
        copy.blockId = def.blockId;
        copy.dimension = def.dimension;
        copy.replaces = def.replaces;
        copy.minY = def.minY;
        copy.maxY = def.maxY;
        copy.veinsPerChunk = def.veinsPerChunk;
        copy.veinSize = def.veinSize;
        this.minecraft.setScreen(new OreEditorScreen(listParent, modName, copy, true));
    }

    private void trySave() {
        String newId = idField.getValue().trim();
        String newDisplay = displayNameField.getValue().trim();

        int minY, maxY, veins, size;
        try {
            minY = minYField.getValue().isEmpty() || minYField.getValue().equals("-") ? -64
                    : Integer.parseInt(minYField.getValue());
            maxY = maxYField.getValue().isEmpty() || maxYField.getValue().equals("-") ? 64
                    : Integer.parseInt(maxYField.getValue());
            veins = veinsField.getValue().isEmpty() ? 8 : Integer.parseInt(veinsField.getValue());
            size = veinSizeField.getValue().isEmpty() ? 6 : Integer.parseInt(veinSizeField.getValue());
        } catch (NumberFormatException e) {
            errorMessage = "Invalid number in fields.";
            return;
        }

        def.id = newId;
        def.displayName = newDisplay.isEmpty() ? newId : newDisplay;
        def.blockId = blockBtn.getValue();
        def.dimension = dimensionBtn.getValue();
        def.replaces = replacesBtn.getValue();
        def.minY = minY;
        def.maxY = maxY;
        def.veinsPerChunk = veins;
        def.veinSize = size;

        String err = def.validate();
        if (err != null) { errorMessage = err; return; }

        String json = GSON.toJson(def);
        ModNetworking.CHANNEL.sendToServer(new SaveOrePacket(modName, def.id, json));
        listParent.onOreChanged();
        this.minecraft.setScreen(listParent);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 18;
        int y = panelY + 26 + 4;

        String[] labels = {
                "ID", "Display", "Block", "Dimension",
                "Replaces", "Min Y", "Max Y", "Veins/Chunk", "Vein Size"
        };
        for (String l : labels) {
            gfx.drawString(this.font, l, labelX, y, LABEL_COLOR, true);
            y += ROW_STEP;
        }

        int hintY = panelY + panelH - 44;
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

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
