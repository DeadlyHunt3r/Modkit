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

public record DeleteOrePacket(String modName, String oreId) implements CustomPacketPayload {

    public static final Type<DeleteOrePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "delete_ore"));

    public static final StreamCodec<ByteBuf, DeleteOrePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), DeleteOrePacket::modName,
            ByteBufCodecs.stringUtf8(64), DeleteOrePacket::oreId,
            DeleteOrePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteOrePacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = tryDelete(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Delete failed: " + error
                    : "\u00a7a[Modkit] Deleted ore '" + pkt.oreId() + "'."));
        });
    }

    private static String tryDelete(DeleteOrePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";
        if (!pkt.oreId().matches("[a-z0-9_]{1,40}")) return "Invalid ore id.";

        Path jsonFile = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("modkit").resolve("ores").resolve(pkt.oreId() + ".json");
        if (!Files.exists(jsonFile)) return "Ore not found.";

        try {
            Files.delete(jsonFile);
            Modkit.LOGGER.info("[Modkit] Deleted ore: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.oreId(), e);
            return "I/O error: " + e.getMessage();
        }
    }
}
