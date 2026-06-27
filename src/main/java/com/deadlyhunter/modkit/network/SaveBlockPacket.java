package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public record SaveBlockPacket(String modName, String blockId, String json) implements CustomPacketPayload {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Type<SaveBlockPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "save_block"));

    public static final StreamCodec<ByteBuf, SaveBlockPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),    SaveBlockPacket::modName,
            ByteBufCodecs.stringUtf8(64),    SaveBlockPacket::blockId,
            ByteBufCodecs.stringUtf8(32768), SaveBlockPacket::json,
            SaveBlockPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveBlockPacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission to edit projects.", player -> {
            String error = trySave(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Save failed: " + error
                    : "\u00a7a[Modkit] Saved block '" + pkt.blockId() + "'."));
        });
    }

    private static String trySave(SaveBlockPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) {
            return "Workspace '" + pkt.modName() + "' does not exist.";
        }

        BlockDefinition def;
        try {
            def = GSON.fromJson(pkt.json(), BlockDefinition.class);
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
        if (def == null) return "Block definition is empty.";
        if (!pkt.blockId().equals(def.id)) {
            return "Block id mismatch: packet=" + pkt.blockId() + " json=" + def.id;
        }
        String validationError = def.validate();
        if (validationError != null) return validationError;

        Path target = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("modkit").resolve("blocks").resolve(def.id + ".json");
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, GSON.toJson(def));
            Modkit.LOGGER.info("[Modkit] Saved block file: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
