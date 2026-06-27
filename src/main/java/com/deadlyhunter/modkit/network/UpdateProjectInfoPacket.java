package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.core.ProjectInfo;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public record UpdateProjectInfoPacket(String modName, String displayName, String description,
                                      String version, String license) implements CustomPacketPayload {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Type<UpdateProjectInfoPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "update_project_info"));

    public static final StreamCodec<ByteBuf, UpdateProjectInfoPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),  UpdateProjectInfoPacket::modName,
            ByteBufCodecs.stringUtf8(128), UpdateProjectInfoPacket::displayName,
            ByteBufCodecs.stringUtf8(512), UpdateProjectInfoPacket::description,
            ByteBufCodecs.stringUtf8(32),  UpdateProjectInfoPacket::version,
            ByteBufCodecs.stringUtf8(64),  UpdateProjectInfoPacket::license,
            UpdateProjectInfoPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateProjectInfoPacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = trySave(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] " + error
                    : "\u00a7a[Modkit] Project info updated."));
        });
    }

    private static String trySave(UpdateProjectInfoPacket pkt) {
        ProjectInfo info = WorkspaceManager.loadProject(pkt.modName());
        if (info == null) return "Workspace not found.";

        if (pkt.license() != null && !pkt.license().isBlank()) {
            boolean known = Arrays.asList(ProjectInfo.LICENSE_OPTIONS).contains(pkt.license());
            if (!known) return "Unknown license: " + pkt.license();
            info.license = pkt.license();
        }

        if (pkt.displayName() != null && !pkt.displayName().isBlank()) info.displayName = pkt.displayName();
        if (pkt.description() != null) info.description = pkt.description();
        if (pkt.version() != null && !pkt.version().isBlank()) info.version = pkt.version();

        Path target = WorkspaceManager.getWorkspacePath(pkt.modName()).resolve("project_info.json");
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
