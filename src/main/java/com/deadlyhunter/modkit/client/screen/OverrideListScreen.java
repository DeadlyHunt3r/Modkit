package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition;
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

public class OverrideListScreen extends ModkitBaseScreen {

    private static final int ROWS_VISIBLE = 6;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;

    private final String modName;
    private final List<RecipeOverrideDefinition> overrides = new ArrayList<>();
    private int scroll = 0;

    public OverrideListScreen(ProjectScreen parent, String modName) {
        super(Component.literal("Recipe Overrides — " + modName), parent);
        this.modName = modName;
        this.panelW = 280;
        this.panelH = 240;
    }

    @Override
    protected void init() {
        super.init();
        loadOverrides();
        rebuildList();
    }

    private void loadOverrides() {
        overrides.clear();
        Path dir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("overrides");
        if (!Files.isDirectory(dir)) return;

        Gson gson = new Gson();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            RecipeOverrideDefinition def = gson.fromJson(Files.readString(p), RecipeOverrideDefinition.class);
                            if (def != null && def.validate() == null) overrides.add(def);
                        } catch (Exception ignored) {}
                    });
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list overrides", e);
        }
        overrides.sort(Comparator.comparing(d -> d.id));
    }

    private void rebuildList() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int listW = 230;
        int listX = centerX - listW / 2;
        int listY = panelY + 40;

        int rowCount = Math.min(ROWS_VISIBLE, overrides.size() - scroll);
        for (int i = 0; i < rowCount; i++) {
            int idx = scroll + i;
            RecipeOverrideDefinition def = overrides.get(idx);
            int rowY = listY + i * (ROW_H + ROW_GAP);

            String modeTag = def.isDisable() ? "§c[OFF]§r" : "§a[REP]§r";
            String label = modeTag + " " + def.targetNamespace + ":" + truncate(def.targetRecipe, 22);

            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> this.minecraft.setScreen(new OverrideEditorScreen(this, modName, def, false))
            ).bounds(listX, rowY, listW, ROW_H).build());
        }

        if (overrides.size() > ROWS_VISIBLE) {
            int sX = listX + listW + 4;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) { scroll--; rebuildList(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (scroll + ROWS_VISIBLE < overrides.size()) { scroll++; rebuildList(); } })
                    .bounds(sX, listY + (ROWS_VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + ROWS_VISIBLE < overrides.size();
            this.addRenderableWidget(down);
        }

        int footerY = panelY + panelH - 30;
        this.addRenderableWidget(Button.builder(
                Component.literal("+ New Override"),
                btn -> this.minecraft.setScreen(new OverrideEditorScreen(this, modName, newBlank(), true))
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    private RecipeOverrideDefinition newBlank() {
        RecipeOverrideDefinition d = new RecipeOverrideDefinition();
        d.id = "new_override";
        d.displayName = "New Override";
        d.mode = "disable";
        return d;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overrides.size() > ROWS_VISIBLE) {
            int ns = scroll - (int) Math.signum(scrollY);
            ns = Math.max(0, Math.min(overrides.size() - ROWS_VISIBLE, ns));
            if (ns != scroll) { scroll = ns; rebuildList(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (overrides.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("No overrides yet — click + New Override").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 80, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal(overrides.size() + " override(s)").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 24, 0xFFFFFF);
        }
    }

    public void onOverrideChanged() {
        loadOverrides();
        rebuildList();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
