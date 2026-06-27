package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.ore.OreDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class OreListScreen extends ModkitBaseScreen {

    private static final int ROWS_VISIBLE = 6;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;

    private final String modName;
    private final List<OreDefinition> ores = new ArrayList<>();
    private final List<BlockDefinition> availableBlocks = new ArrayList<>();
    private int scroll = 0;

    public OreListScreen(ProjectScreen parent, String modName) {
        super(Component.literal("Ores — " + modName), parent);
        this.modName = modName;
    }

    @Override
    protected void init() {
        super.init();
        loadOres();
        loadBlocks();
        rebuildList();
    }

    private void loadOres() {
        ores.clear();
        Path dir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("ores");
        if (!Files.isDirectory(dir)) return;

        Gson gson = new Gson();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            OreDefinition def = gson.fromJson(Files.readString(p), OreDefinition.class);
                            if (def != null && def.validate() == null) ores.add(def);
                        } catch (Exception ignored) {}
                    });
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list ores", e);
        }
        ores.sort(Comparator.comparing(d -> d.id));
    }

    private void loadBlocks() {
        availableBlocks.clear();
        Path dir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("blocks");
        if (!Files.isDirectory(dir)) return;

        Gson gson = new Gson();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            BlockDefinition def = gson.fromJson(Files.readString(p), BlockDefinition.class);
                            if (def != null && def.validate() == null) availableBlocks.add(def);
                        } catch (Exception ignored) {}
                    });
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list blocks for ore picker", e);
        }
    }

    public List<BlockDefinition> getAvailableBlocks() { return availableBlocks; }

    private void rebuildList() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int listW = 220;
        int listX = centerX - listW / 2;
        int listY = panelY + 40;

        int rowCount = Math.min(ROWS_VISIBLE, ores.size() - scroll);
        for (int i = 0; i < rowCount; i++) {
            int idx = scroll + i;
            OreDefinition def = ores.get(idx);
            int rowY = listY + i * (ROW_H + ROW_GAP);

            this.addRenderableWidget(Button.builder(
                    Component.literal(def.displayName + " §8(" + def.id + ")"),
                    btn -> this.minecraft.setScreen(new OreEditorScreen(this, modName, def, false))
            ).bounds(listX, rowY, listW, ROW_H).build());
        }

        if (ores.size() > ROWS_VISIBLE) {
            int sX = listX + listW + 4;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) { scroll--; rebuildList(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (scroll + ROWS_VISIBLE < ores.size()) { scroll++; rebuildList(); } })
                    .bounds(sX, listY + (ROWS_VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + ROWS_VISIBLE < ores.size();
            this.addRenderableWidget(down);
        }

        int footerY = panelY + panelH - 30;
        Button newBtn = Button.builder(
                Component.literal("+ New Ore"),
                btn -> this.minecraft.setScreen(new OreEditorScreen(this, modName, newBlank(), true))
        ).bounds(centerX - 102, footerY, 100, 20).build();
        if (availableBlocks.isEmpty()) {
            newBtn.active = false;
            newBtn.setTooltip(Tooltip.create(
                    Component.literal("You need at least one Block before creating an Ore.")));
        }
        this.addRenderableWidget(newBtn);

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    private OreDefinition newBlank() {
        OreDefinition d = new OreDefinition();
        d.id = "new_ore";
        d.displayName = "New Ore";
        if (!availableBlocks.isEmpty()) {
            d.blockId = availableBlocks.get(0).id;
        }
        return d;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (ores.size() > ROWS_VISIBLE) {
            int newScroll = scroll - (int) Math.signum(scrollY);
            newScroll = Math.max(0, Math.min(ores.size() - ROWS_VISIBLE, newScroll));
            if (newScroll != scroll) { scroll = newScroll; rebuildList(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (ores.isEmpty()) {
            String hint = availableBlocks.isEmpty()
                    ? "Create a Block first, then come back here"
                    : "No ores yet — click + New Ore";
            gfx.drawCenteredString(this.font,
                    Component.literal(hint).withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 80, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal(ores.size() + " ore(s)").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 24, 0xFFFFFF);
        }
    }

    public void onOreChanged() {
        loadOres();
        rebuildList();
    }
}
