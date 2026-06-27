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

public record DeleteWeaponPacket(String modName, String weaponId) implements CustomPacketPayload {

    public static final Type<DeleteWeaponPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "delete_weapon"));

    public static final StreamCodec<ByteBuf, DeleteWeaponPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), DeleteWeaponPacket::modName,
            ByteBufCodecs.stringUtf8(64), DeleteWeaponPacket::weaponId,
            DeleteWeaponPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteWeaponPacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = tryDelete(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Delete failed: " + error
                    : "\u00a7a[Modkit] Deleted weapon '" + pkt.weaponId() + "'."));
        });
    }

    private static String tryDelete(DeleteWeaponPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";
        if (!pkt.weaponId().matches("[a-z0-9_]{1,40}")) return "Invalid weapon id.";

        Path jsonFile = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("modkit").resolve("weapons").resolve(pkt.weaponId() + ".json");
        Path textureFile = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("assets").resolve("textures").resolve("item").resolve(pkt.weaponId() + ".png");

        if (!Files.exists(jsonFile)) return "Weapon not found.";

        try {
            Files.delete(jsonFile);
            if (Files.exists(textureFile)) {
                try { Files.delete(textureFile); }
                catch (IOException ignore) {
                }
            }
            Modkit.LOGGER.info("[Modkit] Deleted weapon: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.weaponId(), e);
            return "I/O error: " + e.getMessage();
        }
    }
}
