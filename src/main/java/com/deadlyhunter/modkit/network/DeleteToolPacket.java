package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.core.WorkspaceManager;
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

public record DeleteToolPacket(String modName, String toolId) implements CustomPacketPayload {

    public static final Type<DeleteToolPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "delete_tool"));

    public static final StreamCodec<ByteBuf, DeleteToolPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), DeleteToolPacket::modName,
            ByteBufCodecs.stringUtf8(64), DeleteToolPacket::toolId,
            DeleteToolPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteToolPacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = tryDelete(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Delete failed: " + error
                    : "\u00a7a[Modkit] Deleted tool '" + pkt.toolId() + "'."));
        });
    }

    private static String tryDelete(DeleteToolPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";
        if (!pkt.toolId().matches("[a-z0-9_]{1,40}")) return "Invalid tool id.";

        Path jsonFile = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("modkit").resolve("tools").resolve(pkt.toolId() + ".json");
        Path textureFile = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("assets").resolve("textures").resolve("item").resolve(pkt.toolId() + ".png");

        if (!Files.exists(jsonFile)) return "Tool not found.";

        try {
            Files.delete(jsonFile);
            if (Files.exists(textureFile)) {
                try { Files.delete(textureFile); }
                catch (IOException ignore) {
                }
            }
            Modkit.LOGGER.info("[Modkit] Deleted tool: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.toolId(), e);
            return "I/O error: " + e.getMessage();
        }
    }
}
