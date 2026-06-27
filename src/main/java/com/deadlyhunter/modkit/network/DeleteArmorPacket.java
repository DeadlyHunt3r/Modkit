package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.armor.ArmorSetDefinition;
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

public record DeleteArmorPacket(String modName, String setId) implements CustomPacketPayload {

    public static final Type<DeleteArmorPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "delete_armor"));

    public static final StreamCodec<ByteBuf, DeleteArmorPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), DeleteArmorPacket::modName,
            ByteBufCodecs.stringUtf8(64), DeleteArmorPacket::setId,
            DeleteArmorPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteArmorPacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = tryDelete(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Delete failed: " + error
                    : "\u00a7a[Modkit] Deleted armor set '" + pkt.setId() + "'."));
        });
    }

    private static String tryDelete(DeleteArmorPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";
        if (!pkt.setId().matches("[a-z0-9_]{1,32}")) return "Invalid armor set id.";

        Path workspace = WorkspaceManager.getWorkspacePath(pkt.modName());
        Path jsonFile = workspace.resolve("modkit").resolve("armor").resolve(pkt.setId() + ".json");

        if (!Files.exists(jsonFile)) return "Armor set not found.";

        try {
            Files.delete(jsonFile);

            Path itemTexDir = workspace.resolve("assets").resolve("textures").resolve("item");
            for (String pieceType : ArmorSetDefinition.PIECE_TYPES) {
                deleteQuietly(itemTexDir.resolve(pkt.setId() + "_" + pieceType + ".png"));
            }
            Path armorTexDir = workspace.resolve("assets").resolve("textures").resolve("armor");
            deleteQuietly(armorTexDir.resolve(pkt.setId() + "_layer_1.png"));
            deleteQuietly(armorTexDir.resolve(pkt.setId() + "_layer_2.png"));

            Modkit.LOGGER.info("[Modkit] Deleted armor set: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.setId(), e);
            return "I/O error: " + e.getMessage();
        }
    }

    private static void deleteQuietly(Path p) {
        try {
            if (Files.exists(p)) Files.delete(p);
        } catch (IOException ignore) {
        }
    }
}
