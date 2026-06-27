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

public record SetTexturePacket(String modName, String itemId, byte[] pngData) implements CustomPacketPayload {

    private static final int MAX_PNG_BYTES = 1024 * 1024;
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    public static final Type<SetTexturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "set_texture"));

    public static final StreamCodec<ByteBuf, SetTexturePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),        SetTexturePacket::modName,
            ByteBufCodecs.stringUtf8(64),        SetTexturePacket::itemId,
            ByteBufCodecs.byteArray(MAX_PNG_BYTES), SetTexturePacket::pngData,
            SetTexturePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetTexturePacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission to upload textures.", player -> {
            String error = trySave(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Texture upload failed: " + error
                    : "\u00a7a[Modkit] Texture set for '" + pkt.itemId() + "'."));
        });
    }

    private static String trySave(SetTexturePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) {
            return "Workspace '" + pkt.modName() + "' does not exist.";
        }
        if (!pkt.itemId().matches("[a-z0-9_]{1,40}")) return "Invalid item id.";

        if (pkt.pngData() == null || pkt.pngData().length < PNG_MAGIC.length) {
            return "Not a valid PNG (too short).";
        }
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (pkt.pngData()[i] != PNG_MAGIC[i]) return "Not a valid PNG (wrong magic bytes).";
        }

        Path target = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("assets").resolve("textures").resolve("item").resolve(pkt.itemId() + ".png");
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, pkt.pngData());
            Modkit.LOGGER.info("[Modkit] Saved texture: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write texture " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
