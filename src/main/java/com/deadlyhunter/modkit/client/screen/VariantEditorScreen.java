package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.deadlyhunter.modkit.network.DeleteBlockPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import com.deadlyhunter.modkit.network.SaveBlockPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class VariantEditorScreen extends ModkitBaseScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final BlockListScreen listParent;
    private final String modName;
    private final BlockDefinition existing;
    private final boolean isNew;

    private final List<BlockDefinition> baseBlocks = new ArrayList<>();
    private CycleButton<String> baseBtn;
    private CycleButton<String> formBtn;
    private String error = null;

    public VariantEditorScreen(BlockListScreen parent, String modName,
                                BlockDefinition existing, boolean isNew) {
        super(Component.literal(isNew ? "New Variant" : "Variant: " + (existing != null ? existing.id : "")), parent);
        this.listParent = parent;
        this.modName = modName;
        this.existing = existing;
        this.isNew = isNew;
        this.panelW = 320;
        this.panelH = 210;
    }

    public BlockListScreen getListParent() { return listParent; }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        loadBaseBlocks();

        int fieldX = panelX + 110;
        int fieldW = panelW - 126;

        if (isNew) {
            List<String> names = new ArrayList<>();
            for (BlockDefinition b : baseBlocks) names.add(b.id);
            if (names.isEmpty()) names.add("(none)");

            baseBtn = CycleButton.<String>builder(Component::literal)
                    .withValues(names)
                    .withInitialValue(names.get(0))
                    .create(fieldX, panelY + 40, fieldW, 18, Component.literal(""));
            this.addRenderableWidget(baseBtn);

            formBtn = CycleButton.<String>builder(s -> Component.literal(formLabel(s)))
                    .withValues("slab", "stairs", "wall", "fence")
                    .withInitialValue("slab")
                    .displayOnlyValue()
                    .create(fieldX, panelY + 66, fieldW, 18, Component.literal(""));
            this.addRenderableWidget(formBtn);

            this.addRenderableWidget(Button.builder(
                    Component.literal("Create"),
                    b -> createVariant()
            ).bounds(panelX + panelW / 2 - 103, panelY + panelH - 30, 100, 20).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("Cancel"),
                    b -> this.onClose()
            ).bounds(panelX + panelW / 2 + 3, panelY + panelH - 30, 100, 20).build());
        } else {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Delete").withStyle(ChatFormatting.RED),
                    b -> deleteVariant()
            ).bounds(panelX + panelW / 2 - 103, panelY + panelH - 30, 100, 20).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("Back"),
                    b -> this.onClose()
            ).bounds(panelX + panelW / 2 + 3, panelY + panelH - 30, 100, 20).build());
        }
    }

    private void loadBaseBlocks() {
        baseBlocks.clear();
        Path dir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("blocks");
        if (!Files.isDirectory(dir)) return;
        Gson g = new Gson();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.toString().endsWith(".json")).sorted().forEach(p -> {
                try {
                    BlockDefinition b = g.fromJson(Files.readString(p), BlockDefinition.class);
                    if (b != null && !b.isVariant() && b.validate() == null) baseBlocks.add(b);
                } catch (Exception ignored) {}
            });
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list base blocks", e);
        }
    }

    private void createVariant() {
        if (baseBlocks.isEmpty()) { error = "No base block available."; return; }
        String baseId = baseBtn.getValue();
        BlockDefinition base = null;
        for (BlockDefinition b : baseBlocks) if (b.id.equals(baseId)) base = b;
        if (base == null) { error = "Base block not found."; return; }

        String form = formBtn.getValue();
        BlockDefinition v = copyFrom(base, form);

        String dupCheck = v.validate();
        if (dupCheck != null) { error = dupCheck; return; }

        Path target = WorkspaceManager.getWorkspacePath(modName)
                .resolve("modkit").resolve("blocks").resolve(v.id + ".json");
        if (Files.exists(target)) {
            error = "Variant '" + v.id + "' already exists.";
            return;
        }

        PacketDistributor.sendToServer(new SaveBlockPacket(modName, v.id, GSON.toJson(v)));
        listParent.onBlockChanged();
        this.minecraft.setScreen(listParent);
    }

    private BlockDefinition copyFrom(BlockDefinition base, String form) {
        BlockDefinition v = new BlockDefinition();
        v.id = base.id + "_" + form;
        v.displayName = base.displayName + " " + formLabel(form);
        v.variantType = form;
        v.textureSource = base.id;

        v.hardness = base.hardness;
        v.resistance = base.resistance;
        v.tool = base.tool;
        v.toolTier = base.toolTier;
        v.requiresCorrectTool = base.requiresCorrectTool;
        v.lightEmission = base.lightEmission;
        v.soundGroup = base.soundGroup;
        v.friction = base.friction;

        v.dropMode = "self";
        v.dropSelf = true;
        return v;
    }

    private void deleteVariant() {
        if (existing == null) return;
        PacketDistributor.sendToServer(new DeleteBlockPacket(modName, existing.id));
        listParent.onBlockChanged();
        this.minecraft.setScreen(listParent);
    }

    private static String formLabel(String form) {
        return switch (form) {
            case "slab" -> "Slab";
            case "stairs" -> "Stairs";
            case "wall" -> "Wall";
            case "fence" -> "Fence";
            default -> form;
        };
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int labelX = panelX + 16;

        if (isNew) {
            gfx.drawString(this.font, "Base Block", labelX, panelY + 44, 0xFFFFFF, true);
            gfx.drawString(this.font, "Form", labelX, panelY + 70, 0xFFFFFF, true);

            String baseId = baseBtn != null ? baseBtn.getValue() : "?";
            String form = formBtn != null ? formBtn.getValue() : "slab";
            gfx.drawString(this.font,
                    Component.literal("→ creates " + baseId + "_" + form).withStyle(ChatFormatting.GRAY),
                    labelX, panelY + 96, 0xFFFFFF);
            gfx.drawString(this.font,
                    Component.literal("inherits texture & stats from base").withStyle(ChatFormatting.DARK_GRAY),
                    labelX, panelY + 110, 0xFFFFFF);
        } else if (existing != null) {
            gfx.drawString(this.font, "ID: " + existing.id, labelX, panelY + 44, 0xFFFFFF, true);
            gfx.drawString(this.font, "Form: " + formLabel(existing.variantType), labelX, panelY + 62, 0xFFFFFF, true);
            gfx.drawString(this.font, "Texture from: " + existing.textureSource, labelX, panelY + 80, 0xCCCCCC, true);
            gfx.drawString(this.font,
                    Component.literal("Variants copy their base's values once.").withStyle(ChatFormatting.DARK_GRAY),
                    labelX, panelY + 104, 0xFFFFFF);
        }

        if (error != null) {
            gfx.drawCenteredString(this.font, Component.literal(error),
                    panelX + panelW / 2, panelY + panelH - 48, 0xFF5555);
        }
    }
}
