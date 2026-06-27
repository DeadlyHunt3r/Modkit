package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.armor.ArmorSetDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import net.neoforged.neoforge.network.PacketDistributor;
import com.deadlyhunter.modkit.network.SaveArmorPacket;
import com.deadlyhunter.modkit.network.SetArmorTexturePacket;
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
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArmorSetEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long MAX_PNG_FILE_SIZE = 1024 * 1024;

    private static final int ROW_H = 18;
    private static final int ROW_GAP = 4;
    private static final int LABEL_COLOR = 0xFFFFFF;

    private final ArmorListScreen listParent;
    private final String modName;
    private final ArmorSetDefinition def;
    private final boolean isNew;

    private EditBox idField;
    private EditBox displayNameField;
    private CycleButton<String> tierBtn;
    private Checkbox helmetBox, chestBox, legsBox, bootsBox;
    private EditBox defHelmetField, defChestField, defLegsField, defBootsField;
    private EditBox toughnessField, knockbackField;
    private EditBox duraMultField, enchantField;
    private CycleButton<String> repairSourceBtn;
    private EditBox repairItemField;
    private CycleButton<String> rarityBtn;
    private Checkbox glowBox;

    private String errorMessage = null;
    private String infoMessage = null;

    public ArmorSetEditorScreen(ArmorListScreen parent, String modName,
                                 ArmorSetDefinition def, boolean isNew) {
        super(Component.literal(isNew ? "New Armor Set" : "Edit: " + def.id), parent);
        this.listParent = parent;
        this.modName = modName;
        this.def = def;
        this.isNew = isNew;
        this.panelW = 340;
        this.panelH = 372;
    }

    public ArmorListScreen getListParent() { return listParent; }

    @Override
    protected void init() {
        super.init();

        int fieldX = panelX + 104;
        int fieldW = 216;
        int y = panelY + 26;

        idField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("id"));
        idField.setMaxLength(32);
        idField.setValue(def.id != null ? def.id : "");
        if (!isNew) idField.setEditable(false);
        idField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_]*"));
        this.addRenderableWidget(idField);
        y += ROW_H + ROW_GAP;

        displayNameField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("name"));
        displayNameField.setMaxLength(64);
        displayNameField.setValue(def.displayName != null ? def.displayName : "");
        this.addRenderableWidget(displayNameField);
        y += ROW_H + ROW_GAP;

        tierBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)))
                .withValues("leather", "chainmail", "iron", "gold", "diamond", "netherite", "custom")
                .withInitialValue(def.tier != null ? def.tier : "iron")
                .displayOnlyValue()
                .create(fieldX, y, fieldW, ROW_H, Component.literal(""),
                        (btn, value) -> updateCustomFieldsAvailability(value));
        this.addRenderableWidget(tierBtn);
        y += ROW_H + ROW_GAP;

        int cbW = 52;
        helmetBox = checkbox(fieldX, y, cbW, ROW_H, Component.literal("H"), def.hasHelmet);
        helmetBox.setTooltip(Tooltip.create(Component.literal("Helmet")));
        this.addRenderableWidget(helmetBox);
        chestBox = checkbox(fieldX + cbW + 2, y, cbW, ROW_H, Component.literal("C"), def.hasChestplate);
        chestBox.setTooltip(Tooltip.create(Component.literal("Chestplate")));
        this.addRenderableWidget(chestBox);
        legsBox = checkbox(fieldX + 2 * (cbW + 2), y, cbW, ROW_H, Component.literal("L"), def.hasLeggings);
        legsBox.setTooltip(Tooltip.create(Component.literal("Leggings")));
        this.addRenderableWidget(legsBox);
        bootsBox = checkbox(fieldX + 3 * (cbW + 2), y, cbW, ROW_H, Component.literal("B"), def.hasBoots);
        bootsBox.setTooltip(Tooltip.create(Component.literal("Boots")));
        this.addRenderableWidget(bootsBox);
        y += ROW_H + ROW_GAP;

        int smallW = 50;
        defHelmetField = makeIntField(fieldX, y, smallW, def.defenseHelmet, "Helmet defense");
        defChestField  = makeIntField(fieldX + (smallW + 5), y, smallW, def.defenseChestplate, "Chestplate defense");
        defLegsField   = makeIntField(fieldX + 2 * (smallW + 5), y, smallW, def.defenseLeggings, "Leggings defense");
        defBootsField  = makeIntField(fieldX + 3 * (smallW + 5), y, smallW, def.defenseBoots, "Boots defense");
        y += ROW_H + ROW_GAP;

        toughnessField = new EditBox(this.font, fieldX, y, 104, ROW_H, Component.literal("toughness"));
        toughnessField.setMaxLength(5);
        toughnessField.setValue(formatFloat(def.toughness));
        toughnessField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,2}(\\.\\d{0,2})?"));
        toughnessField.setTooltip(Tooltip.create(Component.literal("Toughness (Diamond=2, Netherite=3)")));
        this.addRenderableWidget(toughnessField);

        knockbackField = new EditBox(this.font, fieldX + 112, y, 104, ROW_H, Component.literal("knockback"));
        knockbackField.setMaxLength(5);
        knockbackField.setValue(formatFloat(def.knockbackResistance));
        knockbackField.setFilter(s -> s.isEmpty() || s.matches("(0(\\.\\d{0,2})?|1(\\.0{0,2})?)?"));
        knockbackField.setTooltip(Tooltip.create(Component.literal("Knockback resistance 0-1 (Netherite=0.1)")));
        this.addRenderableWidget(knockbackField);
        y += ROW_H + ROW_GAP;

        duraMultField = new EditBox(this.font, fieldX, y, 104, ROW_H, Component.literal("dura mult"));
        duraMultField.setMaxLength(4);
        duraMultField.setValue(String.valueOf(def.durabilityMultiplier));
        duraMultField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,4}"));
        duraMultField.setTooltip(Tooltip.create(
                Component.literal("Durability multiplier (Iron=15, Diamond=33). Helmet=11x, Chest=16x, Legs=15x, Boots=13x")));
        this.addRenderableWidget(duraMultField);

        enchantField = new EditBox(this.font, fieldX + 112, y, 104, ROW_H, Component.literal("enchant"));
        enchantField.setMaxLength(3);
        enchantField.setValue(String.valueOf(def.enchantmentValue));
        enchantField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,3}"));
        enchantField.setTooltip(Tooltip.create(Component.literal("Enchantment value (Iron=9, Gold=25, Diamond=10)")));
        this.addRenderableWidget(enchantField);
        y += ROW_H + ROW_GAP;

        updateCustomFieldsAvailability(tierBtn.getValue());

        repairSourceBtn = CycleButton.<String>builder(s ->
                        Component.literal("mine".equals(s) ? "My Item" : "Custom"))
                .withValues("mine", "other")
                .withInitialValue(def.repairSource != null ? def.repairSource : "other")
                .displayOnlyValue()
                .create(fieldX, y, 70, ROW_H, Component.literal(""));
        this.addRenderableWidget(repairSourceBtn);

        repairItemField = new EditBox(this.font, fieldX + 74, y, fieldW - 74, ROW_H,
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
                .create(fieldX, y, 80, ROW_H, Component.literal(""));
        this.addRenderableWidget(rarityBtn);

        glowBox = checkbox(fieldX + 84, y, fieldW - 84, ROW_H,
                Component.literal("Glow"), def.glow);
        this.addRenderableWidget(glowBox);
        y += ROW_H + ROW_GAP;

        int iconW = 52;
        addTextureButton(fieldX, y, iconW, "H", def.pieceItemId("helmet"), "icon",
                "Helmet icon (16x16)", def.hasHelmet);
        addTextureButton(fieldX + (iconW + 3), y, iconW, "C", def.pieceItemId("chestplate"), "icon",
                "Chestplate icon (16x16)", def.hasChestplate);
        addTextureButton(fieldX + 2 * (iconW + 3), y, iconW, "L", def.pieceItemId("leggings"), "icon",
                "Leggings icon (16x16)", def.hasLeggings);
        addTextureButton(fieldX + 3 * (iconW + 3), y, iconW, "B", def.pieceItemId("boots"), "icon",
                "Boots icon (16x16)", def.hasBoots);
        y += ROW_H + ROW_GAP;

        addTextureButton(fieldX, y, 104, "Layer 1", def.id + "_layer_1", "layer",
                "Worn layer 1 — helmet, chestplate, boots (64x32)", true);
        addTextureButton(fieldX + 112, y, 104, "Layer 2", def.id + "_layer_2", "layer",
                "Worn layer 2 — leggings (64x32)", true);
        y += ROW_H + ROW_GAP;

        this.addRenderableWidget(Button.builder(
                Component.literal("Download Layer Templates"),
                btn -> writeLayerTemplates()
        ).bounds(fieldX, y, fieldW, ROW_H).build());

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
                            new ConfirmDeleteArmorScreen(this, modName, def.id, def.displayName))
            ).bounds(panelX + 12, footerY, 60, 20).build());

            int topRightX = panelX + panelW - 12;
            int topRightY = panelY + 6;
            this.addRenderableWidget(Button.builder(
                    Component.literal("JSON"),
                    btn -> this.minecraft.setScreen(new ViewJsonScreen(this, "JSON: " + def.id, def))
            ).bounds(topRightX - 38, topRightY, 38, 14).build());
        }
    }

    private EditBox makeIntField(int x, int y, int w, int value, String tooltip) {
        EditBox box = new EditBox(this.font, x, y, w, ROW_H, Component.literal(tooltip));
        box.setMaxLength(2);
        box.setValue(String.valueOf(value));
        box.setFilter(s -> s.isEmpty() || s.matches("\\d{0,2}"));
        box.setTooltip(Tooltip.create(Component.literal(tooltip)));
        this.addRenderableWidget(box);
        return box;
    }

    private void addTextureButton(int x, int y, int w, String label, String fileName,
                                   String kind, String tooltip, boolean enabled) {
        boolean exists = textureExists(fileName, kind);
        String status = exists ? " ✓" : "";
        Button btn = Button.builder(
                Component.literal(label + status),
                b -> pickAndUploadTexture(fileName, kind)
        ).bounds(x, y, w, ROW_H).build();
        btn.setTooltip(Tooltip.create(Component.literal(tooltip)));
        btn.active = enabled && !isNew;
        if (isNew) {
            btn.setTooltip(Tooltip.create(Component.literal("Save the set first, then add textures.")));
        }
        this.addRenderableWidget(btn);
    }

    private boolean textureExists(String fileName, String kind) {
        if (def.id == null || def.id.isBlank()) return false;
        String subDir = "icon".equals(kind) ? "item" : "armor";
        Path tex = WorkspaceManager.getWorkspacePath(modName)
                .resolve("assets").resolve("textures").resolve(subDir)
                .resolve(fileName + ".png");
        return Files.isRegularFile(tex);
    }

    private void updateCustomFieldsAvailability(String tierValue) {
        boolean isCustom = "custom".equals(tierValue);
        for (EditBox box : new EditBox[]{defHelmetField, defChestField, defLegsField, defBootsField,
                toughnessField, knockbackField, duraMultField, enchantField}) {
            if (box != null) box.setEditable(isCustom);
        }
    }

    private void trySave() {
        String newId = idField.getValue().trim();
        String newDisplay = displayNameField.getValue().trim();

        int defH, defC, defL, defB, duraMult, ench;
        float tough, knock;
        try {
            defH = parseIntOr(defHelmetField.getValue(), 3);
            defC = parseIntOr(defChestField.getValue(), 8);
            defL = parseIntOr(defLegsField.getValue(), 6);
            defB = parseIntOr(defBootsField.getValue(), 3);
            tough = parseFloatOr(toughnessField.getValue(), 2.0f);
            knock = parseFloatOr(knockbackField.getValue(), 0.0f);
            duraMult = parseIntOr(duraMultField.getValue(), 33);
            ench = parseIntOr(enchantField.getValue(), 10);
        } catch (NumberFormatException e) {
            errorMessage = "Invalid number in fields.";
            infoMessage = null;
            return;
        }

        def.id = newId;
        def.displayName = newDisplay.isEmpty() ? newId : newDisplay;
        def.tier = tierBtn.getValue();
        def.hasHelmet = helmetBox.selected();
        def.hasChestplate = chestBox.selected();
        def.hasLeggings = legsBox.selected();
        def.hasBoots = bootsBox.selected();
        def.defenseHelmet = defH;
        def.defenseChestplate = defC;
        def.defenseLeggings = defL;
        def.defenseBoots = defB;
        def.toughness = tough;
        def.knockbackResistance = knock;
        def.durabilityMultiplier = duraMult;
        def.enchantmentValue = ench;
        def.repairSource = repairSourceBtn.getValue();
        def.repairItem = repairItemField.getValue().trim();
        def.rarity = rarityBtn.getValue();
        def.glow = glowBox.selected();

        String err = def.validate();
        if (err != null) { errorMessage = err; infoMessage = null; return; }

        String json = GSON.toJson(def);
        PacketDistributor.sendToServer(new SaveArmorPacket(modName, def.id, json));
        listParent.onArmorChanged();
        this.minecraft.setScreen(listParent);
    }

    private static int parseIntOr(String s, int fallback) {
        return s == null || s.isBlank() ? fallback : Integer.parseInt(s);
    }

    private static float parseFloatOr(String s, float fallback) {
        return s == null || s.isBlank() ? fallback : Float.parseFloat(s);
    }

    private void pickAndUploadTexture(String fileName, String kind) {
        Thread picker = new Thread(() -> {
            String chosenPath;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.png"));
                filters.flip();
                String hint = "icon".equals(kind) ? "16x16 PNG" : "64x32 PNG";
                chosenPath = TinyFileDialogs.tinyfd_openFileDialog(
                        "Choose a " + hint + " for " + fileName,
                        null, filters, "PNG image", false);
            } catch (Throwable t) {
                Modkit.LOGGER.error("[Modkit] File dialog failed", t);
                final String msg = "Could not open file dialog: " + t.getMessage();
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    errorMessage = msg;
                    infoMessage = null;
                });
                return;
            }
            if (chosenPath == null || chosenPath.isBlank()) return;
            final String pathString = chosenPath;
            net.minecraft.client.Minecraft.getInstance().execute(
                    () -> handlePickedFile(pathString, fileName, kind));
        }, "Modkit-ArmorFilePicker");
        picker.setDaemon(true);
        picker.start();
    }

    private void handlePickedFile(String chosenPath, String fileName, String kind) {
        Path source = Path.of(chosenPath);
        if (!Files.isRegularFile(source)) { errorMessage = "File not found."; infoMessage = null; return; }
        if (!chosenPath.toLowerCase().endsWith(".png")) { errorMessage = "Not a .png file."; infoMessage = null; return; }

        byte[] bytes;
        try {
            long size = Files.size(source);
            if (size > MAX_PNG_FILE_SIZE) {
                errorMessage = "PNG too large (max 1MB). Got " + (size / 1024) + " KB.";
                infoMessage = null;
                return;
            }
            bytes = Files.readAllBytes(source);
        } catch (IOException e) {
            errorMessage = "Read error: " + e.getMessage();
            infoMessage = null;
            return;
        }

        PacketDistributor.sendToServer(new SetArmorTexturePacket(modName, fileName, kind, bytes));
        errorMessage = null;
        infoMessage = "Texture uploaded: " + fileName + ".png";
        this.clearWidgets();
        this.init();
    }

    private void writeLayerTemplates() {
        try {
            Path dir = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("modkit").resolve("templates");
            Files.createDirectories(dir);

            BufferedImage l1 = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            fillRegion(l1, 0, 0, 32, 16, 0x90FF5555);
            fillRegion(l1, 16, 16, 24, 16, 0x9055FF55);
            fillRegion(l1, 40, 16, 16, 16, 0x905599FF);
            fillRegion(l1, 0, 16, 16, 16, 0x90FFFF55);
            ImageIO.write(l1, "PNG", dir.resolve("armor_layer_1_template.png").toFile());

            BufferedImage l2 = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            fillRegion(l2, 16, 16, 24, 16, 0x9055FF55);
            fillRegion(l2, 0, 16, 16, 16, 0x90FFFF55);
            ImageIO.write(l2, "PNG", dir.resolve("armor_layer_2_template.png").toFile());

            errorMessage = null;
            infoMessage = "Templates saved to .minecraft/modkit/templates/";
            Modkit.LOGGER.info("[Modkit] Wrote armor layer templates to {}", dir);
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write layer templates", e);
            errorMessage = "Template write failed: " + e.getMessage();
            infoMessage = null;
        }
    }

    private static void fillRegion(BufferedImage img, int x, int y, int w, int h, int argb) {
        int border = 0xFF000000 | (argb & 0x00FFFFFF);
        for (int px = x; px < x + w; px++) {
            for (int py = y; py < y + h; py++) {
                boolean isBorder = px == x || px == x + w - 1 || py == y || py == y + h - 1;
                img.setRGB(px, py, isBorder ? border : argb);
            }
        }
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
        int labelX = panelX + 14;
        int y = panelY + 30;

        String[] labels = {
                "Set ID", "Set Name", "Tier", "Pieces",
                "Defense", "Tough/Knock", "Dura×/Ench",
                "Repair", "Rarity & Glow",
                "Icons", "Worn Layers", ""
        };
        for (String l : labels) {
            boolean isCustomLabel = l.equals("Defense") || l.equals("Tough/Knock") || l.equals("Dura×/Ench");
            boolean dimmed = isCustomLabel && tierBtn != null && !"custom".equals(tierBtn.getValue());
            if (!l.isEmpty()) {
                gfx.drawString(this.font, l, labelX, y, dimmed ? 0x808080 : LABEL_COLOR, true);
            }
            y += ROW_H + ROW_GAP;
        }

        int hintY = panelY + panelH - 48;
        if (errorMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(errorMessage),
                    panelX + panelW / 2, hintY, 0xFF5555);
        } else if (infoMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(infoMessage),
                    panelX + panelW / 2, hintY, 0x55FF55);
        } else if (tierBtn != null && !"custom".equals(tierBtn.getValue())) {
            gfx.drawCenteredString(this.font,
                    Component.literal("Switch tier to Custom to edit greyed fields")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        } else if (!isNew) {
            gfx.drawCenteredString(this.font,
                    Component.literal("Defense order: Helmet, Chest, Legs, Boots")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        }
    }
}
