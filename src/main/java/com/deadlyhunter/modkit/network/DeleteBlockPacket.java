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


public class DeleteBlockPacket {

    private final String modName;
    private final String blockId;

    public DeleteBlockPacket(String modName, String blockId) {
        this.modName = modName;
        this.blockId = blockId;
    }

    public static void encode(DeleteBlockPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.blockId, 64);
    }

    public static DeleteBlockPacket decode(FriendlyByteBuf buf) {
        return new DeleteBlockPacket(buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(DeleteBlockPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission to delete blocks."));
            ctx.setPacketHandled(true);
            return;
        }

        String error = tryDelete(pkt);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c[Modkit] Delete failed: " + error));
        } else {
            player.sendSystemMessage(Component.literal("§a[Modkit] Deleted block '" + pkt.blockId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String tryDelete(DeleteBlockPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) return "Workspace not found.";
        if (!pkt.blockId.matches("[a-z0-9_]{1,40}")) return "Invalid block id.";

        Path workspace = WorkspaceManager.getWorkspacePath(pkt.modName);
        Path jsonFile = workspace.resolve("modkit").resolve("blocks").resolve(pkt.blockId + ".json");
        Path pngFile = workspace.resolve("assets").resolve("textures").resolve("block").resolve(pkt.blockId + ".png");

        if (!Files.exists(jsonFile)) return "Block '" + pkt.blockId + "' not found.";

        try {
            Files.delete(jsonFile);
            Modkit.LOGGER.info("[Modkit] Deleted block JSON: {}", jsonFile);
            if (Files.exists(pngFile)) {
                Files.delete(pngFile);
                Modkit.LOGGER.info("[Modkit] Deleted block texture: {}", pngFile);
            }

            Path texDir = workspace.resolve("assets").resolve("textures").resolve("block");
            for (String suffix : new String[]{"front", "top", "bottom", "north", "south", "east", "west", "up", "down"}) {
                Path faceTex = texDir.resolve(pkt.blockId + "_" + suffix + ".png");
                try {
                    if (Files.exists(faceTex)) Files.delete(faceTex);
                } catch (IOException ignore) { /* not fatal */ }
            }
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.blockId, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
