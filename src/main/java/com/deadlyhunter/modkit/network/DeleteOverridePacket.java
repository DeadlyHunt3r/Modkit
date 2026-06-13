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

public class DeleteOverridePacket {

    private final String modName;
    private final String overrideId;

    public DeleteOverridePacket(String modName, String overrideId) {
        this.modName = modName;
        this.overrideId = overrideId;
    }

    public static void encode(DeleteOverridePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.overrideId, 64);
    }

    public static DeleteOverridePacket decode(FriendlyByteBuf buf) {
        return new DeleteOverridePacket(buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(DeleteOverridePacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
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
            player.sendSystemMessage(Component.literal("§a[Modkit] Deleted override '" + pkt.overrideId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String tryDelete(DeleteOverridePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) return "Workspace not found.";
        if (!pkt.overrideId.matches("[a-z0-9_]{1,48}")) return "Invalid override id.";

        Path jsonFile = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("modkit").resolve("overrides").resolve(pkt.overrideId + ".json");

        if (!Files.exists(jsonFile)) return "Override not found.";

        try {
            Files.delete(jsonFile);
            Modkit.LOGGER.info("[Modkit] Deleted override: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.overrideId, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
