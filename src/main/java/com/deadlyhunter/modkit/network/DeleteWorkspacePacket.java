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
import java.util.Comparator;
import java.util.stream.Stream;

public record DeleteWorkspacePacket(String modName) implements CustomPacketPayload {

    public static final Type<DeleteWorkspacePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "delete_workspace"));

    public static final StreamCodec<ByteBuf, DeleteWorkspacePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), DeleteWorkspacePacket::modName,
            DeleteWorkspacePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteWorkspacePacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission to delete workspaces.", player -> {
            String error = tryDelete(pkt.modName());
            player.sendSystemMessage(Component.literal(error != null
                    ? "\u00a7c[Modkit] Delete failed: " + error
                    : "\u00a7a[Modkit] Deleted workspace '" + pkt.modName() + "'."));
        });
    }

    private static String tryDelete(String modName) {
        if (!modName.matches("[a-z0-9_]{1,30}")) return "Invalid workspace name.";
        if (!WorkspaceManager.exists(modName)) return "Workspace not found.";

        Path workspace = WorkspaceManager.getWorkspacePath(modName);
        try (Stream<Path> walk = Files.walk(workspace)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
            Modkit.LOGGER.info("[Modkit] Deleted workspace: {}", workspace);
            return null;
        } catch (IOException e) {
            Modkit.LOGGER.error("[Modkit] Failed to delete " + workspace, e);
            return "I/O error: " + e.getMessage();
        }
    }
}
