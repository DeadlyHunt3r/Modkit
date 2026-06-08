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

public class DeleteOrePacket {

    private final String modName;
    private final String oreId;

    public DeleteOrePacket(String modName, String oreId) {
        this.modName = modName;
        this.oreId = oreId;
    }

    public static void encode(DeleteOrePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.oreId, 64);
    }

    public static DeleteOrePacket decode(FriendlyByteBuf buf) {
        return new DeleteOrePacket(buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(DeleteOrePacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
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
            player.sendSystemMessage(Component.literal("§a[Modkit] Deleted ore '" + pkt.oreId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String tryDelete(DeleteOrePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) return "Workspace not found.";
        if (!pkt.oreId.matches("[a-z0-9_]{1,40}")) return "Invalid ore id.";

        Path jsonFile = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("modkit").resolve("ores").resolve(pkt.oreId + ".json");

        if (!Files.exists(jsonFile)) return "Ore not found.";

        try {
            Files.delete(jsonFile);
            Modkit.LOGGER.info("[Modkit] Deleted ore: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.oreId, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
