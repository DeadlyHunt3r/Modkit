package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record SaveItemPacket(String modName, String itemId, String json) implements CustomPacketPayload {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Type<SaveItemPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "save_item"));

    public static final StreamCodec<ByteBuf, SaveItemPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),    SaveItemPacket::modName,
            ByteBufCodecs.stringUtf8(64),    SaveItemPacket::itemId,
            ByteBufCodecs.stringUtf8(32768), SaveItemPacket::json,
            SaveItemPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SaveItemPacket pkt, final IPayloadContext context) {
        context.enqueueWork(() -> {

            if (!(context.player() instanceof ServerPlayer player)) return;

            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("\u00a7c[Modkit] No permission to edit projects."));
                return;
            }

            String error = trySave(pkt);
            if (error != null) {
                player.sendSystemMessage(Component.literal("\u00a7c[Modkit] Save failed: " + error));
            } else {
                player.sendSystemMessage(Component.literal("\u00a7a[Modkit] Saved item '" + pkt.itemId + "'."));
            }
        });
    }

    private static String trySave(SaveItemPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) {
            return "Workspace '" + pkt.modName + "' does not exist.";
        }

        ItemDefinition def;
        try {
            def = GSON.fromJson(pkt.json, ItemDefinition.class);
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
        if (def == null) return "Item definition is empty.";
        if (!pkt.itemId.equals(def.id)) {
            return "Item id mismatch: packet=" + pkt.itemId + " json=" + def.id;
        }
        String validationError = def.validate();
        if (validationError != null) return validationError;

        String prettyJson = GSON.toJson(def);
        Path target = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("modkit").resolve("items").resolve(def.id + ".json");

        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, prettyJson);
            Modkit.LOGGER.info("[Modkit] Saved item file: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
