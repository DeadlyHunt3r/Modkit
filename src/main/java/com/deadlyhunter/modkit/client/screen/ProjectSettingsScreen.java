package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.core.ProjectInfo;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import net.neoforged.neoforge.network.PacketDistributor;
import com.deadlyhunter.modkit.network.UpdateProjectInfoPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ProjectSettingsScreen extends ModkitBaseScreen {

    private static final int TEXT = 0xFFFFFF;

    private final ProjectScreen projectParent;
    private final String modName;
    private final ProjectInfo info;

    private EditBox displayNameField;
    private EditBox descriptionField;
    private EditBox versionField;
    private CycleButton<String> licenseBtn;

    public ProjectSettingsScreen(ProjectScreen parent, String modName) {
        super(Component.literal("Project Settings"), parent);
        this.projectParent = parent;
        this.modName = modName;
        this.info = WorkspaceManager.loadProject(modName);
        this.panelW = 280;
        this.panelH = 245;
    }

    @Override
    protected void init() {
        super.init();

        if (info == null) return;

        int fieldX = panelX + 90;
        int fieldW = 170;
        int rowH = 18;
        int rowGap = 4;
        int y = panelY + 30;

        displayNameField = new EditBox(this.font, fieldX, y, fieldW, rowH, Component.literal("display"));
        displayNameField.setMaxLength(64);
        displayNameField.setValue(info.displayName != null ? info.displayName : info.modName);
        this.addRenderableWidget(displayNameField);
        y += rowH + rowGap;

        descriptionField = new EditBox(this.font, fieldX, y, fieldW, rowH, Component.literal("description"));
        descriptionField.setMaxLength(256);
        descriptionField.setValue(info.description != null ? info.description : "");
        this.addRenderableWidget(descriptionField);
        y += rowH + rowGap;

        versionField = new EditBox(this.font, fieldX, y, fieldW, rowH, Component.literal("version"));
        versionField.setMaxLength(16);
        versionField.setValue(info.version != null ? info.version : "0.1.0");
        this.addRenderableWidget(versionField);
        y += rowH + rowGap;

        String initialLicense = info.license != null && !info.license.isBlank()
                ? info.license : "All Rights Reserved";
        boolean known = false;
        for (String opt : ProjectInfo.LICENSE_OPTIONS) {
            if (opt.equals(initialLicense)) { known = true; break; }
        }
        if (!known) initialLicense = "All Rights Reserved";

        licenseBtn = CycleButton.<String>builder(s -> Component.literal(s))
                .withValues(ProjectInfo.LICENSE_OPTIONS)
                .withInitialValue(initialLicense)
                .displayOnlyValue()
                .create(fieldX, y, fieldW, rowH, Component.literal(""));
        this.addRenderableWidget(licenseBtn);

        int footerY = panelY + panelH - 30;
        int centerX = panelX + panelW / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("Save"),
                btn -> trySave()
        ).bounds(centerX - 102, footerY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose()
        ).bounds(centerX + 2, footerY, 100, 20).build());
    }

    private void trySave() {
        PacketDistributor.sendToServer(new UpdateProjectInfoPacket(
                modName,
                displayNameField.getValue().trim(),
                descriptionField.getValue().trim(),
                versionField.getValue().trim(),
                licenseBtn.getValue()
        ));
        this.minecraft.setScreen(projectParent);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (info == null) {
            gfx.drawCenteredString(this.font,
                    Component.literal("Could not load project info."),
                    panelX + panelW / 2, panelY + 60, TEXT);
            return;
        }

        int labelX = panelX + 18;
        int rowH = 18;
        int rowGap = 4;
        int y = panelY + 34;

        String[] labels = {"Display", "Description", "Version", "License"};
        for (String l : labels) {
            gfx.drawString(this.font, l, labelX, y, TEXT, true);
            y += rowH + rowGap;
        }

        int infoY = panelY + 30 + 4 * (rowH + rowGap) + 14;
        gfx.drawString(this.font, "Read-only:", labelX, infoY, TEXT, true);
        gfx.drawString(this.font, "Mod ID: " + info.modId, labelX, infoY + 14, TEXT, true);
        gfx.drawString(this.font, "Author: " + info.author, labelX, infoY + 26, TEXT, true);
    }
}
