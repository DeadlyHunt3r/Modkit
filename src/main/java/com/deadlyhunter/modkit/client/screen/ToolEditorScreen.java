package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.tool.ToolDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.deadlyhunter.modkit.network.ModNetworking;
import com.deadlyhunter.modkit.network.SaveToolPacket;
import com.deadlyhunter.modkit.network.SetTexturePacket;
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


public class ToolEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long MAX_PNG_FILE_SIZE = 1024 * 1024;

    private static final int ROW_H = 18;
    private static final int ROW_GAP = 4;
    private static final int LABEL_COLOR = 0xFFFFFF;

    private final ToolListScreen listParent;
    private final String modName;
    private final ToolDefinition def;
    private final boolean isNew;

    private EditBox idField;
    private EditBox displayNameField;
    private CycleButton<String> typeBtn;
    private CycleButton<String> tierBtn;
    private EditBox damageBonusField;
    private EditBox attackSpeedField;
    private EditBox miningLevelField;
    private EditBox durabilityField;
    private EditBox damageBaseField;
    private EditBox miningSpeedField;
    private EditBox enchantValueField;
    private CycleButton<String> repairSourceBtn;
    private EditBox repairItemField;
    private CycleButton<String> rarityBtn;
    private Checkbox glowBox;

    private String errorMessage = null;
    private boolean hasTexture = false;

    public ToolEditorScreen(ToolListScreen parent, String modName,
                             ToolDefinition def, boolean isNew) {
        super(Component.literal(isNew ? "New " + cap(def.toolType) : "Edit: " + def.id), parent);
        this.listParent = parent;
        this.modName = modName;
        this.def = def;
        this.isNew = isNew;
        this.panelW = 320;
        this.panelH = 380;
    }

    public ToolListScreen getListParent() { return listParent; }

    @Override
    protected void init() {
        super.init();
        recomputeTextureFlag();

        int leftFieldX = panelX + 100;
        int leftFieldW = 200;

        int y = panelY + 26;

        idField = new EditBox(this.font, leftFieldX, y, leftFieldW, ROW_H, Component.literal("id"));
        idField.setMaxLength(40);
        idField.setValue(def.id != null ? def.id : "");
        if (!isNew) idField.setEditable(false);
        idField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_]*"));
        this.addRenderableWidget(idField);
        y += ROW_H + ROW_GAP;

        displayNameField = new EditBox(this.font, leftFieldX, y, leftFieldW, ROW_H, Component.literal("display"));
        displayNameField.setMaxLength(64);
        displayNameField.setValue(def.displayName != null ? def.displayName : "");
        this.addRenderableWidget(displayNameField);
        y += ROW_H + ROW_GAP;

        typeBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                .withValues("pickaxe", "axe", "shovel", "hoe")
                .withInitialValue(def.toolType != null ? def.toolType : "pickaxe")
                .displayOnlyValue()
                .create(leftFieldX, y, leftFieldW, ROW_H, Component.literal(""),
                        (btn, value) -> onToolTypeChanged(value));

        if (!isNew) typeBtn.active = false;
        this.addRenderableWidget(typeBtn);
        y += ROW_H + ROW_GAP;

        tierBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                .withValues("wood", "stone", "iron", "diamond", "netherite", "custom")
                .withInitialValue(def.tier != null ? def.tier : "iron")
                .displayOnlyValue()
                .create(leftFieldX, y, leftFieldW, ROW_H, Component.literal(""),
                        (btn, value) -> updateCustomFieldsAvailability(value));
        this.addRenderableWidget(tierBtn);
        y += ROW_H + ROW_GAP;

        damageBonusField = new EditBox(this.font, leftFieldX, y, leftFieldW, ROW_H,
                Component.literal("damage bonus"));
        damageBonusField.setMaxLength(6);
        damageBonusField.setValue(formatFloat(def.damageBonus));
        damageBonusField.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{0,3}(\\.\\d{0,2})?"));
        this.addRenderableWidget(damageBonusField);
        y += ROW_H + ROW_GAP;

        attackSpeedField = new EditBox(this.font, leftFieldX, y, leftFieldW, ROW_H,
                Component.literal("attack speed"));
        attackSpeedField.setMaxLength(6);
        attackSpeedField.setValue(formatFloat(def.attackSpeed));
        attackSpeedField.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{0,2}(\\.\\d{0,2})?"));
        this.addRenderableWidget(attackSpeedField);
        y += ROW_H + ROW_GAP;

        miningLevelField = new EditBox(this.font, leftFieldX, y, leftFieldW, ROW_H,
                Component.literal("mining level"));
        miningLevelField.setMaxLength(2);
        miningLevelField.setValue(String.valueOf(def.miningLevel));
        miningLevelField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,2}"));
        this.addRenderableWidget(miningLevelField);
        y += ROW_H + ROW_GAP;

        durabilityField = new EditBox(this.font, leftFieldX, y, leftFieldW, ROW_H,
                Component.literal("durability"));
        durabilityField.setMaxLength(6);
        durabilityField.setValue(String.valueOf(def.durability));
        durabilityField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,6}"));
        this.addRenderableWidget(durabilityField);
        y += ROW_H + ROW_GAP;

        damageBaseField = new EditBox(this.font, leftFieldX, y, leftFieldW, ROW_H,
                Component.literal("damage base"));
        damageBaseField.setMaxLength(6);
        damageBaseField.setValue(formatFloat(def.damageBase));
        damageBaseField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}(\\.\\d{0,2})?"));
        this.addRenderableWidget(damageBaseField);
        y += ROW_H + ROW_GAP;

        miningSpeedField = new EditBox(this.font, leftFieldX, y, leftFieldW, ROW_H,
                Component.literal("mining speed"));
        miningSpeedField.setMaxLength(6);
        miningSpeedField.setValue(formatFloat(def.miningSpeed));
        miningSpeedField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}(\\.\\d{0,2})?"));
        this.addRenderableWidget(miningSpeedField);
        y += ROW_H + ROW_GAP;

        enchantValueField = new EditBox(this.font, leftFieldX, y, leftFieldW, ROW_H,
                Component.literal("enchant value"));
        enchantValueField.setMaxLength(3);
        enchantValueField.setValue(String.valueOf(def.enchantmentValue));
        enchantValueField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}"));
        this.addRenderableWidget(enchantValueField);
        y += ROW_H + ROW_GAP;

        updateCustomFieldsAvailability(tierBtn.getValue());

        repairSourceBtn = CycleButton.<String>builder(s ->
                        Component.literal("mine".equals(s) ? "My Item" : "Custom"))
                .withValues("mine", "other")
                .withInitialValue(def.repairSource != null ? def.repairSource : "other")
                .displayOnlyValue()
                .create(leftFieldX, y, 70, ROW_H, Component.literal(""));
        this.addRenderableWidget(repairSourceBtn);

        repairItemField = new EditBox(this.font, leftFieldX + 74, y, leftFieldW - 74, ROW_H,
                Component.literal("repair item"));
        repairItemField.setMaxLength(80);
        repairItemField.setValue(def.repairItem != null ? def.repairItem : "");
        repairItemField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_:/]*"));
        this.addRenderableWidget(repairItemField);
        y += ROW_H + ROW_GAP;

        rarityBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                .withValues("common", "uncommon", "rare", "epic")
                .withInitialValue(def.rarity != null ? def.rarity : "common")
                .displayOnlyValue()
                .create(leftFieldX, y, 80, ROW_H, Component.literal(""));
        this.addRenderableWidget(rarityBtn);

        glowBox = new Checkbox(leftFieldX + 84, y, leftFieldW - 84, ROW_H,
                Component.literal("Glow"), def.glow);
        this.addRenderableWidget(glowBox);
        y += ROW_H + ROW_GAP;

        Button textureBtn = Button.builder(
                Component.literal(hasTexture ? "Change..." : "Choose..."),
                btn -> openTexturePicker()
        ).bounds(leftFieldX, y, leftFieldW, ROW_H).build();
        if (isNew) {
            textureBtn.active = false;
            textureBtn.setTooltip(Tooltip.create(
                    Component.literal("Save the tool first, then set its texture.")));
        }
        this.addRenderableWidget(textureBtn);

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

        if (!isNew) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Delete").withStyle(ChatFormatting.RED),
                    btn -> this.minecraft.setScreen(
                            new ConfirmDeleteToolScreen(this, modName, def.id, def.displayName))
            ).bounds(panelX + 12, footerY, 60, 20).build());

            int topRightX = panelX + panelW - 12;
            int topRightY = panelY + 6;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Copy"),
                    btn -> duplicateTool()
            ).bounds(topRightX - 38, topRightY, 38, 14).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("JSON"),
                    btn -> this.minecraft.setScreen(new ViewJsonScreen(this, "JSON: " + def.id, def))
            ).bounds(topRightX - 38 - 4 - 38, topRightY, 38, 14).build());
        }
    }

    private void onToolTypeChanged(String newType) {
        if (!isNew) return;
        float[] defaults = ToolDefinition.vanillaDefaultsFor(newType);
        if (damageBonusField != null) damageBonusField.setValue(formatFloat(defaults[0]));
        if (attackSpeedField != null) attackSpeedField.setValue(formatFloat(defaults[1]));
    }

    private void updateCustomFieldsAvailability(String tierValue) {
        boolean isCustom = "custom".equals(tierValue);
        if (miningLevelField != null) miningLevelField.setEditable(isCustom);
        if (durabilityField != null) durabilityField.setEditable(isCustom);
        if (damageBaseField != null) damageBaseField.setEditable(isCustom);
        if (miningSpeedField != null) miningSpeedField.setEditable(isCustom);
        if (enchantValueField != null) enchantValueField.setEditable(isCustom);
    }

    private void duplicateTool() {
        ToolDefinition copy = new ToolDefinition();
        copy.id = "";
        copy.displayName = def.displayName + " Copy";
        copy.toolType = def.toolType;
        copy.tier = def.tier;
        copy.damageBonus = def.damageBonus;
        copy.attackSpeed = def.attackSpeed;
        copy.miningLevel = def.miningLevel;
        copy.durability = def.durability;
        copy.damageBase = def.damageBase;
        copy.miningSpeed = def.miningSpeed;
        copy.enchantmentValue = def.enchantmentValue;
        copy.repairSource = def.repairSource;
        copy.repairItem = def.repairItem;
        copy.rarity = def.rarity;
        copy.glow = def.glow;
        copy.tooltipLines = new java.util.ArrayList<>(def.tooltipLines);
        this.minecraft.setScreen(new ToolEditorScreen(listParent, modName, copy, true));
    }

    private void recomputeTextureFlag() {
        if (def.id == null || def.id.isBlank()) { hasTexture = false; return; }
        Path tex = WorkspaceManager.getWorkspacePath(modName)
                .resolve("assets").resolve("textures").resolve("item")
                .resolve(def.id + ".png");
        hasTexture = Files.isRegularFile(tex);
    }

    private void trySave() {
        String newId = idField.getValue().trim();
        String newDisplay = displayNameField.getValue().trim();

        float damageBonus, attackSpeed, damageBase, miningSpeed;
        int miningLevel, durability, enchantValue;
        try {
            damageBonus = damageBonusField.getValue().isEmpty() || damageBonusField.getValue().equals("-") ? 0f
                    : Float.parseFloat(damageBonusField.getValue());
            attackSpeed = attackSpeedField.getValue().isEmpty() || attackSpeedField.getValue().equals("-") ? -2.8f
                    : Float.parseFloat(attackSpeedField.getValue());
            miningLevel = miningLevelField.getValue().isEmpty() ? 2
                    : Integer.parseInt(miningLevelField.getValue());
            durability = durabilityField.getValue().isEmpty() ? 250
                    : Integer.parseInt(durabilityField.getValue());
            damageBase = damageBaseField.getValue().isEmpty() ? 2f
                    : Float.parseFloat(damageBaseField.getValue());
            miningSpeed = miningSpeedField.getValue().isEmpty() ? 6f
                    : Float.parseFloat(miningSpeedField.getValue());
            enchantValue = enchantValueField.getValue().isEmpty() ? 14
                    : Integer.parseInt(enchantValueField.getValue());
        } catch (NumberFormatException e) {
            errorMessage = "Invalid number in fields.";
            return;
        }

        def.id = newId;
        def.displayName = newDisplay.isEmpty() ? newId : newDisplay;
        def.toolType = typeBtn.getValue();
        def.tier = tierBtn.getValue();
        def.damageBonus = damageBonus;
        def.attackSpeed = attackSpeed;
        def.miningLevel = miningLevel;
        def.durability = durability;
        def.damageBase = damageBase;
        def.miningSpeed = miningSpeed;
        def.enchantmentValue = enchantValue;
        def.repairSource = repairSourceBtn.getValue();
        def.repairItem = repairItemField.getValue().trim();
        def.rarity = rarityBtn.getValue();
        def.glow = glowBox.selected();

        String err = def.validate();
        if (err != null) { errorMessage = err; return; }

        String json = GSON.toJson(def);
        ModNetworking.CHANNEL.sendToServer(new SaveToolPacket(modName, def.id, json));
        listParent.onToolChanged();
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
                        "Choose a 16x16 PNG for " + def.toolType + " " + def.id,
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
        }, "Modkit-ToolFilePicker");
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

        ModNetworking.CHANNEL.sendToServer(new SetTexturePacket(modName, def.id, bytes));
        hasTexture = true;
        errorMessage = null;
        this.clearWidgets();
        this.init();
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String formatFloat(float v) {
        if (v == (int) v) return String.valueOf((int) v);
        return String.valueOf(v);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 18;
        int y = panelY + 30;

        String[] labels = {
                "ID", "Display",
                "Type", "Tier",
                "Damage Bonus", "Attack Speed",
                "Mining Level", "Durability", "Damage Base", "Mining Speed", "Enchant Val",
                "Repair", "Rarity & Glow", "Texture"
        };
        for (String l : labels) {
            boolean isCustomLabel = l.equals("Mining Level") || l.equals("Durability")
                    || l.equals("Damage Base") || l.equals("Mining Speed") || l.equals("Enchant Val");
            boolean dimmed = isCustomLabel && tierBtn != null && !"custom".equals(tierBtn.getValue());
            gfx.drawString(this.font, l, labelX, y, dimmed ? 0x808080 : LABEL_COLOR, true);
            y += ROW_H + ROW_GAP;
        }

        if (!isNew) {
            int textureRowY = panelY + 30 + 13 * (ROW_H + ROW_GAP);
            String status = hasTexture ? "✓" : "—";
            int color = hasTexture ? 0x55FF55 : 0xAAAAAA;
            gfx.drawString(this.font, status, panelX + 78, textureRowY, color, true);
        }

        int hintY = panelY + panelH - 50;
        if (errorMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(errorMessage),
                    panelX + panelW / 2, hintY, 0xFF5555);
        } else if (tierBtn != null && !"custom".equals(tierBtn.getValue())) {
            gfx.drawCenteredString(this.font,
                    Component.literal("Switch tier to Custom to edit greyed fields")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        } else if (!isNew) {
            gfx.drawCenteredString(this.font,
                    Component.literal("ID locked — delete & recreate to rename")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        }
    }
}
