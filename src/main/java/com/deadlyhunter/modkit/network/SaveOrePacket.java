package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.ore.OreDefinition;
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

public record SaveOrePacket(String modName, String oreId, String json) implements CustomPacketPayload {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Type<SaveOrePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "save_ore"));

    public static final StreamCodec<ByteBuf, SaveOrePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),    SaveOrePacket::modName,
            ByteBufCodecs.stringUtf8(64),    SaveOrePacket::oreId,
            ByteBufCodecs.stringUtf8(32768), SaveOrePacket::json,
            SaveOrePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveOrePacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = trySave(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Save failed: " + error
                    : "\u00a7a[Modkit] Saved ore '" + pkt.oreId() + "'."));
        });
    }

    private static String trySave(SaveOrePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";

        OreDefinition def;
        try {
            def = GSON.fromJson(pkt.json(), OreDefinition.class);
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
        if (def == null) return "Ore definition is empty.";
        if (!pkt.oreId().equals(def.id)) {
            return "Ore id mismatch: packet=" + pkt.oreId() + " json=" + def.id;
        }
        String validationError = def.validate();
        if (validationError != null) return validationError;

        Path target = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("modkit").resolve("ores").resolve(def.id + ".json");
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, GSON.toJson(def));
            Modkit.LOGGER.info("[Modkit] Saved ores file: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
