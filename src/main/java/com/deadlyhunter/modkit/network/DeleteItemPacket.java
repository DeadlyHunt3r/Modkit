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

public class DeleteItemPacket {

    private final String modName;
    private final String itemId;

    public DeleteItemPacket(String modName, String itemId) {
        this.modName = modName;
        this.itemId = itemId;
    }

    public static void encode(DeleteItemPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.itemId, 64);
    }

    public static DeleteItemPacket decode(FriendlyByteBuf buf) {
        return new DeleteItemPacket(buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(DeleteItemPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission to delete items."));
            ctx.setPacketHandled(true);
            return;
        }

        String error = tryDelete(pkt);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c[Modkit] Delete failed: " + error));
        } else {
            player.sendSystemMessage(Component.literal(
                    "§a[Modkit] Deleted item '" + pkt.itemId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String tryDelete(DeleteItemPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) {
            return "Workspace '" + pkt.modName + "' does not exist.";
        }
        if (!pkt.itemId.matches("[a-z0-9_]{1,40}")) {
            return "Invalid item id.";
        }

        Path workspace = WorkspaceManager.getWorkspacePath(pkt.modName);
        Path jsonFile = workspace.resolve("modkit").resolve("items").resolve(pkt.itemId + ".json");
        Path pngFile = workspace.resolve("assets").resolve("textures").resolve("item").resolve(pkt.itemId + ".png");

        if (!Files.exists(jsonFile)) {
            return "Item '" + pkt.itemId + "' not found.";
        }

        try {
            Files.delete(jsonFile);
            Modkit.LOGGER.info("[Modkit] Deleted item JSON: {}", jsonFile);
            if (Files.exists(pngFile)) {
                Files.delete(pngFile);
                Modkit.LOGGER.info("[Modkit] Deleted item texture: {}", pngFile);
            }
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.itemId, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
