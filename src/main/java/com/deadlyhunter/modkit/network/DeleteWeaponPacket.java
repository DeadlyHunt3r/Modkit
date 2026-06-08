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

public class DeleteWeaponPacket {

    private final String modName;
    private final String weaponId;

    public DeleteWeaponPacket(String modName, String weaponId) {
        this.modName = modName;
        this.weaponId = weaponId;
    }

    public static void encode(DeleteWeaponPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.weaponId, 64);
    }

    public static DeleteWeaponPacket decode(FriendlyByteBuf buf) {
        return new DeleteWeaponPacket(buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(DeleteWeaponPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
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
            player.sendSystemMessage(Component.literal("§a[Modkit] Deleted weapon '" + pkt.weaponId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String tryDelete(DeleteWeaponPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) return "Workspace not found.";
        if (!pkt.weaponId.matches("[a-z0-9_]{1,40}")) return "Invalid weapon id.";

        Path jsonFile = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("modkit").resolve("weapons").resolve(pkt.weaponId + ".json");
        Path textureFile = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("assets").resolve("textures").resolve("item").resolve(pkt.weaponId + ".png");

        if (!Files.exists(jsonFile)) return "Weapon not found.";

        try {
            Files.delete(jsonFile);
            if (Files.exists(textureFile)) {
                try { Files.delete(textureFile); }
                catch (IOException ignore) { /* not fatal */ }
            }
            Modkit.LOGGER.info("[Modkit] Deleted weapon: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.weaponId, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
