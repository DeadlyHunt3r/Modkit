package com.deadlyhunter.modkit.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

final class ServerActions {

    private ServerActions() {}

    static void asOp(IPayloadContext context, String noPermissionMessage, Consumer<ServerPlayer> body) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal(noPermissionMessage));
                return;
            }
            body.accept(player);
        });
    }
}
