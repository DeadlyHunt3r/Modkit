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

public record DeleteBlockPacket(String modName, String blockId) implements CustomPacketPayload {

    public static final Type<DeleteBlockPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "delete_block"));

    public static final StreamCodec<ByteBuf, DeleteBlockPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), DeleteBlockPacket::modName,
            ByteBufCodecs.stringUtf8(64), DeleteBlockPacket::blockId,
            DeleteBlockPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteBlockPacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission to delete blocks.", player -> {
            String error = tryDelete(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Delete failed: " + error
                    : "\u00a7a[Modkit] Deleted block '" + pkt.blockId() + "'."));
        });
    }

    private static String tryDelete(DeleteBlockPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";
        if (!pkt.blockId().matches("[a-z0-9_]{1,40}")) return "Invalid block id.";

        Path workspace = WorkspaceManager.getWorkspacePath(pkt.modName());
        Path jsonFile = workspace.resolve("modkit").resolve("blocks").resolve(pkt.blockId() + ".json");
        Path pngFile = workspace.resolve("assets").resolve("textures").resolve("block").resolve(pkt.blockId() + ".png");

        if (!Files.exists(jsonFile)) return "Block '" + pkt.blockId() + "' not found.";

        try {
            Files.delete(jsonFile);
            Modkit.LOGGER.info("[Modkit] Deleted block JSON: {}", jsonFile);
            if (Files.exists(pngFile)) {
                Files.delete(pngFile);
                Modkit.LOGGER.info("[Modkit] Deleted block texture: {}", pngFile);
            }

            Path texDir = workspace.resolve("assets").resolve("textures").resolve("block");
            for (String suffix : new String[]{"front", "top", "bottom", "north", "south", "east", "west", "up", "down"}) {
                Path faceTex = texDir.resolve(pkt.blockId() + "_" + suffix + ".png");
                try {
                    if (Files.exists(faceTex)) Files.delete(faceTex);
                } catch (IOException ignore) {
                }
            }
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.blockId(), e);
            return "I/O error: " + e.getMessage();
        }
    }
}
