package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.core.ProjectInfo;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.deadlyhunter.modkit.network.ExportProjectPacket;
import com.deadlyhunter.modkit.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class ProjectScreen extends ModkitBaseScreen {

    private final String modName;
    private final ProjectInfo info;

    public ProjectScreen(WorkspaceSelectionScreen parent, String modName) {
        super(Component.literal("Project: " + modName), parent);
        this.modName = modName;
        this.info = WorkspaceManager.loadProject(modName);
        this.panelW = 248;
        this.panelH = 240;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int colW = 100;
        int colH = 20;
        int gap = 4;

        int leftX = centerX - colW - gap / 2;
        int rightX = centerX + gap / 2;
        int startY = panelY + 50;
        int rowSpacing = colH + gap;

        addActive("Items", leftX, startY, colW, colH,
                () -> this.minecraft.setScreen(new ItemListScreen(this, modName)));
        addActive("Blocks", rightX, startY, colW, colH,
                () -> this.minecraft.setScreen(new BlockListScreen(this, modName)));

        addActive("Ores", leftX, startY + rowSpacing, colW, colH,
                () -> this.minecraft.setScreen(new OreListScreen(this, modName)));
        addActive("Recipes", rightX, startY + rowSpacing, colW, colH,
                () -> this.minecraft.setScreen(new RecipeListScreen(this, modName)));

        addActive("Weapons", leftX, startY + 2 * rowSpacing, colW, colH,
                () -> this.minecraft.setScreen(new WeaponListScreen(this, modName)));
        addActive("Tools", rightX, startY + 2 * rowSpacing, colW, colH,
                () -> this.minecraft.setScreen(new ToolListScreen(this, modName)));

        addActive("Armor", leftX, startY + 3 * rowSpacing, colW, colH,
                () -> this.minecraft.setScreen(new ArmorListScreen(this, modName)));
        addActive("Settings", rightX, startY + 3 * rowSpacing, colW, colH,
                () -> this.minecraft.setScreen(new ProjectSettingsScreen(this, modName)));

        int footerY = panelY + panelH - 30;
        this.addRenderableWidget(Button.builder(
                Component.literal("Export to .jar"),
                btn -> {
                    ModNetworking.CHANNEL.sendToServer(new ExportProjectPacket(modName));
                }
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    private void addActive(String label, int x, int y, int w, int h, Runnable onClick) {
        this.addRenderableWidget(Button.builder(
                Component.literal(label),
                btn -> onClick.run()
        ).bounds(x, y, w, h).build());
    }

    private void addComingSoon(String label, int x, int y, int w, int h) {
        Button b = Button.builder(
                Component.literal(label).withStyle(ChatFormatting.DARK_GRAY),
                btn -> {}
        ).bounds(x, y, w, h)
                .tooltip(Tooltip.create(Component.literal("Coming in a later phase.")))
                .build();
        b.active = false;
        this.addRenderableWidget(b);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (info != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(info.modId).withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 24, 0xFFFFFF);
        }
    }
}
