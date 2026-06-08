package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.core.ProjectInfo;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

public class WorkspaceSelectionScreen extends ModkitBaseScreen {

    private static final int ROWS_VISIBLE = 6;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;

    private List<String> workspaces;
    private int scroll = 0;

    public WorkspaceSelectionScreen(ModkitMainScreen parent) {
        super(Component.literal("My Projects"), parent);
    }

    @Override
    protected void init() {
        super.init();
        workspaces = WorkspaceManager.listWorkspaces();
        rebuildList();
    }

    private void rebuildList() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int listW = 220;
        int delW = 20;
        int listX = centerX - (listW + delW + 4) / 2;
        int listY = panelY + 40;

        int rowCount = Math.min(ROWS_VISIBLE, workspaces.size() - scroll);
        for (int i = 0; i < rowCount; i++) {
            int idx = scroll + i;
            String name = workspaces.get(idx);
            int rowY = listY + i * (ROW_H + ROW_GAP);

            ProjectInfo info = WorkspaceManager.loadProject(name);
            String label = info != null ? info.displayName + " §8(" + name + ")" : name + " §c(corrupt)";
            final ProjectInfo finfo = info;

            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> {
                        if (finfo != null) this.minecraft.setScreen(new ProjectScreen(this, name));
                    }
            ).bounds(listX, rowY, listW, ROW_H).build());

            String displayForConfirm = info != null ? info.displayName : name;
            this.addRenderableWidget(Button.builder(
                    Component.literal("X").withStyle(ChatFormatting.RED),
                    btn -> this.minecraft.setScreen(
                            new ConfirmDeleteWorkspaceScreen(this, name, displayForConfirm))
            ).bounds(listX + listW + 4, rowY, delW, ROW_H).build());
        }

        if (workspaces.size() > ROWS_VISIBLE) {
            int sX = listX + listW + delW + 8;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) { scroll--; rebuildList(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (scroll + ROWS_VISIBLE < workspaces.size()) { scroll++; rebuildList(); } })
                    .bounds(sX, listY + (ROWS_VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + ROWS_VISIBLE < workspaces.size();
            this.addRenderableWidget(down);
        }

        int footerY = panelY + panelH - 30;
        this.addRenderableWidget(Button.builder(
                Component.literal("+ New Project"),
                btn -> this.minecraft.setScreen(new NewWorkspaceScreen(this))
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (workspaces.size() > ROWS_VISIBLE) {
            int newScroll = scroll - (int) Math.signum(delta);
            newScroll = Math.max(0, Math.min(workspaces.size() - ROWS_VISIBLE, newScroll));
            if (newScroll != scroll) { scroll = newScroll; rebuildList(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (workspaces.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("No projects yet — click + New Project").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 80, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal(workspaces.size() + " project(s)").withStyle(ChatFormatting.GRAY),
                    this.width / 2, panelY + 24, 0xFFFFFF);
        }
    }

    public void refresh() {
        workspaces = WorkspaceManager.listWorkspaces();
        scroll = Math.min(scroll, Math.max(0, workspaces.size() - ROWS_VISIBLE));
        rebuildList();
    }
}
