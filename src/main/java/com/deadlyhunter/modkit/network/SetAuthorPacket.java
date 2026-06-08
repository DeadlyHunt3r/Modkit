package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.core.AuthorConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetAuthorPacket {

    private final String prefix;

    public SetAuthorPacket(String prefix) {
        this.prefix = prefix;
    }

    public static void encode(SetAuthorPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.prefix, 32);
    }

    public static SetAuthorPacket decode(FriendlyByteBuf buf) {
        return new SetAuthorPacket(buf.readUtf(32));
    }

    public static void handle(SetAuthorPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission to change author."));
            ctx.setPacketHandled(true);
            return;
        }

        if (!AuthorConfig.isValid(pkt.prefix)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] " + AuthorConfig.getValidationHint()));
        } else {
            AuthorConfig.setAuthor(pkt.prefix);
            player.sendSystemMessage(Component.literal("§a[Modkit] Author set to '" + pkt.prefix + "'."));
        }
        ctx.setPacketHandled(true);
    }
}
