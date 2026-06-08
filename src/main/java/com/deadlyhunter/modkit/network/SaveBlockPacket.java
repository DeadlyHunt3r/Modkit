package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class SaveBlockPacket {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String modName;
    private final String blockId;
    private final String json;

    public SaveBlockPacket(String modName, String blockId, String json) {
        this.modName = modName;
        this.blockId = blockId;
        this.json = json;
    }

    public static void encode(SaveBlockPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.blockId, 64);
        buf.writeUtf(pkt.json, 32768);
    }

    public static SaveBlockPacket decode(FriendlyByteBuf buf) {
        return new SaveBlockPacket(buf.readUtf(64), buf.readUtf(64), buf.readUtf(32768));
    }

    public static void handle(SaveBlockPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission to edit projects."));
            ctx.setPacketHandled(true);
            return;
        }

        String error = trySave(pkt);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c[Modkit] Save failed: " + error));
        } else {
            player.sendSystemMessage(Component.literal("§a[Modkit] Saved block '" + pkt.blockId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String trySave(SaveBlockPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) {
            return "Workspace '" + pkt.modName + "' does not exist.";
        }

        BlockDefinition def;
        try {
            def = GSON.fromJson(pkt.json, BlockDefinition.class);
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
        if (def == null) return "Block definition is empty.";
        if (!pkt.blockId.equals(def.id)) {
            return "Block id mismatch: packet=" + pkt.blockId + " json=" + def.id;
        }
        String validationError = def.validate();
        if (validationError != null) return validationError;

        String prettyJson = GSON.toJson(def);
        Path target = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("modkit").resolve("blocks").resolve(def.id + ".json");

        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, prettyJson);
            Modkit.LOGGER.info("[Modkit] Saved block file: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
