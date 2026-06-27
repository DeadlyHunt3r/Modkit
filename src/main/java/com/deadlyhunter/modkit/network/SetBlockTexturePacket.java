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
import java.util.Set;

public record SetBlockTexturePacket(String modName, String blockId, String face, byte[] pngData) implements CustomPacketPayload {

    private static final int MAX_PNG_BYTES = 1024 * 1024;
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final Set<String> VALID_FACES =
            Set.of("", "front", "top", "bottom", "north", "south", "east", "west", "up", "down");

    public SetBlockTexturePacket {
        if (face == null) face = "";
    }

    public SetBlockTexturePacket(String modName, String blockId, byte[] pngData) {
        this(modName, blockId, "", pngData);
    }

    public static final Type<SetBlockTexturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "set_block_texture"));

    public static final StreamCodec<ByteBuf, SetBlockTexturePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),        SetBlockTexturePacket::modName,
            ByteBufCodecs.stringUtf8(64),        SetBlockTexturePacket::blockId,
            ByteBufCodecs.stringUtf8(16),        SetBlockTexturePacket::face,
            ByteBufCodecs.byteArray(MAX_PNG_BYTES), SetBlockTexturePacket::pngData,
            SetBlockTexturePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetBlockTexturePacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission to upload textures.", player -> {
            String error = trySave(pkt);
            if (error != null) {
                player.sendSystemMessage(Component.literal("\u00a7c[Modkit] Texture upload failed: " + error));
            } else {
                String faceInfo = pkt.face().isEmpty() ? "" : " (" + pkt.face() + ")";
                player.sendSystemMessage(Component.literal(
                        "\u00a7a[Modkit] Texture set for block '" + pkt.blockId() + "'" + faceInfo + "."));
            }
        });
    }

    private static String trySave(SetBlockTexturePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace does not exist.";
        if (!pkt.blockId().matches("[a-z0-9_]{1,40}")) return "Invalid block id.";
        if (!VALID_FACES.contains(pkt.face())) return "Invalid face suffix.";

        if (pkt.pngData() == null || pkt.pngData().length < PNG_MAGIC.length) return "Not a valid PNG (too short).";
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (pkt.pngData()[i] != PNG_MAGIC[i]) return "Not a valid PNG (wrong magic bytes).";
        }

        String fileName = pkt.face().isEmpty() ? pkt.blockId() + ".png" : pkt.blockId() + "_" + pkt.face() + ".png";
        Path target = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("assets").resolve("textures").resolve("block").resolve(fileName);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, pkt.pngData());
            Modkit.LOGGER.info("[Modkit] Saved block texture: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
