package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.recipe.RecipeDefinition;
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

public record SaveRecipePacket(String modName, String recipeId, String json) implements CustomPacketPayload {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Type<SaveRecipePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "save_recipe"));

    public static final StreamCodec<ByteBuf, SaveRecipePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),    SaveRecipePacket::modName,
            ByteBufCodecs.stringUtf8(64),    SaveRecipePacket::recipeId,
            ByteBufCodecs.stringUtf8(32768), SaveRecipePacket::json,
            SaveRecipePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveRecipePacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = trySave(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Save failed: " + error
                    : "\u00a7a[Modkit] Saved recipe '" + pkt.recipeId() + "'."));
        });
    }

    private static String trySave(SaveRecipePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";

        RecipeDefinition def;
        try {
            def = GSON.fromJson(pkt.json(), RecipeDefinition.class);
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
        if (def == null) return "Recipe definition is empty.";
        if (!pkt.recipeId().equals(def.id)) {
            return "Recipe id mismatch: packet=" + pkt.recipeId() + " json=" + def.id;
        }
        String validationError = def.validate();
        if (validationError != null) return validationError;

        Path target = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("modkit").resolve("recipes").resolve(def.id + ".json");
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, GSON.toJson(def));
            Modkit.LOGGER.info("[Modkit] Saved recipes file: {}", target);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to write " + target, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
