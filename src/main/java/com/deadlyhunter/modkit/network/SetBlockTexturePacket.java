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

public class SetBlockTexturePacket {

    private static final int MAX_PNG_BYTES = 1024 * 1024;
    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final String modName;
    private final String blockId;
    private final byte[] pngData;

    public SetBlockTexturePacket(String modName, String blockId, byte[] pngData) {
        this.modName = modName;
        this.blockId = blockId;
        this.pngData = pngData;
    }

    public static void encode(SetBlockTexturePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.blockId, 64);
        buf.writeByteArray(pkt.pngData);
    }

    public static SetBlockTexturePacket decode(FriendlyByteBuf buf) {
        return new SetBlockTexturePacket(buf.readUtf(64), buf.readUtf(64), buf.readByteArray(MAX_PNG_BYTES));
    }

    public static void handle(SetBlockTexturePacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
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
            player.sendSystemMessage(Component.literal("§a[Modkit] Texture set for block '" + pkt.blockId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String trySave(SetBlockTexturePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) return "Workspace does not exist.";
        if (!pkt.blockId.matches("[a-z0-9_]{1,40}")) return "Invalid block id.";

        if (pkt.pngData == null || pkt.pngData.length < PNG_MAGIC.length) return "Not a valid PNG (too short).";
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (pkt.pngData[i] != PNG_MAGIC[i]) return "Not a valid PNG (wrong magic bytes).";
        }

        Path target = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("assets").resolve("textures").resolve("block")
                .resolve(pkt.blockId + ".png");

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, pkt.pngData);
            Modkit.LOGGER.info("[Modkit] Saved block texture: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
