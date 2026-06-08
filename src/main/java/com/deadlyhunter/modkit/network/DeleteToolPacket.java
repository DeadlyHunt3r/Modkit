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

public class DeleteToolPacket {

    private final String modName;
    private final String toolId;

    public DeleteToolPacket(String modName, String toolId) {
        this.modName = modName;
        this.toolId = toolId;
    }

    public static void encode(DeleteToolPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.toolId, 64);
    }

    public static DeleteToolPacket decode(FriendlyByteBuf buf) {
        return new DeleteToolPacket(buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(DeleteToolPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission."));
            ctx.setPacketHandled(true);
            return;
        }

        String error = tryDelete(pkt);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c[Modkit] Delete failed: " + error));
        } else {
            player.sendSystemMessage(Component.literal("§a[Modkit] Deleted tool '" + pkt.toolId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String tryDelete(DeleteToolPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) return "Workspace not found.";
        if (!pkt.toolId.matches("[a-z0-9_]{1,40}")) return "Invalid tool id.";

        Path jsonFile = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("modkit").resolve("tools").resolve(pkt.toolId + ".json");
        Path textureFile = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("assets").resolve("textures").resolve("item").resolve(pkt.toolId + ".png");

        if (!Files.exists(jsonFile)) return "Tool not found.";

        try {
            Files.delete(jsonFile);
            if (Files.exists(textureFile)) {
                try { Files.delete(textureFile); }
                catch (IOException ignore) { /* not fatal */ }
            }
            Modkit.LOGGER.info("[Modkit] Deleted tool: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.toolId, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
