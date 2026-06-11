package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class SetArmorTexturePacket {

    private static final int MAX_PNG_BYTES = 1024 * 1024;

    private final String modName;
    private final String fileName;
    private final String kind;
    private final byte[] pngBytes;

    public SetArmorTexturePacket(String modName, String fileName, String kind, byte[] pngBytes) {
        this.modName = modName;
        this.fileName = fileName;
        this.kind = kind;
        this.pngBytes = pngBytes;
    }

    public static void encode(SetArmorTexturePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.fileName, 80);
        buf.writeUtf(pkt.kind, 16);
        buf.writeByteArray(pkt.pngBytes);
    }

    public static SetArmorTexturePacket decode(FriendlyByteBuf buf) {
        return new SetArmorTexturePacket(
                buf.readUtf(64),
                buf.readUtf(80),
                buf.readUtf(16),
                buf.readByteArray(MAX_PNG_BYTES + 1024));
    }

    public static void handle(SetArmorTexturePacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission."));
            ctx.setPacketHandled(true);
            return;
        }

        String error = trySave(pkt);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c[Modkit] Texture failed: " + error));
        } else {
            player.sendSystemMessage(Component.literal("§a[Modkit] Texture saved: " + pkt.fileName + ".png"));
        }
        ctx.setPacketHandled(true);
    }

    private static String trySave(SetArmorTexturePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) return "Workspace not found.";
        if (!pkt.fileName.matches("[a-z0-9_]{1,72}")) return "Invalid texture file name.";
        if (!"icon".equals(pkt.kind) && !"layer".equals(pkt.kind)) return "Invalid texture kind.";

        if (pkt.pngBytes == null || pkt.pngBytes.length == 0) return "Empty file.";
        if (pkt.pngBytes.length > MAX_PNG_BYTES) return "PNG too large (max 1MB).";

        if (pkt.pngBytes.length < 8
                || (pkt.pngBytes[0] & 0xFF) != 0x89 || pkt.pngBytes[1] != 'P'
                || pkt.pngBytes[2] != 'N' || pkt.pngBytes[3] != 'G') {
            return "File is not a valid PNG.";
        }

        String subDir = "icon".equals(pkt.kind) ? "item" : "armor";
        Path target = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("assets").resolve("textures").resolve(subDir)
                .resolve(pkt.fileName + ".png");

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, pkt.pngBytes);
            Modkit.LOGGER.info("[Modkit] Saved armor texture: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
