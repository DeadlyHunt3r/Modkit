package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
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

public class BlockListScreen extends ModkitBaseScreen {

    private static final int ROWS_VISIBLE = 6;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;

    private final String modName;
    private final List<BlockDefinition> blocks = new ArrayList<>();
    private int scroll = 0;
    private boolean cannotMakeVariant = false;

    public BlockListScreen(ProjectScreen parent, String modName) {
        super(Component.literal("Blocks — " + modName), parent);
        this.modName = modName;
    }

    @Override
    protected void init() {
        super.init();
        loadBlocks();
        rebuildList();
    }

    private void loadBlocks() {
        blocks.clear();
        Path blocksDir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("blocks");
        if (!Files.isDirectory(blocksDir)) return;

        Gson gson = new Gson();
        try (Stream<Path> stream = Files.list(blocksDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            BlockDefinition def = gson.fromJson(Files.readString(p), BlockDefinition.class);
                            if (def != null && def.validate() == null) blocks.add(def);
                        } catch (Exception ignored) {}
                    });
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list blocks", e);
        }
        blocks.sort(Comparator.comparing(d -> d.id));
    }

    private void rebuildList() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int listW = 200;
        int listX = centerX - listW / 2;
        int listY = panelY + 40;

        int rowCount = Math.min(ROWS_VISIBLE, blocks.size() - scroll);
        for (int i = 0; i < rowCount; i++) {
            int idx = scroll + i;
            BlockDefinition def = blocks.get(idx);
            int rowY = listY + i * (ROW_H + ROW_GAP);

            String label = def.isVariant()
                    ? "§b[" + variantShort(def.variantType) + "]§r " + def.displayName
                    : def.displayName + " §8(" + def.id + ")";

            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> {
                        if (def.isVariant()) {
                            this.minecraft.setScreen(new VariantEditorScreen(this, modName, def, false));
                        } else {
                            this.minecraft.setScreen(new BlockEditorScreen(this, modName, def, false));
                        }
                    }
            ).bounds(listX, rowY, listW, ROW_H).build());
        }

        if (blocks.size() > ROWS_VISIBLE) {
            int sX = listX + listW + 4;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) { scroll--; rebuildList(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (scroll + ROWS_VISIBLE < blocks.size()) { scroll++; rebuildList(); } })
                    .bounds(sX, listY + (ROWS_VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + ROWS_VISIBLE < blocks.size();
            this.addRenderableWidget(down);
        }

        int footerY = panelY + panelH - 30;
        int bw = 76;
        this.addRenderableWidget(Button.builder(
                Component.literal("+ Block"),
                btn -> this.minecraft.setScreen(new BlockEditorScreen(this, modName, newBlank(), true))
        ).bounds(centerX - bw - bw / 2 - 4, footerY, bw, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Variant"),
                btn -> openNewVariant()
        ).bounds(centerX - bw / 2, footerY, bw, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX + bw / 2 + 4, footerY, bw, 20).build());
    }

    private void openNewVariant() {
        boolean hasBase = false;
        for (BlockDefinition b : blocks) {
            if (!b.isVariant()) { hasBase = true; break; }
        }
        if (!hasBase) {
            cannotMakeVariant = true;
            rebuildList();
            return;
        }
        cannotMakeVariant = false;
        this.minecraft.setScreen(new VariantEditorScreen(this, modName, null, true));
    }

    private static String variantShort(String type) {
        return switch (type) {
            case "slab" -> "Slab";
            case "stairs" -> "Stair";
            case "wall" -> "Wall";
            case "fence" -> "Fence";
            default -> "?";
        };
    }

    private BlockDefinition newBlank() {
        BlockDefinition d = new BlockDefinition();
        d.id = "new_block";
        d.displayName = "New Block";
        return d;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (blocks.size() > ROWS_VISIBLE) {
            int newScroll = scroll - (int) Math.signum(scrollY);
            newScroll = Math.max(0, Math.min(blocks.size() - ROWS_VISIBLE, newScroll));
            if (newScroll != scroll) { scroll = newScroll; rebuildList(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (blocks.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("No blocks yet — click + Block").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 80, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal(blocks.size() + " block(s)").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 24, 0xFFFFFF);
        }
        if (cannotMakeVariant) {
            gfx.drawCenteredString(this.font,
                    Component.literal("Make a normal block first, then a variant of it.")
                            .withStyle(ChatFormatting.RED),
                    this.width / 2, panelY + panelH - 48, 0xFFFFFF);
        }
    }

    public void onBlockChanged() {
        loadBlocks();
        rebuildList();
    }
}
