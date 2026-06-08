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
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DeleteWorkspacePacket {

    private final String modName;

    public DeleteWorkspacePacket(String modName) {
        this.modName = modName;
    }

    public static void encode(DeleteWorkspacePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
    }

    public static DeleteWorkspacePacket decode(FriendlyByteBuf buf) {
        return new DeleteWorkspacePacket(buf.readUtf(64));
    }

    public static void handle(DeleteWorkspacePacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission to delete workspaces."));
            ctx.setPacketHandled(true);
            return;
        }

        String error = tryDelete(pkt.modName);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c[Modkit] Delete failed: " + error));
        } else {
            player.sendSystemMessage(Component.literal(
                    "§a[Modkit] Deleted workspace '" + pkt.modName + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String tryDelete(String modName) {
        if (!modName.matches("[a-z0-9_]{1,30}")) return "Invalid workspace name.";
        if (!WorkspaceManager.exists(modName)) return "Workspace not found.";

        Path workspace = WorkspaceManager.getWorkspacePath(modName);
        try (Stream<Path> walk = Files.walk(workspace)) {

            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
            Modkit.LOGGER.info("[Modkit] Deleted workspace: {}", workspace);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to delete " + workspace, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
