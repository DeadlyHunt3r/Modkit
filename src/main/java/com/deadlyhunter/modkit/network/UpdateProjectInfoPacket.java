package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.core.ProjectInfo;
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
import java.util.Arrays;
import java.util.function.Supplier;

public class UpdateProjectInfoPacket {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String modName;
    private final String displayName;
    private final String description;
    private final String version;
    private final String license;

    public UpdateProjectInfoPacket(String modName, String displayName, String description,
                                    String version, String license) {
        this.modName = modName;
        this.displayName = displayName;
        this.description = description;
        this.version = version;
        this.license = license;
    }

    public static void encode(UpdateProjectInfoPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.displayName, 128);
        buf.writeUtf(pkt.description, 512);
        buf.writeUtf(pkt.version, 32);
        buf.writeUtf(pkt.license, 64);
    }

    public static UpdateProjectInfoPacket decode(FriendlyByteBuf buf) {
        return new UpdateProjectInfoPacket(
                buf.readUtf(64), buf.readUtf(128),
                buf.readUtf(512), buf.readUtf(32),
                buf.readUtf(64));
    }

    public static void handle(UpdateProjectInfoPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission."));
            ctx.setPacketHandled(true);
            return;
        }

        String error = trySave(pkt);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c[Modkit] " + error));
        } else {
            player.sendSystemMessage(Component.literal("§a[Modkit] Project info updated."));
        }
        ctx.setPacketHandled(true);
    }

    private static String trySave(UpdateProjectInfoPacket pkt) {
        ProjectInfo info = WorkspaceManager.loadProject(pkt.modName);
        if (info == null) return "Workspace not found.";

        if (pkt.license != null && !pkt.license.isBlank()) {
            boolean known = Arrays.asList(ProjectInfo.LICENSE_OPTIONS).contains(pkt.license);
            if (!known) return "Unknown license: " + pkt.license;
            info.license = pkt.license;
        }

        if (pkt.displayName != null && !pkt.displayName.isBlank()) info.displayName = pkt.displayName;
        if (pkt.description != null) info.description = pkt.description;
        if (pkt.version != null && !pkt.version.isBlank()) info.version = pkt.version;

        Path target = WorkspaceManager.getWorkspacePath(pkt.modName).resolve("project_info.json");
        try {
            Files.writeString(target, GSON.toJson(info));
            Modkit.LOGGER.info("[Modkit] Updated project info: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
