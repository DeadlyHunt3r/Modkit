package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.weapon.WeaponDefinition;
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

public class WeaponListScreen extends ModkitBaseScreen {

    private static final int ROWS_VISIBLE = 6;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;

    private final String modName;
    private final List<WeaponDefinition> weapons = new ArrayList<>();
    private int scroll = 0;

    public WeaponListScreen(ProjectScreen parent, String modName) {
        super(Component.literal("Weapons — " + modName), parent);
        this.modName = modName;
    }

    @Override
    protected void init() {
        super.init();
        loadWeapons();
        rebuildList();
    }

    private void loadWeapons() {
        weapons.clear();
        Path dir = WorkspaceManager.getWorkspacePath(modName).resolve("modkit").resolve("weapons");
        if (!Files.isDirectory(dir)) return;

        Gson gson = new Gson();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            WeaponDefinition def = gson.fromJson(Files.readString(p), WeaponDefinition.class);
                            if (def != null && def.validate() == null) weapons.add(def);
                        } catch (Exception ignored) {}
                    });
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to list weapons", e);
        }
        weapons.sort(Comparator.comparing(d -> d.id));
    }

    private void rebuildList() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int listW = 220;
        int listX = centerX - listW / 2;
        int listY = panelY + 40;

        int rowCount = Math.min(ROWS_VISIBLE, weapons.size() - scroll);
        for (int i = 0; i < rowCount; i++) {
            int idx = scroll + i;
            WeaponDefinition def = weapons.get(idx);
            int rowY = listY + i * (ROW_H + ROW_GAP);

            String label = def.displayName + " §8(" + def.tier + ", " + def.id + ")";

            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> this.minecraft.setScreen(new WeaponEditorScreen(this, modName, def, false))
            ).bounds(listX, rowY, listW, ROW_H).build());
        }

        if (weapons.size() > ROWS_VISIBLE) {
            int sX = listX + listW + 4;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) { scroll--; rebuildList(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (scroll + ROWS_VISIBLE < weapons.size()) { scroll++; rebuildList(); } })
                    .bounds(sX, listY + (ROWS_VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + ROWS_VISIBLE < weapons.size();
            this.addRenderableWidget(down);
        }

        int footerY = panelY + panelH - 30;
        this.addRenderableWidget(Button.builder(
                Component.literal("+ New Sword"),
                btn -> this.minecraft.setScreen(new WeaponEditorScreen(this, modName, newBlank(), true))
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    private WeaponDefinition newBlank() {
        WeaponDefinition d = new WeaponDefinition();
        d.id = "new_sword";
        d.displayName = "New Sword";
        d.weaponType = "sword";
        d.tier = "iron";
        return d;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (weapons.size() > ROWS_VISIBLE) {
            int newScroll = scroll - (int) Math.signum(scrollY);
            newScroll = Math.max(0, Math.min(weapons.size() - ROWS_VISIBLE, newScroll));
            if (newScroll != scroll) { scroll = newScroll; rebuildList(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (weapons.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("No weapons yet — click + New Sword").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 80, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal(weapons.size() + " weapon(s)").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 24, 0xFFFFFF);
        }
    }

    public void onWeaponChanged() {
        loadWeapons();
        rebuildList();
    }
}
