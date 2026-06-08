package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
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

public class ItemListScreen extends ModkitBaseScreen {

    private static final int ROWS_VISIBLE = 6;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;

    private final String modName;
    private final List<ItemDefinition> items = new ArrayList<>();
    private int scroll = 0;

    public ItemListScreen(ProjectScreen parent, String modName) {
        super(Component.literal("Items — " + modName), parent);
        this.modName = modName;
    }

    @Override
    protected void init() {
        super.init();
        loadItems();
        rebuildList();
    }

    private void loadItems() {
        items.clear();
        Path itemsDir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("items");
        if (!Files.isDirectory(itemsDir)) return;

        Gson gson = new Gson();
        try (Stream<Path> stream = Files.list(itemsDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            ItemDefinition def = gson.fromJson(Files.readString(p), ItemDefinition.class);
                            if (def != null && def.validate() == null) items.add(def);
                        } catch (Exception ignored) {}
                    });
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list items", e);
        }
        items.sort(Comparator.comparing(d -> d.id));
    }

    private void rebuildList() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int listW = 200;
        int listX = centerX - listW / 2;
        int listY = panelY + 40;

        int rowCount = Math.min(ROWS_VISIBLE, items.size() - scroll);
        for (int i = 0; i < rowCount; i++) {
            int idx = scroll + i;
            ItemDefinition def = items.get(idx);
            int rowY = listY + i * (ROW_H + ROW_GAP);

            this.addRenderableWidget(Button.builder(
                    Component.literal(def.displayName + " §8(" + def.id + ")"),
                    btn -> this.minecraft.setScreen(new ItemEditorScreen(this, modName, def, false))
            ).bounds(listX, rowY, listW, ROW_H).build());
        }

        if (items.size() > ROWS_VISIBLE) {
            int sX = listX + listW + 4;
            Button up = Button.builder(Component.literal("▲"), b -> { if (scroll > 0) { scroll--; rebuildList(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"), b -> { if (scroll + ROWS_VISIBLE < items.size()) { scroll++; rebuildList(); } })
                    .bounds(sX, listY + (ROWS_VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + ROWS_VISIBLE < items.size();
            this.addRenderableWidget(down);
        }

        int footerY = panelY + panelH - 30;
        this.addRenderableWidget(Button.builder(
                Component.literal("+ New Item"),
                btn -> this.minecraft.setScreen(new ItemEditorScreen(this, modName, newBlank(), true))
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    private ItemDefinition newBlank() {
        ItemDefinition d = new ItemDefinition();
        d.id = "new_item";
        d.displayName = "New Item";
        d.maxStackSize = 64;
        d.rarity = "common";
        return d;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (items.size() > ROWS_VISIBLE) {
            int newScroll = scroll - (int) Math.signum(delta);
            newScroll = Math.max(0, Math.min(items.size() - ROWS_VISIBLE, newScroll));
            if (newScroll != scroll) { scroll = newScroll; rebuildList(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (items.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("No items yet — click + New Item").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 80, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal(items.size() + " item(s)").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 24, 0xFFFFFF);
        }
    }

    public void onItemChanged() {
        loadItems();
        rebuildList();
    }
}
