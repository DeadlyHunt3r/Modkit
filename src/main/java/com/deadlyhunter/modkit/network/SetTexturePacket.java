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

public class SetTexturePacket {

    private static final int MAX_PNG_BYTES = 1024 * 1024;

    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final String modName;
    private final String itemId;
    private final byte[] pngData;

    public SetTexturePacket(String modName, String itemId, byte[] pngData) {
        this.modName = modName;
        this.itemId = itemId;
        this.pngData = pngData;
    }

    public static void encode(SetTexturePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.itemId, 64);
        buf.writeByteArray(pkt.pngData);
    }

    public static SetTexturePacket decode(FriendlyByteBuf buf) {
        String modName = buf.readUtf(64);
        String itemId = buf.readUtf(64);
        byte[] data = buf.readByteArray(MAX_PNG_BYTES);
        return new SetTexturePacket(modName, itemId, data);
    }

    public static void handle(SetTexturePacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission to upload textures."));
            ctx.setPacketHandled(true);
            return;
        }

        String error = trySave(pkt);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c[Modkit] Texture upload failed: " + error));
        } else {
            player.sendSystemMessage(Component.literal(
                    "§a[Modkit] Texture set for '" + pkt.itemId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String trySave(SetTexturePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) {
            return "Workspace '" + pkt.modName + "' does not exist.";
        }

        if (!pkt.itemId.matches("[a-z0-9_]{1,40}")) {
            return "Invalid item id.";
        }

        if (pkt.pngData == null || pkt.pngData.length < PNG_MAGIC.length) {
            return "Not a valid PNG (too short).";
        }
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (pkt.pngData[i] != PNG_MAGIC[i]) {
                return "Not a valid PNG (wrong magic bytes).";
            }
        }

        Path target = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("assets").resolve("textures").resolve("item")
                .resolve(pkt.itemId + ".png");

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, pkt.pngData);
            Modkit.LOGGER.info("[Modkit] Saved texture: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write texture " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
