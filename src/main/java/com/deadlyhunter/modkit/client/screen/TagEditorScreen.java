package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.content.tag.TagUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TagEditorScreen extends ModkitBaseScreen {

    private static final int ROW_H = 18;
    private static final int ROW_GAP = 3;
    private static final int MAX_TAGS = 20;

    private final net.minecraft.client.gui.screens.Screen returnTo;
    private final List<String> tags;
    private final boolean isBlock;

    private EditBox tagField;
    private int suggestionScroll = 0;
    private String message = null;

    public TagEditorScreen(net.minecraft.client.gui.screens.Screen returnTo,
                            List<String> tags, boolean isBlock, String subjectName) {
        super(Component.literal("Tags — " + subjectName), returnTo);
        this.returnTo = returnTo;
        this.tags = tags;
        this.isBlock = isBlock;
        this.panelW = 360;
        this.panelH = 290;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int leftX = panelX + 16;
        int fieldW = 200;

        tagField = new EditBox(this.font, leftX, panelY + 40, fieldW, 18, Component.empty());
        tagField.setMaxLength(80);
        tagField.setHint(Component.literal("namespace:path"));
        tagField.setFilter(s -> s.isEmpty() || s.matches("[a-z0-9_.:/-]*"));
        this.addRenderableWidget(tagField);

        this.addRenderableWidget(Button.builder(
                Component.literal("+ Add"),
                b -> addTypedTag()
        ).bounds(leftX + fieldW + 6, panelY + 40, 60, 18).build());

        int listY = panelY + 70;
        for (int i = 0; i < tags.size() && i < 8; i++) {
            final int index = i;
            String tag = tags.get(i);
            int rowY = listY + i * (ROW_H + ROW_GAP);
            this.addRenderableWidget(Button.builder(
                    Component.literal("✕").withStyle(ChatFormatting.RED),
                    b -> { tags.remove(index); this.clearWidgets(); this.init(); })
                    .bounds(leftX, rowY, 18, ROW_H).build());
        }

        List<String> suggestions = isBlock ? TagUtil.COMMON_BLOCK_TAGS : TagUtil.COMMON_ITEM_TAGS;
        int sugX = panelX + panelW - 150;
        int sugW = 134;
        int sugY = panelY + 70;
        int visible = 8;
        int count = Math.min(visible, suggestions.size() - suggestionScroll);
        for (int i = 0; i < count; i++) {
            String sug = suggestions.get(suggestionScroll + i);
            boolean already = tags.contains(sug);
            int rowY = sugY + i * (ROW_H + ROW_GAP);
            Button b = Button.builder(
                    Component.literal(already ? "✓ " + shortTag(sug) : shortTag(sug)),
                    btn -> {
                        tagField.setValue(sug);
                        tagField.setFocused(true);
                        this.setFocused(tagField);
                        message = "Refine (e.g. add /iron) then + Add";
                    }
            ).bounds(sugX, rowY, sugW, ROW_H).build();
            b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal(sug + "\nClick to put in the field, then refine")));
            this.addRenderableWidget(b);
        }

        if (suggestions.size() > visible) {
            int sX = sugX + sugW + 2;
            Button up = Button.builder(Component.literal("▲"),
                    b -> { if (suggestionScroll > 0) { suggestionScroll--; this.clearWidgets(); this.init(); } })
                    .bounds(sX, sugY, 14, ROW_H).build();
            up.active = suggestionScroll > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(Component.literal("▼"),
                    b -> { if (suggestionScroll + visible < suggestions.size()) { suggestionScroll++; this.clearWidgets(); this.init(); } })
                    .bounds(sX, sugY + (visible - 1) * (ROW_H + ROW_GAP), 14, ROW_H).build();
            down.active = suggestionScroll + visible < suggestions.size();
            this.addRenderableWidget(down);
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                b -> this.onClose()
        ).bounds(panelX + panelW / 2 - 50, panelY + panelH - 28, 100, 20).build());
    }

    private void addTypedTag() {
        String tag = tagField.getValue().trim().toLowerCase();
        if (tag.isEmpty()) { message = "Type a tag id first."; return; }
        String err = TagUtil.validateTagId(tag);
        if (err != null) { message = err; return; }
        if (tags.contains(tag)) { message = "Already added."; return; }
        if (tags.size() >= MAX_TAGS) { message = "Max " + MAX_TAGS + " tags."; return; }
        tags.add(tag);
        tagField.setValue("");
        message = null;
        this.clearWidgets();
        this.init();
    }

    private static String shortTag(String tag) {
        int colon = tag.indexOf(':');
        String path = colon >= 0 ? tag.substring(colon + 1) : tag;
        if (path.length() > 18) path = path.substring(0, 17) + "…";
        return path;
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int leftX = panelX + 16;

        gfx.drawString(this.font, "Add a tag:", leftX, panelY + 26, 0xFFFFFF, true);

        gfx.drawString(this.font,
                Component.literal("Your tags (" + tags.size() + "):").withStyle(ChatFormatting.GRAY),
                leftX, panelY + 60, 0xFFFFFF);

        int listY = panelY + 70;
        for (int i = 0; i < tags.size() && i < 8; i++) {
            int rowY = listY + i * (ROW_H + ROW_GAP);
            gfx.drawString(this.font, shortTagFull(tags.get(i)), leftX + 24, rowY + 5, 0xFFFFFF, false);
        }
        if (tags.size() > 8) {
            gfx.drawString(this.font, "+" + (tags.size() - 8) + " more…",
                    leftX + 24, listY + 8 * (ROW_H + ROW_GAP) + 2, 0xAAAAAA, false);
        }

        int sugX = panelX + panelW - 150;
        gfx.drawString(this.font,
                Component.literal("Common tags:").withStyle(ChatFormatting.GRAY),
                sugX, panelY + 60, 0xFFFFFF);

        if (message != null) {
            gfx.drawCenteredString(this.font, Component.literal(message),
                    panelX + panelW / 2, panelY + panelH - 44, 0xFF5555);
        }
    }

    private static String shortTagFull(String tag) {
        if (tag.length() > 26) return tag.substring(0, 25) + "…";
        return tag;
    }
}
