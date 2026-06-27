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

public record DeleteRecipePacket(String modName, String recipeId) implements CustomPacketPayload {

    public static final Type<DeleteRecipePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "delete_recipe"));

    public static final StreamCodec<ByteBuf, DeleteRecipePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), DeleteRecipePacket::modName,
            ByteBufCodecs.stringUtf8(64), DeleteRecipePacket::recipeId,
            DeleteRecipePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteRecipePacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission.", player -> {
            String error = tryDelete(pkt);
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Delete failed: " + error
                    : "\u00a7a[Modkit] Deleted recipe '" + pkt.recipeId() + "'."));
        });
    }

    private static String tryDelete(DeleteRecipePacket pkt) {
        if (!WorkspaceManager.exists(pkt.modName())) return "Workspace not found.";
        if (!pkt.recipeId().matches("[a-z0-9_]{1,40}")) return "Invalid recipe id.";

        Path jsonFile = WorkspaceManager.getWorkspacePath(pkt.modName())
                .resolve("modkit").resolve("recipes").resolve(pkt.recipeId() + ".json");
        if (!Files.exists(jsonFile)) return "Recipe not found.";

        try {
            Files.delete(jsonFile);
            Modkit.LOGGER.info("[Modkit] Deleted recipe: {}", jsonFile);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Delete error for " + pkt.recipeId(), e);
            return "I/O error: " + e.getMessage();
        }
    }
}
