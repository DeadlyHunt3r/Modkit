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

public record DeleteOverridePacket(String modName, String overrideId) implements CustomPacketPayload {

    public static final Type<DeleteOverridePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "delete_override"));

    public static final StreamCodec<ByteBuf, DeleteOverridePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), DeleteOverridePacket::modName,
            ByteBufCodecs.stringUtf8(64), DeleteOverridePacket::overrideId,
            DeleteOverridePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteOverridePacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = tryDelete(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Delete failed: " + error
                    : "\u00a7a[Modkit] Deleted override '" + pkt.overrideId() + "'."));
        });
    }

    private static String tryDelete(DeleteOverridePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";
        if (!pkt.overrideId().matches("[a-z0-9_]{1,48}")) return "Invalid override id.";

        Path jsonFile = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("modkit").resolve("overrides").resolve(pkt.overrideId() + ".json");
        if (!Files.exists(jsonFile)) return "Override not found.";

        try {
            Files.delete(jsonFile);
            Modkit.LOGGER.info("[Modkit] Deleted override: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.overrideId(), e);
            return "I/O error: " + e.getMessage();
        }
    }
}
