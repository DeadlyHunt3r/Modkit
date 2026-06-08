package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.weapon.WeaponDefinition;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class SaveWeaponPacket {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String modName;
    private final String weaponId;
    private final String json;

    public SaveWeaponPacket(String modName, String weaponId, String json) {
        this.modName = modName;
        this.weaponId = weaponId;
        this.json = json;
    }

    public static void encode(SaveWeaponPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
        buf.writeUtf(pkt.weaponId, 64);
        buf.writeUtf(pkt.json, 32768);
    }

    public static SaveWeaponPacket decode(FriendlyByteBuf buf) {
        return new SaveWeaponPacket(buf.readUtf(64), buf.readUtf(64), buf.readUtf(32768));
    }

    public static void handle(SaveWeaponPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission."));
            ctx.setPacketHandled(true);
            return;
        }

        String error = trySave(pkt);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c[Modkit] Save failed: " + error));
        } else {
            player.sendSystemMessage(Component.literal("§a[Modkit] Saved weapon '" + pkt.weaponId + "'."));
        }
        ctx.setPacketHandled(true);
    }

    private static String trySave(SaveWeaponPacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName)) return "Workspace not found.";

        WeaponDefinition def;
        try {
            def = GSON.fromJson(pkt.json, WeaponDefinition.class);
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
        if (def == null) return "Weapon definition is empty.";
        if (!pkt.weaponId.equals(def.id)) {
            return "Weapon id mismatch: packet=" + pkt.weaponId + " json=" + def.id;
        }
        String validationError = def.validate();
        if (validationError != null) return validationError;

        String prettyJson = GSON.toJson(def);
        Path target = WorkspaceManager.getWorkspacePath(pkt.modName)
                .resolve("modkit").resolve("weapons").resolve(def.id + ".json");

        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, prettyJson);
            Modkit.LOGGER.info("[Modkit] Saved weapon file: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
