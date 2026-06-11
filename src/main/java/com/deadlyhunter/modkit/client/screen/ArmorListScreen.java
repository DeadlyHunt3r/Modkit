package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.armor.ArmorSetDefinition;
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

public class ArmorListScreen extends ModkitBaseScreen {

    private static final int ROWS_VISIBLE = 6;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;

    private final String modName;
    private final List<ArmorSetDefinition> sets = new ArrayList<>();
    private int scroll = 0;

    public ArmorListScreen(ProjectScreen parent, String modName) {
        super(Component.literal("Armor Sets — " + modName), parent);
        this.modName = modName;
    }

    @Override
    protected void init() {
        super.init();
        loadSets();
        rebuildList();
    }

    private void loadSets() {
        sets.clear();
        Path dir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("armor");
        if (!Files.isDirectory(dir)) return;

        Gson gson = new Gson();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            ArmorSetDefinition def = gson.fromJson(Files.readString(p), ArmorSetDefinition.class);
                            if (def != null && def.validate() == null) sets.add(def);
                        } catch (Exception ignored) {}
                    });
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list armor sets", e);
        }
        sets.sort(Comparator.comparing(d -> d.id));
    }

    private void rebuildList() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int listW = 220;
        int listX = centerX - listW / 2;
        int listY = panelY + 40;

        int rowCount = Math.min(ROWS_VISIBLE, sets.size() - scroll);
        for (int i = 0; i < rowCount; i++) {
            int idx = scroll + i;
            ArmorSetDefinition def = sets.get(idx);
            int rowY = listY + i * (ROW_H + ROW_GAP);

            int pieceCount = 0;
            for (String pt : ArmorSetDefinition.PIECE_TYPES) if (def.hasPiece(pt)) pieceCount++;
            String label = def.displayName + " §8(" + def.tier + ", " + pieceCount + " pcs)";

            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> this.minecraft.setScreen(new ArmorSetEditorScreen(this, modName, def, false))
            ).bounds(listX, rowY, listW, ROW_H).build());
        }

        if (sets.size() > ROWS_VISIBLE) {
            int sX = listX + listW + 4;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) { scroll--; rebuildList(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (scroll + ROWS_VISIBLE < sets.size()) { scroll++; rebuildList(); } })
                    .bounds(sX, listY + (ROWS_VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + ROWS_VISIBLE < sets.size();
            this.addRenderableWidget(down);
        }

        int footerY = panelY + panelH - 30;
        this.addRenderableWidget(Button.builder(
                Component.literal("+ New Armor Set"),
                btn -> this.minecraft.setScreen(new ArmorSetEditorScreen(this, modName, newBlank(), true))
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    private ArmorSetDefinition newBlank() {
        ArmorSetDefinition d = new ArmorSetDefinition();
        d.id = "new_armor";
        d.displayName = "New Armor";
        d.tier = "iron";
        return d;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (sets.size() > ROWS_VISIBLE) {
            int newScroll = scroll - (int) Math.signum(delta);
            newScroll = Math.max(0, Math.min(sets.size() - ROWS_VISIBLE, newScroll));
            if (newScroll != scroll) { scroll = newScroll; rebuildList(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (sets.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("No armor sets yet — click + New Armor Set").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 80, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal(sets.size() + " armor set(s)").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 24, 0xFFFFFF);
        }
    }

    public void onArmorChanged() {
        loadSets();
        rebuildList();
    }
}
