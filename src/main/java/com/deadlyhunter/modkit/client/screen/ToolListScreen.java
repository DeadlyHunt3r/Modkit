package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.tool.ToolDefinition;
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

public class ToolListScreen extends ModkitBaseScreen {

    private static final int ROWS_VISIBLE = 6;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;

    private final String modName;
    private final List<ToolDefinition> tools = new ArrayList<>();
    private int scroll = 0;

    public ToolListScreen(ProjectScreen parent, String modName) {
        super(Component.literal("Tools — " + modName), parent);
        this.modName = modName;
        this.panelW = 260;
        this.panelH = 252;
    }

    @Override
    protected void init() {
        super.init();
        loadTools();
        rebuildList();
    }

    private void loadTools() {
        tools.clear();
        Path dir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("tools");
        if (!Files.isDirectory(dir)) return;

        Gson gson = new Gson();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            ToolDefinition def = gson.fromJson(Files.readString(p), ToolDefinition.class);
                            if (def != null && def.validate() == null) tools.add(def);
                        } catch (Exception ignored) {}
                    });
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list tools", e);
        }
        tools.sort(Comparator.comparing(d -> d.id));
    }

    private void rebuildList() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int listW = 220;
        int listX = centerX - listW / 2;
        int listY = panelY + 40;

        int rowCount = Math.min(ROWS_VISIBLE, tools.size() - scroll);
        for (int i = 0; i < rowCount; i++) {
            int idx = scroll + i;
            ToolDefinition def = tools.get(idx);
            int rowY = listY + i * (ROW_H + ROW_GAP);

            String label = "§7[" + cap(def.toolType) + "]§r " + def.displayName + " §8(" + def.id + ")";

            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> this.minecraft.setScreen(new ToolEditorScreen(this, modName, def, false))
            ).bounds(listX, rowY, listW, ROW_H).build());
        }

        if (tools.size() > ROWS_VISIBLE) {
            int sX = listX + listW + 4;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) { scroll--; rebuildList(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (scroll + ROWS_VISIBLE < tools.size()) { scroll++; rebuildList(); } })
                    .bounds(sX, listY + (ROWS_VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + ROWS_VISIBLE < tools.size();
            this.addRenderableWidget(down);
        }

        int footerY = panelY + panelH - 74;
        int btnW = 110;
        int gap = 4;
        int row1X = centerX - btnW - gap / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Pickaxe"),
                btn -> openEditor(blank("pickaxe"), true)
        ).bounds(row1X, footerY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Axe"),
                btn -> openEditor(blank("axe"), true)
        ).bounds(row1X + btnW + gap, footerY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Shovel"),
                btn -> openEditor(blank("shovel"), true)
        ).bounds(row1X, footerY + 22, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Hoe"),
                btn -> openEditor(blank("hoe"), true)
        ).bounds(row1X + btnW + gap, footerY + 22, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX - 50, footerY + 48, 100, 20).build());
    }

    private void openEditor(ToolDefinition def, boolean isNew) {
        this.minecraft.setScreen(new ToolEditorScreen(this, modName, def, isNew));
    }

    private ToolDefinition blank(String toolType) {
        ToolDefinition d = new ToolDefinition();
        d.toolType = toolType;
        d.id = "new_" + toolType;
        d.displayName = "New " + cap(toolType);
        d.tier = "iron";
        float[] defaults = ToolDefinition.vanillaDefaultsFor(toolType);
        d.damageBonus = defaults[0];
        d.attackSpeed = defaults[1];
        return d;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tools.size() > ROWS_VISIBLE) {
            int newScroll = scroll - (int) Math.signum(delta);
            newScroll = Math.max(0, Math.min(tools.size() - ROWS_VISIBLE, newScroll));
            if (newScroll != scroll) { scroll = newScroll; rebuildList(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (tools.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("No tools yet — pick a type below").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 80, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal(tools.size() + " tool(s)").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 24, 0xFFFFFF);
        }
    }

    public void onToolChanged() {
        loadTools();
        rebuildList();
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
