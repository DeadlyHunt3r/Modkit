package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.core.AuthorConfig;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CreateWorkspacePacket(String modName) implements CustomPacketPayload {

    public static final Type<CreateWorkspacePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "create_workspace"));

    public static final StreamCodec<ByteBuf, CreateWorkspacePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), CreateWorkspacePacket::modName,
            CreateWorkspacePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CreateWorkspacePacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission to create workspaces.", player -> {
            if (!AuthorConfig.isSet()) {
                player.sendSystemMessage(Component.literal("\u00a7c[Modkit] Set an author prefix first."));
                return;
            }
            WorkspaceManager.CreateResult result = WorkspaceManager.create(AuthorConfig.getAuthor(), pkt.modName());
            player.sendSystemMessage(Component.literal(result.success
                    ? "\u00a7a[Modkit] Created workspace '" + pkt.modName() + "'."
                    : "\u00a7c[Modkit] " + result.message));
        });
    }
}
