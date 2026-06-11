package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.deadlyhunter.modkit.network.ModNetworking;
import com.deadlyhunter.modkit.network.SaveBlockPacket;
import com.deadlyhunter.modkit.network.SetBlockTexturePacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlockEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long MAX_PNG_FILE_SIZE = 1024 * 1024;

    private static final int ROW_H = 18;
    private static final int ROW_GAP = 4;
    private static final int ROW_STEP = ROW_H + ROW_GAP;
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final boolean LABEL_SHADOW = true;

    private final BlockListScreen listParent;
    private final String modName;
    private final BlockDefinition def;
    private final boolean isNew;

    private EditBox idField;
    private EditBox displayNameField;
    private EditBox hardnessField;
    private EditBox resistanceField;
    private EditBox lightField;
    private EditBox frictionField;
    private CycleButton<String> toolBtn;
    private CycleButton<String> toolTierBtn;
    private CycleButton<String> soundGroupBtn;
    private Checkbox requiresCorrectToolBox;

    private String errorMessage = null;
    private boolean hasTexture = false;

    public BlockEditorScreen(BlockListScreen parent, String modName, BlockDefinition def, boolean isNew) {
        super(Component.literal(isNew ? "New Block" : "Edit: " + def.id), parent);
        this.listParent = parent;
        this.modName = modName;
        this.def = def;
        this.isNew = isNew;
        this.panelW = 280;
        this.panelH = 327;
    }

    public BlockListScreen getListParent() { return listParent; }

    @Override
    protected void init() {
        super.init();
        recomputeTextureFlag();

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

        hardnessField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("hardness"));
        hardnessField.setMaxLength(8);
        hardnessField.setValue(String.valueOf(def.hardness));
        hardnessField.setFilter(s -> s.isEmpty() || s.matches("-?\\d{0,4}(\\.\\d{0,2})?"));
        this.addRenderableWidget(hardnessField);
        y += ROW_STEP;

        resistanceField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("resistance"));
        resistanceField.setMaxLength(8);
        resistanceField.setValue(String.valueOf(def.resistance));
        resistanceField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,5}(\\.\\d{0,2})?"));
        this.addRenderableWidget(resistanceField);
        y += ROW_STEP;

        toolBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                .withValues("any", "pickaxe", "axe", "shovel", "hoe")
                .withInitialValue(def.tool != null ? def.tool : "any")
                .displayOnlyValue()
                .create(fieldX, y, fieldW, ROW_H, Component.literal(""),
                        (btn, value) -> updateToolNeededAvailability(value));
        this.addRenderableWidget(toolBtn);
        y += ROW_STEP;

        toolTierBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                .withValues("wood", "stone", "iron", "diamond")
                .withInitialValue(def.toolTier != null ? def.toolTier : "wood")
                .displayOnlyValue()
                .create(fieldX, y, fieldW, ROW_H, Component.literal(""));
        this.addRenderableWidget(toolTierBtn);
        y += ROW_STEP;

        boolean initialEnabled = !"any".equals(def.tool);
        requiresCorrectToolBox = new Checkbox(fieldX, y, fieldW, ROW_H,
                Component.literal(def.requiresCorrectTool && initialEnabled ? "Yes" : "No"),
                def.requiresCorrectTool && initialEnabled);
        this.addRenderableWidget(requiresCorrectToolBox);
        updateToolNeededAvailability(toolBtn.getValue());
        y += ROW_STEP;

        lightField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("light"));
        lightField.setMaxLength(2);
        lightField.setValue(String.valueOf(def.lightEmission));
        lightField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,2}"));
        this.addRenderableWidget(lightField);
        y += ROW_STEP;

        soundGroupBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                .withValues("stone", "wood", "gravel", "grass", "metal", "glass", "wool", "sand", "snow")
                .withInitialValue(def.soundGroup != null ? def.soundGroup : "stone")
                .displayOnlyValue()
                .create(fieldX, y, fieldW, ROW_H, Component.literal(""));
        this.addRenderableWidget(soundGroupBtn);
        y += ROW_STEP;

        frictionField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("friction"));
        frictionField.setMaxLength(5);
        frictionField.setValue(String.valueOf(def.friction));
        frictionField.setFilter(s -> s.isEmpty() || s.matches("\\d?(\\.\\d{0,3})?"));
        this.addRenderableWidget(frictionField);
        y += ROW_STEP;

        Button textureBtn = Button.builder(
                Component.literal(hasTexture ? "Change..." : "Choose..."),
                btn -> openTexturePicker()
        ).bounds(fieldX, y, fieldW, ROW_H).build();
        if (isNew) {
            textureBtn.active = false;
            textureBtn.setTooltip(Tooltip.create(
                    Component.literal("Save the block first, then set its texture.")));
        }
        this.addRenderableWidget(textureBtn);
        y += ROW_STEP;

        Button dropsBtn = Button.builder(
                Component.literal(dropsButtonLabel()),
                btn -> this.minecraft.setScreen(new BlockDropsScreen(this, modName, def))
        ).bounds(fieldX, y, fieldW, ROW_H).build();
        this.addRenderableWidget(dropsBtn);

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
                            new ConfirmDeleteBlockScreen(this, modName, def.id, def.displayName))
            ).bounds(panelX + 12, footerY, 60, 20).build());

            int topRightX = panelX + panelW - 12;
            int topRightY = panelY + 6;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Copy"),
                    btn -> duplicateBlock()
            ).bounds(topRightX - 38, topRightY, 38, 14).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("JSON"),
                    btn -> this.minecraft.setScreen(new ViewJsonScreen(this, "JSON: " + def.id, def))
            ).bounds(topRightX - 38 - 4 - 38, topRightY, 38, 14).build());
        }
    }

    private void duplicateBlock() {
        BlockDefinition copy = new BlockDefinition();
        copy.id = "";
        copy.displayName = def.displayName + " Copy";
        copy.hardness = def.hardness;
        copy.resistance = def.resistance;
        copy.tool = def.tool;
        copy.toolTier = def.toolTier;
        copy.requiresCorrectTool = def.requiresCorrectTool;
        copy.lightEmission = def.lightEmission;
        copy.soundGroup = def.soundGroup;
        copy.friction = def.friction;
        copy.dropSelf = def.dropSelf;

        this.minecraft.setScreen(new BlockEditorScreen(listParent, modName, copy, true));
    }

    private void updateToolNeededAvailability(String toolValue) {
        if (requiresCorrectToolBox == null) return;
        boolean enabled = !"any".equals(toolValue);

        requiresCorrectToolBox.active = enabled;

        if (!enabled) {
            if (requiresCorrectToolBox.selected()) {
                requiresCorrectToolBox.onPress();
            }
            requiresCorrectToolBox.setTooltip(Tooltip.create(
                    Component.literal("Pick a specific tool first — has no effect when Tool is Any.")));
        } else {
            requiresCorrectToolBox.setTooltip(null);
        }
    }

    private void recomputeTextureFlag() {
        if (def.id == null || def.id.isBlank()) { hasTexture = false; return; }
        Path tex = WorkspaceManager.getWorkspacePath(modName)
                .resolve("assets").resolve("textures").resolve("block")
                .resolve(def.id + ".png");
        hasTexture = Files.isRegularFile(tex);
    }

    private void trySave() {
        String newId = idField.getValue().trim();
        String newDisplay = displayNameField.getValue().trim();

        float hardness, resistance, friction;
        int light;
        try {
            hardness = hardnessField.getValue().isEmpty() ? 1.5f : Float.parseFloat(hardnessField.getValue());
            resistance = resistanceField.getValue().isEmpty() ? 6.0f : Float.parseFloat(resistanceField.getValue());
            friction = frictionField.getValue().isEmpty() ? 0.6f : Float.parseFloat(frictionField.getValue());
            light = lightField.getValue().isEmpty() ? 0 : Integer.parseInt(lightField.getValue());
        } catch (NumberFormatException e) {
            errorMessage = "Invalid number in fields.";
            return;
        }

        def.id = newId;
        def.displayName = newDisplay.isEmpty() ? newId : newDisplay;
        def.hardness = hardness;
        def.resistance = resistance;
        def.tool = toolBtn.getValue();
        def.toolTier = toolTierBtn.getValue();
        def.requiresCorrectTool = !"any".equals(def.tool) && requiresCorrectToolBox.selected();
        def.lightEmission = light;
        def.soundGroup = soundGroupBtn.getValue();
        def.friction = friction;

        String err = def.validate();
        if (err != null) { errorMessage = err; return; }

        String json = GSON.toJson(def);
        ModNetworking.CHANNEL.sendToServer(new SaveBlockPacket(modName, def.id, json));
        listParent.onBlockChanged();
        this.minecraft.setScreen(listParent);
    }

    private void openTexturePicker() {
        Thread picker = new Thread(() -> {
            String chosenPath;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.png"));
                filters.flip();
                chosenPath = TinyFileDialogs.tinyfd_openFileDialog(
                        "Choose a 16x16 PNG for block " + def.id,
                        null, filters, "PNG image", false);
            } catch (Throwable t) {
                Modkit.LOGGER.error("[Modkit] File dialog failed", t);
                final String msg = "Could not open file dialog: " + t.getMessage();
                net.minecraft.client.Minecraft.getInstance().execute(() -> errorMessage = msg);
                return;
            }

            if (chosenPath == null || chosenPath.isBlank()) return;
            final String pathString = chosenPath;
            net.minecraft.client.Minecraft.getInstance().execute(() -> handlePickedFile(pathString));
        }, "Modkit-BlockFilePicker");
        picker.setDaemon(true);
        picker.start();
    }

    private void handlePickedFile(String chosenPath) {
        Path source = Path.of(chosenPath);
        if (!Files.isRegularFile(source)) { errorMessage = "File not found."; return; }
        if (!chosenPath.toLowerCase().endsWith(".png")) { errorMessage = "Not a .png file."; return; }

        byte[] bytes;
        try {
            long size = Files.size(source);
            if (size > MAX_PNG_FILE_SIZE) {
                errorMessage = "PNG too large (max 1MB). Got " + (size / 1024) + " KB.";
                return;
            }
            bytes = Files.readAllBytes(source);
        } catch (IOException e) {
            errorMessage = "Read error: " + e.getMessage();
            return;
        }

        ModNetworking.CHANNEL.sendToServer(new SetBlockTexturePacket(modName, def.id, bytes));
        hasTexture = true;
        errorMessage = null;
        this.clearWidgets();
        this.init();
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 18;
        int y = panelY + 26 + 4;

        String[] labels = {
                "ID", "Display", "Hardness", "Resistance",
                "Tool", "Tool Tier", "Tool needed?", "Light",
                "Sound", "Friction", "Texture", "Drops"
        };
        for (String l : labels) {
            gfx.drawString(this.font, l, labelX, y, LABEL_COLOR, LABEL_SHADOW);
            y += ROW_STEP;
        }

        if (!isNew) {
            int textureRowY = panelY + 26 + 10 * ROW_STEP + 4;
            String status = hasTexture ? "✓" : "—";
            int color = hasTexture ? 0x55FF55 : 0xAAAAAA;
            gfx.drawString(this.font, status, panelX + 98, textureRowY, color, LABEL_SHADOW);
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

    private String dropsButtonLabel() {
        String mode = def.dropMode != null ? def.dropMode : "self";
        return switch (mode) {
            case "self"       -> "Self ▸";
            case "nothing"    -> "Nothing ▸";
            case "item_mine"  -> {
                String item = def.dropItem != null && !def.dropItem.isBlank() ? def.dropItem : "(none)";
                yield "My: " + item + " ▸";
            }
            case "item_other" -> {
                String item = def.dropItem != null && !def.dropItem.isBlank() ? def.dropItem : "(none)";
                yield "Item: " + item + " ▸";
            }
            case "item"       -> {
                String item = def.dropItem != null && !def.dropItem.isBlank() ? def.dropItem : "(none)";
                yield "Item: " + item + " ▸";
            }
            default -> "Configure ▸";
        };
    }
}
