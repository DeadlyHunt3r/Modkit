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

public record DeleteItemPacket(String modName, String itemId) implements CustomPacketPayload {

    public static final Type<DeleteItemPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "delete_item"));

    public static final StreamCodec<ByteBuf, DeleteItemPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), DeleteItemPacket::modName,
            ByteBufCodecs.stringUtf8(64), DeleteItemPacket::itemId,
            DeleteItemPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteItemPacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission to delete items.", player -> {
            String error = tryDelete(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Delete failed: " + error
                    : "\u00a7a[Modkit] Deleted item '" + pkt.itemId() + "'."));
        });
    }

    private static String tryDelete(DeleteItemPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) {
            return "Workspace '" + pkt.modName() + "' does not exist.";
        }
        if (!pkt.itemId().matches("[a-z0-9_]{1,40}")) return "Invalid item id.";

        Path workspace = WorkspaceManager.getWorkspacePath(pkt.modName());
        Path jsonFile = workspace.resolve("modkit").resolve("items").resolve(pkt.itemId() + ".json");
        Path pngFile = workspace.resolve("assets").resolve("textures").resolve("item").resolve(pkt.itemId() + ".png");

        if (!Files.exists(jsonFile)) return "Item '" + pkt.itemId() + "' not found.";

        try {
            Files.delete(jsonFile);
            Modkit.LOGGER.info("[Modkit] Deleted item JSON: {}", jsonFile);
            if (Files.exists(pngFile)) {
                Files.delete(pngFile);
                Modkit.LOGGER.info("[Modkit] Deleted item texture: {}", pngFile);
            }
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.itemId(), e);
            return "I/O error: " + e.getMessage();
        }
    }
}
