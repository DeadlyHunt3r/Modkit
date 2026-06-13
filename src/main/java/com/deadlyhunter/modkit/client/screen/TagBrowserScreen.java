package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.content.tag.TagUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public class TagBrowserScreen extends ModkitBaseScreen {

    private static final int ROW_H = 18;
    private static final int ROW_GAP = 3;
    private static final int VISIBLE = 7;

    private final Consumer<String> onPick;
    private final boolean isBlock;
    private int scroll = 0;

    private EditBox tagField;
    private String prefill;
    private String message = null;

    public TagBrowserScreen(net.minecraft.client.gui.screens.Screen parent,
                             boolean isBlock, Consumer<String> onPick) {
        this(parent, isBlock, null, onPick);
    }

    public TagBrowserScreen(net.minecraft.client.gui.screens.Screen parent,
                             boolean isBlock, String prefill, Consumer<String> onPick) {
        super(Component.literal("Common Tags"), parent);
        this.onPick = onPick;
        this.isBlock = isBlock;
        this.prefill = prefill;
        this.panelW = 290;
        this.panelH = 280;
    }

    private List<String> suggestions() {
        return isBlock ? TagUtil.COMMON_BLOCK_TAGS : TagUtil.COMMON_ITEM_TAGS;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int leftX = panelX + 16;

        tagField = new EditBox(this.font, leftX, panelY + 40, panelW - 90, 18, Component.empty());
        tagField.setMaxLength(80);
        tagField.setHint(Component.literal("forge:ingots/iron"));
        tagField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_.:/-]*"));
        if (prefill != null) { tagField.setValue(prefill); prefill = null; }
        this.addRenderableWidget(tagField);

        this.addRenderableWidget(Button.builder(
                Component.literal("Use"),
                b -> confirm()
        ).bounds(leftX + panelW - 88, panelY + 40, 60, 18).build());

        List<String> list = suggestions();
        int listX = leftX;
        int listW = panelW - 60;
        int listY = panelY + 72;

        int count = Math.min(VISIBLE, list.size() - scroll);
        for (int i = 0; i < count; i++) {
            String tag = list.get(scroll + i);
            int rowY = listY + i * (ROW_H + ROW_GAP);
            this.addRenderableWidget(Button.builder(
                    Component.literal(tag),
                    b -> { tagField.setValue(tag); message = "Add a suffix like /iron, then Use"; }
            ).bounds(listX, rowY, listW, ROW_H).build());
        }

        if (list.size() > VISIBLE) {
            int sX = listX + listW + 4;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (scroll > 0) { scroll--; this.clearWidgets(); this.init(); } })
                    .bounds(sX, listY, 18, ROW_H).build();
            up.active = scroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (scroll + VISIBLE < list.size()) { scroll++; this.clearWidgets(); this.init(); } })
                    .bounds(sX, listY + (VISIBLE - 1) * (ROW_H + ROW_GAP), 18, ROW_H).build();
            down.active = scroll + VISIBLE < list.size();
            this.addRenderableWidget(down);
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                b -> this.onClose()
        ).bounds(panelX + panelW / 2 - 50, panelY + panelH - 28, 100, 20).build());
    }

    private void confirm() {
        String tag = tagField.getValue().trim().toLowerCase();
        String err = TagUtil.validateTagId(tag);
        if (err != null) { message = err; return; }
        onPick.accept(tag);
        this.onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<String> list = suggestions();
        if (list.size() > VISIBLE) {
            int ns = scroll - (int) Math.signum(delta);
            ns = Math.max(0, Math.min(list.size() - VISIBLE, ns));
            if (ns != scroll) { scroll = ns; this.clearWidgets(); this.init(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int leftX = panelX + 16;
        gfx.drawString(this.font, "Tag:", leftX, panelY + 26, 0xFFFFFF, true);
        gfx.drawString(this.font,
                Component.literal("Click a category, then refine:").withStyle(ChatFormatting.GRAY),
                leftX, panelY + 62, 0xFFFFFF);

        if (message != null) {
            gfx.drawCenteredString(this.font, Component.literal(message).withStyle(ChatFormatting.GRAY),
                    panelX + panelW / 2, panelY + panelH - 44, 0xFFFFFF);
        }
    }
}
