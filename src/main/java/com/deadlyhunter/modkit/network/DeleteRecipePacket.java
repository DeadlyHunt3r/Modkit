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

public class DeleteRecipePacket {

    private final String modName;
    private final String recipeId;

    public DeleteRecipePacket(String modName, String recipeId) {
        this.modName = modName;
        this.recipeId = recipeId;
    }

    public static void encode(DeleteRecipePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.recipeId, 64);
    }

    public static DeleteRecipePacket decode(FriendlyByteBuf buf) {
        return new DeleteRecipePacket(buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(DeleteRecipePacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
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
            player.sendSystemMessage(Component.literal("§a[Modkit] Deleted recipe '" + pkt.recipeId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String tryDelete(DeleteRecipePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) return "Workspace not found.";
        if (!pkt.recipeId.matches("[a-z0-9_]{1,40}")) return "Invalid recipe id.";

        Path jsonFile = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("modkit").resolve("recipes").resolve(pkt.recipeId + ".json");

        if (!Files.exists(jsonFile)) return "Recipe not found.";

        try {
            Files.delete(jsonFile);
            Modkit.LOGGER.info("[Modkit] Deleted recipe: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.recipeId, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
