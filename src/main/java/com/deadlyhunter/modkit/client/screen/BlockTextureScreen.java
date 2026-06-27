package com.deadlyhunter.modkit.client.screen;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import net.neoforged.neoforge.network.PacketDistributor;
import com.deadlyhunter.modkit.network.SetBlockTexturePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlockTextureScreen extends ModkitBaseScreen {

    private static final long MAX_PNG_FILE_SIZE = 1024 * 1024;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 6;

    private final BlockEditorScreen editorParent;
    private final String modName;
    private final BlockDefinition def;

    private String message = null;
    private boolean messageIsError = false;

    public BlockTextureScreen(BlockEditorScreen parent, String modName, BlockDefinition def) {
        super(Component.literal("Textures — " + def.id), parent);
        this.editorParent = parent;
        this.modName = modName;
        this.def = def;
        this.panelW = 300;
        int slotCount = countSlotsForMode(def.textureMode);
        this.panelH = 90 + slotCount * (ROW_H + ROW_GAP) + 36;
    }

    private static int countSlotsForMode(String mode) {
        return switch (mode == null ? "all" : mode) {
            case "front_other"      -> 2;
            case "front_top_bottom" -> 4;
            case "all_unique"       -> 6;
            default                 -> 1;
        };
    }

    private record Slot(String label, String face) {}

    private Slot[] slotsForMode() {
        return switch (def.textureMode == null ? "all" : def.textureMode) {
            case "front_other" -> new Slot[]{
                    new Slot("Front", "front"),
                    new Slot("Rest (other 5)", "")
            };
            case "front_top_bottom" -> new Slot[]{
                    new Slot("Front", "front"),
                    new Slot("Top", "top"),
                    new Slot("Bottom", "bottom"),
                    new Slot("Sides", "")
            };
            case "all_unique" -> new Slot[]{
                    new Slot("North", "north"),
                    new Slot("South", "south"),
                    new Slot("East", "east"),
                    new Slot("West", "west"),
                    new Slot("Up", "up"),
                    new Slot("Down", "down")
            };
            default -> new Slot[]{ new Slot("All Sides", "") };
        };
    }

    @Override
    protected void init() {
        super.init();

        Slot[] slots = slotsForMode();
        int btnX = panelX + 150;
        int btnW = 130;
        int y = panelY + 50;

        for (Slot slot : slots) {
            final Slot s = slot;
            boolean exists = textureExists(s.face());
            this.addRenderableWidget(Button.builder(
                    Component.literal(exists ? "Change..." : "Choose..."),
                    b -> pickTexture(s.face())
            ).bounds(btnX, y, btnW, ROW_H).build());
            y += ROW_H + ROW_GAP;
        }

        int footerY = panelY + panelH - 30;
        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                b -> this.onClose()
        ).bounds(panelX + panelW / 2 - 50, footerY, 100, 20).build());
    }

    private boolean textureExists(String face) {
        String fileName = face.isEmpty() ? def.id + ".png" : def.id + "_" + face + ".png";
        Path tex = WorkspaceManager.getWorkspacePath(modName)
                .resolve("assets").resolve("textures").resolve("block").resolve(fileName);
        return Files.isRegularFile(tex);
    }

    private void pickTexture(String face) {
        Thread picker = new Thread(() -> {
            String chosenPath;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.png"));
                filters.flip();
                String label = face.isEmpty() ? "block" : face + " face";
                chosenPath = TinyFileDialogs.tinyfd_openFileDialog(
                        "Choose a 16x16 PNG for " + def.id + " (" + label + ")",
                        null, filters, "PNG image", false);
            } catch (Throwable t) {
                Modkit.LOGGER.error("[Modkit] File dialog failed", t);
                final String msg = "Could not open file dialog: " + t.getMessage();
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    message = msg; messageIsError = true;
                });
                return;
            }
            if (chosenPath == null || chosenPath.isBlank()) return;
            final String pathString = chosenPath;
            net.minecraft.client.Minecraft.getInstance().execute(() -> handlePicked(pathString, face));
        }, "Modkit-BlockFacePicker");
        picker.setDaemon(true);
        picker.start();
    }

    private void handlePicked(String chosenPath, String face) {
        Path source = Path.of(chosenPath);
        if (!Files.isRegularFile(source)) { setError("File not found."); return; }
        if (!chosenPath.toLowerCase().endsWith(".png")) { setError("Not a .png file."); return; }

        byte[] bytes;
        try {
            long size = Files.size(source);
            if (size > MAX_PNG_FILE_SIZE) {
                setError("PNG too large (max 1MB). Got " + (size / 1024) + " KB.");
                return;
            }
            bytes = Files.readAllBytes(source);
        } catch (IOException e) {
            setError("Read error: " + e.getMessage());
            return;
        }

        PacketDistributor.sendToServer(new SetBlockTexturePacket(modName, def.id, face, bytes));
        message = "Uploaded: " + (face.isEmpty() ? "base" : face);
        messageIsError = false;
        this.clearWidgets();
        this.init();
    }

    private void setError(String msg) {
        message = msg;
        messageIsError = true;
    }

    @Override
    protected void renderPanelContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.drawCenteredString(this.font,
                Component.literal(modeDescription()).withStyle(ChatFormatting.GRAY),
                panelX + panelW / 2, panelY + 26, 0xFFFFFF);

        Slot[] slots = slotsForMode();
        int labelX = panelX + 20;
        int y = panelY + 50 + 6;
        for (Slot slot : slots) {
            boolean exists = textureExists(slot.face());
            String status = exists ? " §a✓" : " §7—";
            gfx.drawString(this.font, slot.label() + status, labelX, y, 0xFFFFFF, true);
            y += ROW_H + ROW_GAP;
        }

        if (message != null) {
            gfx.drawCenteredString(this.font,
                    Component.literal(message),
                    panelX + panelW / 2, panelY + panelH - 46,
                    messageIsError ? 0xFF5555 : 0x55FF55);
        }
    }

    private String modeDescription() {
        return switch (def.textureMode == null ? "all" : def.textureMode) {
            case "front_other"      -> "Front faces player, rest shared";
            case "front_top_bottom" -> "Front faces player + top/bottom/sides";
            case "all_unique"       -> "Each direction its own texture";
            default                 -> "One texture on all sides";
        };
    }
}
