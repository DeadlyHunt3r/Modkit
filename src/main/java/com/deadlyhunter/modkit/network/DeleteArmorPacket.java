package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.armor.ArmorSetDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class DeleteArmorPacket {

    private final String modName;
    private final String setId;

    public DeleteArmorPacket(String modName, String setId) {
        this.modName = modName;
        this.setId = setId;
    }

    public static void encode(DeleteArmorPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.setId, 64);
    }

    public static DeleteArmorPacket decode(FriendlyByteBuf buf) {
        return new DeleteArmorPacket(buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(DeleteArmorPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
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
            player.sendSystemMessage(Component.literal("§a[Modkit] Deleted armor set '" + pkt.setId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String tryDelete(DeleteArmorPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) return "Workspace not found.";
        if (!pkt.setId.matches("[a-z0-9_]{1,32}")) return "Invalid armor set id.";

        Path workspace = WorkspaceManager.getWorkspacePath(pkt.modName);
        Path jsonFile = workspace.resolve("modkit").resolve("armor").resolve(pkt.setId + ".json");

        if (!Files.exists(jsonFile)) return "Armor set not found.";

        try {
            Files.delete(jsonFile);

            Path itemTexDir = workspace.resolve("assets").resolve("textures").resolve("item");
            for (String pieceType : ArmorSetDefinition.PIECE_TYPES) {
                deleteQuietly(itemTexDir.resolve(pkt.setId + "_" + pieceType + ".png"));
            }
            Path armorTexDir = workspace.resolve("assets").resolve("textures").resolve("armor");
            deleteQuietly(armorTexDir.resolve(pkt.setId + "_layer_1.png"));
            deleteQuietly(armorTexDir.resolve(pkt.setId + "_layer_2.png"));

            Modkit.LOGGER.info("[Modkit] Deleted armor set: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.setId, e);
            return "I/O error: " + e.getMessage();
        }
    }

    private static void deleteQuietly(Path p) {
        try {
            if (Files.exists(p)) Files.delete(p);
        } catch (IOException ignore) { /* not fatal */ }
    }
}
