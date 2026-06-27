package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.weapon.WeaponDefinition;
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

public record SaveWeaponPacket(String modName, String weaponId, String json) implements CustomPacketPayload {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Type<SaveWeaponPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "save_weapon"));

    public static final StreamCodec<ByteBuf, SaveWeaponPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),    SaveWeaponPacket::modName,
            ByteBufCodecs.stringUtf8(64),    SaveWeaponPacket::weaponId,
            ByteBufCodecs.stringUtf8(32768), SaveWeaponPacket::json,
            SaveWeaponPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveWeaponPacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = trySave(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Save failed: " + error
                    : "\u00a7a[Modkit] Saved weapon '" + pkt.weaponId() + "'."));
        });
    }

    private static String trySave(SaveWeaponPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";

        WeaponDefinition def;
        try {
            def = GSON.fromJson(pkt.json(), WeaponDefinition.class);
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
        if (def == null) return "Weapon definition is empty.";
        if (!pkt.weaponId().equals(def.id)) {
            return "Weapon id mismatch: packet=" + pkt.weaponId() + " json=" + def.id;
        }
        String validationError = def.validate();
        if (validationError != null) return validationError;

        Path target = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("modkit").resolve("weapons").resolve(def.id + ".json");
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, GSON.toJson(def));
            Modkit.LOGGER.info("[Modkit] Saved weapons file: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
