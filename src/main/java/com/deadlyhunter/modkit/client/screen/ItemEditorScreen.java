package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import net.neoforged.neoforge.network.PacketDistributor;
import com.deadlyhunter.modkit.network.SaveItemPacket;
import com.deadlyhunter.modkit.network.SetTexturePacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class ItemEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_TOOLTIP_LINES = 3;
    private static final long MAX_PNG_FILE_SIZE = 1024 * 1024;

    private static final int ROW_H = 18;
    private static final int ROW_GAP = 4;
    private static final int ROW_STEP = ROW_H + ROW_GAP;


    private static final int LABEL_COLOR = 0xFFFFFF;

    private static final boolean LABEL_SHADOW = true;

    private final ItemListScreen listParent;
    private final String modName;
    private final ItemDefinition def;
    private final boolean isNew;

    private EditBox idField;
    private EditBox displayNameField;
    private EditBox fuelField;
    private final List<EditBox> tooltipFields = new ArrayList<>();
    private CycleButton<Integer> stackSizeBtn;
    private CycleButton<String> rarityBtn;
    private Checkbox glowBox;

    private String errorMessage = null;
    private boolean hasTexture = false;

    public ItemEditorScreen(ItemListScreen parent, String modName, ItemDefinition def, boolean isNew) {
        super(Component.literal(isNew ? "New Item" : "Edit: " + def.id), parent);
        this.listParent = parent;
        this.modName = modName;
        this.def = def;
        this.isNew = isNew;
        this.panelW = 280;
        this.panelH = 295;
    }

    public ItemListScreen getListParent() { return listParent; }

    @Override
    protected void init() {
        super.init();
        recomputeTextureFlag();

        int fieldX = panelX + 90;
        int fieldW = 170;
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


        stackSizeBtn = CycleButton.<Integer>builder(v -> Component.literal(String.valueOf(v)))
                .withValues(1, 16, 64)
                .withInitialValue(snapStack(def.maxStackSize))
                .displayOnlyValue()
                .create(fieldX, y, fieldW, ROW_H, Component.literal(""));
        this.addRenderableWidget(stackSizeBtn);
        y += ROW_STEP;


        rarityBtn = CycleButton.<String>builder(s -> Component.literal(cap(s)).withStyle(rarityColor(s)))
                .withValues("common", "uncommon", "rare", "epic")
                .withInitialValue(def.rarity != null ? def.rarity : "common")
                .displayOnlyValue()
                .create(fieldX, y, fieldW, ROW_H, Component.literal(""));
        this.addRenderableWidget(rarityBtn);
        y += ROW_STEP;


        glowBox = checkbox(fieldX, y, fieldW, ROW_H,
                Component.literal(def.glow ? "Enabled" : "Disabled"), def.glow);
        this.addRenderableWidget(glowBox);
        y += ROW_STEP;


        fuelField = new EditBox(this.font, fieldX, y, fieldW, ROW_H, Component.literal("fuel"));
        fuelField.setMaxLength(6);
        fuelField.setValue(String.valueOf(def.fuelBurnTime));
        fuelField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,6}"));
        this.addRenderableWidget(fuelField);
        y += ROW_STEP;


        int thirdW = (fieldW - 12) / 3;
        Button textureBtn = Button.builder(
                Component.literal(hasTexture ? "Texture" : "Texture"),
                btn -> openTexturePicker()
        ).bounds(fieldX, y, thirdW, ROW_H).build();
        if (isNew) {
            textureBtn.active = false;
            textureBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal("Save the item first, then set its texture.")));
        } else {
            textureBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal(hasTexture ? "Texture set — click to change" : "Choose a texture")));
        }
        this.addRenderableWidget(textureBtn);

        Button foodBtn = Button.builder(
                Component.literal(def.food != null ? "Food ✓" : "Food"),
                btn -> this.minecraft.setScreen(new FoodEditorScreen(this, modName, def))
        ).bounds(fieldX + thirdW + 6, y, thirdW, ROW_H).build();
        this.addRenderableWidget(foodBtn);

        Button tagsBtn = Button.builder(
                Component.literal(def.tags != null && !def.tags.isEmpty()
                        ? "Tags (" + def.tags.size() + ")" : "Tags"),
                btn -> {
                    if (def.tags == null) def.tags = new java.util.ArrayList<>();
                    this.minecraft.setScreen(new TagEditorScreen(this, def.tags, false,
                            def.id == null || def.id.isBlank() ? "item" : def.id));
                }
        ).bounds(fieldX + 2 * (thirdW + 6), y, thirdW, ROW_H).build();
        this.addRenderableWidget(tagsBtn);

        int tooltipBlockY = panelY + 26 + 7 * ROW_STEP + 14;
        int tooltipX = panelX + 18;
        int tooltipW = panelW - 36;

        tooltipFields.clear();
        int ty = tooltipBlockY;
        for (int i = 0; i < MAX_TOOLTIP_LINES; i++) {
            EditBox tl = new EditBox(this.font, tooltipX, ty, tooltipW, ROW_H,
                    Component.literal("tooltip " + (i + 1)));
            tl.setMaxLength(80);
            if (def.tooltipLines != null && i < def.tooltipLines.size()) {
                tl.setValue(def.tooltipLines.get(i));
            }
            this.addRenderableWidget(tl);
            tooltipFields.add(tl);
            ty += ROW_STEP;
        }

        int tooltipBlockEnd = tooltipBlockY + 3 * ROW_STEP - ROW_GAP;
        int hintY = tooltipBlockEnd + 8;
        int footerY = hintY + 16;


        int saveCancelW = 70;
        int saveCancelGap = 6;
        int rightEdge = panelX + panelW - 12;
        int cancelX = rightEdge - saveCancelW;
        int saveX = cancelX - saveCancelGap - saveCancelW;

        this.addRenderableWidget(Button.builder(
                Component.literal("Save"),
                btn -> trySave()
        ).bounds(saveX, footerY, saveCancelW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose()
        ).bounds(cancelX, footerY, saveCancelW, 20).build());

        if (!isNew) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Delete").withStyle(ChatFormatting.RED),
                    btn -> this.minecraft.setScreen(
                            new ConfirmDeleteScreen(this, modName, def.id, def.displayName))
            ).bounds(panelX + 12, footerY, 60, 20).build());


            int topRightX = panelX + panelW - 12;
            int topRightY = panelY + 6;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Copy"),
                    btn -> duplicateItem()
            ).bounds(topRightX - 38, topRightY, 38, 14).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("JSON"),
                    btn -> this.minecraft.setScreen(new ViewJsonScreen(this, "JSON: " + def.id, def))
            ).bounds(topRightX - 38 - 4 - 38, topRightY, 38, 14).build());
        }
    }

    private void duplicateItem() {

        ItemDefinition copy = new ItemDefinition();
        copy.id = "";
        copy.displayName = def.displayName + " Copy";
        copy.maxStackSize = def.maxStackSize;
        copy.rarity = def.rarity;
        copy.glow = def.glow;
        copy.fuelBurnTime = def.fuelBurnTime;
        copy.tooltipLines = def.tooltipLines == null ? java.util.Collections.emptyList()
                : new java.util.ArrayList<>(def.tooltipLines);
        copy.food = def.food;

        this.minecraft.setScreen(new ItemEditorScreen(listParent, modName, copy, true));
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
        int newStack = stackSizeBtn.getValue();
        String newRarity = rarityBtn.getValue();
        boolean newGlow = glowBox.selected();
        int newFuel;
        try { newFuel = fuelField.getValue().isEmpty() ? 0 : Integer.parseInt(fuelField.getValue()); }
        catch (NumberFormatException e) { newFuel = 0; }

        List<String> tooltips = new ArrayList<>();
        for (EditBox tl : tooltipFields) {
            String v = tl.getValue().trim();
            if (!v.isEmpty()) tooltips.add(v);
        }

        def.id = newId;
        def.displayName = newDisplay.isEmpty() ? newId : newDisplay;
        def.maxStackSize = newStack;
        def.rarity = newRarity;
        def.glow = newGlow;
        def.fuelBurnTime = newFuel;
        def.tooltipLines = tooltips;

        String err = def.validate();
        if (err != null) { errorMessage = err; return; }

        String json = GSON.toJson(def);
        PacketDistributor.sendToServer(new SaveItemPacket(modName, def.id, json));
        listParent.onItemChanged();
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
                        "Choose a 16x16 PNG for " + def.id,
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
        }, "Modkit-FilePicker");
        picker.setDaemon(true);
        picker.start();
    }


    private void handlePickedFile(String chosenPath) {
        Path source = Path.of(chosenPath);
        if (!Files.isRegularFile(source)) { errorMessage = "File not found: " + chosenPath; return; }
        if (!chosenPath.toLowerCase().endsWith(".png")) { errorMessage = "Selected file is not a .png"; return; }

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

        PacketDistributor.sendToServer(new SetTexturePacket(modName, def.id, bytes));

        hasTexture = true;
        errorMessage = null;
        this.clearWidgets();
        this.init();
    }


    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 18;

        int y = panelY + 26 + 4;

        String[] labels = {"ID", "Display", "Stack", "Rarity", "Glow", "Fuel", "Tex/Food/Tag"};
        for (String l : labels) {
            gfx.drawString(this.font, l, labelX, y, LABEL_COLOR, LABEL_SHADOW);
            y += ROW_STEP;
        }



        int tooltipBlockY = panelY + 26 + 7 * ROW_STEP + 14;
        gfx.drawString(this.font, "Tooltips:", labelX, tooltipBlockY - 12, LABEL_COLOR, LABEL_SHADOW);


        int tooltipBlockEnd = tooltipBlockY + 3 * ROW_STEP - ROW_GAP;
        int hintY = tooltipBlockEnd + 8;
        if (errorMessage != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(errorMessage),
                    panelX + panelW / 2, hintY, 0xAA0000);
        } else if (!isNew) {
            gfx.drawCenteredString(this.font,
                    Component.literal("ID locked — delete & recreate to rename")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    panelX + panelW / 2, hintY, 0xFFFFFF);
        }
    }



    private static int snapStack(int v) { return v <= 1 ? 1 : v <= 16 ? 16 : 64; }
    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    private static ChatFormatting rarityColor(String r) {
        return switch (r) {
            case "uncommon" -> ChatFormatting.YELLOW;
            case "rare" -> ChatFormatting.AQUA;
            case "epic" -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.WHITE;
        };
    }
}
