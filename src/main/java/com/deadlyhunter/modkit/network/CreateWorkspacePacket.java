package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.core.AuthorConfig;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateWorkspacePacket {

    private final String modName;

    public CreateWorkspacePacket(String modName) {
        this.modName = modName;
    }

    public static void encode(CreateWorkspacePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
    }

    public static CreateWorkspacePacket decode(FriendlyByteBuf buf) {
        return new CreateWorkspacePacket(buf.readUtf(64));
    }

    public static void handle(CreateWorkspacePacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission to create workspaces."));
            ctx.setPacketHandled(true);
            return;
        }

        if (!AuthorConfig.isSet()) {
            player.sendSystemMessage(Component.literal("§c[Modkit] Set an author prefix first."));
            ctx.setPacketHandled(true);
            return;
        }

        WorkspaceManager.CreateResult result =
                WorkspaceManager.create(AuthorConfig.getAuthor(), pkt.modName);
        if (result.success) {
            player.sendSystemMessage(Component.literal("§a[Modkit] Created workspace '" + pkt.modName + "'."));
        } else {
            player.sendSystemMessage(Component.literal("§c[Modkit] " + result.message));
        }
        ctx.setPacketHandled(true);
    }
}
