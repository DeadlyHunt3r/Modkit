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

public record SetArmorTexturePacket(String modName, String fileName, String kind, byte[] pngBytes) implements CustomPacketPayload {

    private static final int MAX_PNG_BYTES = 1024 * 1024;

    public static final Type<SetArmorTexturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "set_armor_texture"));

    public static final StreamCodec<ByteBuf, SetArmorTexturePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),               SetArmorTexturePacket::modName,
            ByteBufCodecs.stringUtf8(80),               SetArmorTexturePacket::fileName,
            ByteBufCodecs.stringUtf8(16),               SetArmorTexturePacket::kind,
            ByteBufCodecs.byteArray(MAX_PNG_BYTES + 1024), SetArmorTexturePacket::pngBytes,
            SetArmorTexturePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetArmorTexturePacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = trySave(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Texture failed: " + error
                    : "\u00a7a[Modkit] Texture saved: " + pkt.fileName() + ".png"));
        });
    }

    private static String trySave(SetArmorTexturePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";
        if (!pkt.fileName().matches("[a-z0-9_]{1,72}")) return "Invalid texture file name.";
        if (!"icon".equals(pkt.kind()) && !"layer".equals(pkt.kind())) return "Invalid texture kind.";

        byte[] png = pkt.pngBytes();
        if (png == null || png.length == 0) return "Empty file.";
        if (png.length > MAX_PNG_BYTES) return "PNG too large (max 1MB).";
        if (png.length < 8 || (png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
            return "File is not a valid PNG.";
        }

        String subDir = "icon".equals(pkt.kind()) ? "item" : "armor";
        Path target = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("assets").resolve("textures").resolve(subDir).resolve(pkt.fileName() + ".png");
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, png);
            Modkit.LOGGER.info("[Modkit] Saved armor texture: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
